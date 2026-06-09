package com.ecommerce.carrinho.service;

import com.ecommerce.carrinho.model.Carrinho;
import com.ecommerce.carrinho.model.ItemCarrinho;
import com.ecommerce.carrinho.repository.CarrinhoRepository;
import com.ecommerce.pedido.grpc.CriarPedidoRequest;
import com.ecommerce.pedido.grpc.CriarPedidoResponse;
import com.ecommerce.pedido.grpc.ItemPedido;
import com.ecommerce.pedido.grpc.PedidoServiceGrpc;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarrinhoService {

    private final CarrinhoRepository carrinhoRepository;

    @GrpcClient("pedido-service")
    private PedidoServiceGrpc.PedidoServiceBlockingStub pedidoStub;

    public Carrinho buscarOuCriar(Long usuarioId) {
        return carrinhoRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> carrinhoRepository.save(Carrinho.builder()
                        .usuarioId(usuarioId)
                        .itens(new ArrayList<>())
                        .build()));
    }

    public Carrinho adicionarItem(Long usuarioId, ItemCarrinho item) {
        Carrinho carrinho = buscarOuCriar(usuarioId);
        carrinho.getItens().add(item);
        return carrinhoRepository.save(carrinho);
    }

    public String checkout(Long usuarioId) {
        Carrinho carrinho = carrinhoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Carrinho não encontrado"));

        double valorTotal = carrinho.getItens().stream()
                .mapToDouble(i -> i.getPrecoUnitario() * i.getQuantidade())
                .sum();

        List<ItemPedido> itensPedido = carrinho.getItens().stream()
                .map(i -> ItemPedido.newBuilder()
                        .setProdutoId(i.getProdutoId())
                        .setQuantidade(i.getQuantidade())
                        .setPrecoUnitario(i.getPrecoUnitario())
                        .build())
                .collect(Collectors.toList());

        // =====================================================
        // INVOCAÇÃO REMOTA (RPC) via gRPC
        // Comunicação síncrona entre Carrinho e Pedido usando
        // Protocol Buffers. Chamada remota que parece local.
        // =====================================================
        CriarPedidoRequest request = CriarPedidoRequest.newBuilder()
                .setUsuarioId(usuarioId)
                .setValorTotal(valorTotal)
                .addAllItens(itensPedido)
                .build();

        CriarPedidoResponse response = pedidoStub.criarPedido(request);

        // Esvaziar carrinho após checkout
        carrinho.getItens().clear();
        carrinhoRepository.save(carrinho);

        return "Pedido #" + response.getPedidoId() + " criado via gRPC! Status: "
                + response.getStatus() + ". Aguardando processamento do pagamento.";
    }
}

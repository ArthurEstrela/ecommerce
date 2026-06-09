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
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarrinhoService {

    private final CarrinhoRepository carrinhoRepository;
    private final RestTemplate restTemplate;

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
        Map<String, Object> produto = buscarProduto(item.getProdutoId());
        validarQuantidade(item.getProdutoId(), item.getQuantidade(), produto);
        item.setPrecoUnitario(Double.valueOf(produto.get("preco").toString()));

        Carrinho carrinho = buscarOuCriar(usuarioId);
        carrinho.getItens().add(item);
        return carrinhoRepository.save(carrinho);
    }

    public Carrinho alterarQuantidade(Long usuarioId, Long itemId, Integer quantidade) {
        Carrinho carrinho = carrinhoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Carrinho não encontrado"));

        ItemCarrinho item = carrinho.getItens().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item não encontrado no carrinho"));

        Map<String, Object> produto = buscarProduto(item.getProdutoId());
        validarQuantidade(item.getProdutoId(), quantidade, produto);

        item.setQuantidade(quantidade);
        item.setPrecoUnitario(Double.valueOf(produto.get("preco").toString()));

        return carrinhoRepository.save(carrinho);
    }

    public Carrinho removerItem(Long usuarioId, Long itemId) {
        Carrinho carrinho = carrinhoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Carrinho não encontrado"));

        boolean removido = carrinho.getItens().removeIf(item -> item.getId().equals(itemId));
        if (!removido) {
            throw new RuntimeException("Item não encontrado no carrinho");
        }

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

        return "Pedido #" + response.getPedidoId()
                + " criado com sucesso via gRPC. Status: "
                + response.getStatus()
                + ". Aguardando processamento do pagamento.";
    }

    private Map<String, Object> buscarProduto(Long produtoId) {
        Map<String, Object> produto = restTemplate.getForObject(
                "http://produto-service/api/produtos/" + produtoId,
                Map.class
        );

        if (produto == null) {
            throw new RuntimeException("Produto não encontrado");
        }

        return produto;
    }

    private void validarQuantidade(Long produtoId, Integer quantidade, Map<String, Object> produto) {
        Integer estoque = Integer.valueOf(produto.get("estoque").toString());
        if (quantidade == null || quantidade <= 0) {
            throw new RuntimeException("Quantidade deve ser maior que zero");
        }
        if (estoque < quantidade) {
            throw new RuntimeException("Estoque insuficiente para o produto " + produtoId);
        }
    }
}

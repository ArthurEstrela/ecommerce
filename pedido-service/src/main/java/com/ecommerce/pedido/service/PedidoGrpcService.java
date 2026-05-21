package com.ecommerce.pedido.service;

import com.ecommerce.pedido.grpc.CriarPedidoRequest;
import com.ecommerce.pedido.grpc.CriarPedidoResponse;
import com.ecommerce.pedido.grpc.PedidoServiceGrpc;
import com.ecommerce.pedido.model.ItemPedido;
import com.ecommerce.pedido.model.Pedido;
import com.ecommerce.pedido.repository.PedidoRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
public class PedidoGrpcService extends PedidoServiceGrpc.PedidoServiceImplBase {

    private final PedidoRepository pedidoRepository;

    @Override
    public void criarPedido(CriarPedidoRequest request, StreamObserver<CriarPedidoResponse> responseObserver) {
        
        Pedido pedido = Pedido.builder()
                .usuarioId(request.getUsuarioId())
                .valorTotal(request.getValorTotal())
                .status("PROCESSANDO")
                .itens(request.getItensList().stream().map(item -> ItemPedido.builder()
                        .produtoId(item.getProdutoId())
                        .quantidade(item.getQuantidade())
                        .precoUnitario(item.getPrecoUnitario())
                        .build()).collect(Collectors.toList()))
                .build();

        Pedido salvo = pedidoRepository.save(pedido);

        CriarPedidoResponse response = CriarPedidoResponse.newBuilder()
                .setPedidoId(salvo.getId())
                .setStatus(salvo.getStatus())
                .setMensagem("Pedido criado com sucesso via gRPC!")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

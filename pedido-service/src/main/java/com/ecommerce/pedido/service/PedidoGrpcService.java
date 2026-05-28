package com.ecommerce.pedido.service;

import com.ecommerce.pedido.config.RabbitConfig;
import com.ecommerce.pedido.grpc.CriarPedidoRequest;
import com.ecommerce.pedido.grpc.CriarPedidoResponse;
import com.ecommerce.pedido.grpc.PedidoServiceGrpc;
import com.ecommerce.pedido.model.ItemPedido;
import com.ecommerce.pedido.model.Pedido;
import com.ecommerce.pedido.repository.PedidoRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
public class PedidoGrpcService extends PedidoServiceGrpc.PedidoServiceImplBase {

    private final PedidoRepository pedidoRepository;
    private final RabbitTemplate rabbitTemplate;

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

        // =====================================================
        // FILA DEDICADA (Direct Exchange) - Processamento Assíncrono
        // Publica uma mensagem na fila "pedido.criado.queue" para
        // que o serviço de Notificação processe assincronamente.
        // Diferente do Pub/Sub: aqui é ponto-a-ponto (1 produtor → 1 consumidor).
        // =====================================================
        Map<String, Object> evento = new HashMap<>();
        evento.put("pedidoId", salvo.getId());
        evento.put("usuarioId", salvo.getUsuarioId());
        evento.put("valorTotal", salvo.getValorTotal());
        evento.put("status", salvo.getStatus());
        rabbitTemplate.convertAndSend(
                RabbitConfig.PEDIDO_EXCHANGE,
                RabbitConfig.PEDIDO_ROUTING_KEY,
                evento
        );
        System.out.println("Pedido Service: Evento 'pedido.criado' publicado na FILA DEDICADA (Direct) para pedido: " + salvo.getId());

        CriarPedidoResponse response = CriarPedidoResponse.newBuilder()
                .setPedidoId(salvo.getId())
                .setStatus(salvo.getStatus())
                .setMensagem("Pedido criado com sucesso via gRPC!")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

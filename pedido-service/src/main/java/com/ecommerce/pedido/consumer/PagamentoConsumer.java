package com.ecommerce.pedido.consumer;

import com.ecommerce.pedido.config.RabbitConfig;
import com.ecommerce.pedido.model.Pedido;
import com.ecommerce.pedido.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumidor Pub/Sub (Fanout Exchange)
 * 
 * Este consumer se inscreve na exchange "pagamento.exchange" (Fanout)
 * e atualiza o status do pedido quando o pagamento é confirmado.
 * 
 * Demonstra o padrão Publish/Subscribe: um evento publicado pelo
 * Pagamento-Service é recebido por MÚLTIPLOS consumidores
 * (Estoque, Notificação e agora Pedido) de forma independente.
 */
@Component
@RequiredArgsConstructor
public class PagamentoConsumer {

    private final PedidoRepository pedidoRepository;

    @RabbitListener(queues = RabbitConfig.PEDIDO_PAGAMENTO_QUEUE)
    public void consumirPagamento(Map<String, Object> mensagem) {
        System.out.println("Pedido Service: Recebido evento de pagamento via PUB/SUB (Fanout): " + mensagem);
        
        Long pedidoId = Long.valueOf(mensagem.get("pedidoId").toString());
        String status = mensagem.get("status").toString();

        pedidoRepository.findById(pedidoId).ifPresent(pedido -> {
            if ("APROVADO".equals(status)) {
                pedido.setStatus("PAGO");
                pedidoRepository.save(pedido);
                System.out.println("Pedido Service: Pedido #" + pedidoId + " atualizado para PAGO");
            } else {
                pedido.setStatus("PAGAMENTO_RECUSADO");
                pedidoRepository.save(pedido);
                System.out.println("Pedido Service: Pedido #" + pedidoId + " marcado como PAGAMENTO_RECUSADO");
            }
        });
    }
}

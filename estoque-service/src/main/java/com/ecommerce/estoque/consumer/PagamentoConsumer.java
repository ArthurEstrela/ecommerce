package com.ecommerce.estoque.consumer;

import com.ecommerce.estoque.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PagamentoConsumer {

    @RabbitListener(queues = RabbitConfig.ESTOQUE_QUEUE)
    public void consumirPagamento(Map<String, Object> mensagem) {
        System.out.println("Estoque Service: Recebido evento de pagamento: " + mensagem);
        Long pedidoId = Long.valueOf(mensagem.get("pedidoId").toString());
        String status = mensagem.get("status").toString();

        if ("APROVADO".equals(status)) {
            System.out.println("Estoque Service: Reservando produtos para o pedido: " + pedidoId);
            // Lógica de reserva de estoque aqui
        }
    }
}

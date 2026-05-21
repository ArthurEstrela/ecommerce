package com.ecommerce.notificacao.consumer;

import com.ecommerce.notificacao.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificacaoConsumer {

    @RabbitListener(queues = RabbitConfig.NOTIFICACAO_QUEUE)
    public void consumirNotificacao(Map<String, Object> mensagem) {
        System.out.println("Notificação Service: Recebido evento para envio de alerta: " + mensagem);
        Long pedidoId = Long.valueOf(mensagem.get("pedidoId").toString());
        String status = mensagem.get("status").toString();

        System.out.println("### ALERTA ENVIADO ###");
        System.out.println("Olá! Seu pedido #" + pedidoId + " teve o status atualizado para: " + status);
        System.out.println("#######################");
    }
}

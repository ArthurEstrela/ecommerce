package com.ecommerce.pagamento.service;

import com.ecommerce.pagamento.config.RabbitConfig;
import com.ecommerce.pagamento.event.PagamentoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final RabbitTemplate rabbitTemplate;

    public void processarPagamento(Long pedidoId, Double valor) {
        // Simulação de processamento
        PagamentoEvent event = PagamentoEvent.builder()
                .pedidoId(pedidoId)
                .status("APROVADO")
                .valor(valor)
                .build();

        rabbitTemplate.convertAndSend(RabbitConfig.PAGAMENTO_EXCHANGE, "", event);
        System.out.println("Evento de pagamento publicado para o pedido: " + pedidoId);
    }
}

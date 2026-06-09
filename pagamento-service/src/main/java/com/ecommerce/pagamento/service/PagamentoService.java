package com.ecommerce.pagamento.service;

import com.ecommerce.pagamento.config.RabbitConfig;
import com.ecommerce.pagamento.event.PagamentoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;

    public void processarPagamento(Long pedidoId) {
        Map<String, Object> pedido = restTemplate.getForObject(
                "http://pedido-service/api/pedidos/" + pedidoId,
                Map.class
        );

        if (pedido == null) {
            throw new RuntimeException("Pedido não encontrado");
        }

        Double valor = Double.valueOf(pedido.get("valorTotal").toString());
        List<Map<String, Object>> itens = (List<Map<String, Object>>) pedido.get("itens");

        // Simulação de processamento
        PagamentoEvent event = PagamentoEvent.builder()
                .pedidoId(pedidoId)
                .status("APROVADO")
                .valor(valor)
                .itens(itens)
                .build();

        rabbitTemplate.convertAndSend(RabbitConfig.PAGAMENTO_EXCHANGE, "", event);
        System.out.println("Evento de pagamento publicado para o pedido: " + pedidoId);
    }
}

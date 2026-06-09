package com.ecommerce.estoque.consumer;

import com.ecommerce.estoque.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PagamentoConsumer {

    private final RestTemplate restTemplate;

    @RabbitListener(queues = RabbitConfig.ESTOQUE_QUEUE)
    public void consumirPagamento(Map<String, Object> mensagem) {
        System.out.println("Estoque Service: Recebido evento de pagamento: " + mensagem);
        Long pedidoId = Long.valueOf(mensagem.get("pedidoId").toString());
        String status = mensagem.get("status").toString();

        if ("APROVADO".equals(status)) {
            List<Map<String, Object>> itens = (List<Map<String, Object>>) mensagem.get("itens");

            if (itens == null || itens.isEmpty()) {
                System.out.println("Estoque Service: Pedido #" + pedidoId + " não possui itens para baixa de estoque");
                return;
            }

            itens.forEach(item -> {
                Long produtoId = Long.valueOf(item.get("produtoId").toString());
                Integer quantidade = Integer.valueOf(item.get("quantidade").toString());

                String url = "http://produto-service/api/produtos/" + produtoId
                        + "/baixar-estoque?quantidade=" + quantidade;

                restTemplate.postForObject(url, null, Map.class);
                System.out.println("Estoque Service: Baixou " + quantidade
                        + " unidade(s) do produto #" + produtoId
                        + " para o pedido #" + pedidoId);
            });
        }
    }
}

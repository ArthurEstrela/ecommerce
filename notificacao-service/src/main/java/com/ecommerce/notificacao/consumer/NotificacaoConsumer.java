package com.ecommerce.notificacao.consumer;

import com.ecommerce.notificacao.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificacaoConsumer {

    /**
     * PADRÃO PUB/SUB (Fanout Exchange)
     * 
     * Consome eventos publicados na exchange "pagamento.exchange" (Fanout).
     * Este é o padrão Publish/Subscribe: o Pagamento publica UM evento e
     * MÚLTIPLOS consumidores (Estoque, Notificação, Pedido) recebem
     * independentemente.
     */
    @RabbitListener(queues = RabbitConfig.NOTIFICACAO_QUEUE)
    public void consumirPagamento(Map<String, Object> mensagem) {
        System.out.println("Notificação Service [PUB/SUB]: Recebido evento de pagamento: " + mensagem);
        Long pedidoId = Long.valueOf(mensagem.get("pedidoId").toString());
        String status = mensagem.get("status").toString();

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     📧 NOTIFICAÇÃO DE PAGAMENTO         ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  Pedido: #" + pedidoId);
        System.out.println("║  Status: " + status);
        System.out.println("║  Canal:  PUB/SUB (Fanout Exchange)");
        System.out.println("╚══════════════════════════════════════════╝");
    }

    /**
     * PADRÃO FILA DEDICADA (Direct Exchange)
     * 
     * Consome mensagens da fila "pedido.criado.queue" (Direct Exchange).
     * Este é o padrão de FILA para processamento assíncrono ponto-a-ponto:
     * o Pedido-Service envia uma mensagem diretamente para ESTA fila e
     * apenas UM consumidor (Notificação) processa.
     * 
     * Diferença fundamental do Pub/Sub:
     * - Pub/Sub (Fanout): 1 mensagem → N consumidores
     * - Fila (Direct):    1 mensagem → 1 consumidor
     */
    @RabbitListener(queues = RabbitConfig.PEDIDO_CRIADO_QUEUE)
    public void consumirPedidoCriado(Map<String, Object> mensagem) {
        System.out.println("Notificação Service [FILA DEDICADA]: Recebido evento de pedido criado: " + mensagem);
        Long pedidoId = Long.valueOf(mensagem.get("pedidoId").toString());
        Long usuarioId = Long.valueOf(mensagem.get("usuarioId").toString());
        Double valorTotal = Double.valueOf(mensagem.get("valorTotal").toString());

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     📦 NOTIFICAÇÃO DE PEDIDO CRIADO     ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  Pedido:  #" + pedidoId);
        System.out.println("║  Usuário: #" + usuarioId);
        System.out.println("║  Valor:   R$ " + String.format("%.2f", valorTotal));
        System.out.println("║  Canal:   FILA DEDICADA (Direct Exchange)");
        System.out.println("╚══════════════════════════════════════════╝");
    }
}

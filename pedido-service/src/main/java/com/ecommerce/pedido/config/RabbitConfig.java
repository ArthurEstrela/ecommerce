package com.ecommerce.pedido.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // ==================== FILA DEDICADA (Direct Exchange) ====================
    // Demonstra: Comunicação ponto-a-ponto assíncrona (Fila)
    // Quando um pedido é criado, uma mensagem é enviada diretamente para
    // a fila de notificação para processamento assíncrono.
    public static final String PEDIDO_EXCHANGE = "pedido.exchange";
    public static final String PEDIDO_CRIADO_QUEUE = "pedido.criado.queue";
    public static final String PEDIDO_ROUTING_KEY = "pedido.criado";

    @Bean
    public DirectExchange pedidoExchange() {
        return new DirectExchange(PEDIDO_EXCHANGE);
    }

    @Bean
    public Queue pedidoCriadoQueue() {
        return new Queue(PEDIDO_CRIADO_QUEUE, true);
    }

    @Bean
    public Binding pedidoCriadoBinding(Queue pedidoCriadoQueue, DirectExchange pedidoExchange) {
        return BindingBuilder.bind(pedidoCriadoQueue).to(pedidoExchange).with(PEDIDO_ROUTING_KEY);
    }

    // ==================== PUB/SUB (Fanout Exchange - consumir pagamento) ======
    // Demonstra: Publish/Subscribe (Eventos)
    // Pedido-service se inscreve na exchange de pagamento para atualizar
    // o status do pedido quando o pagamento é confirmado.
    public static final String PAGAMENTO_EXCHANGE = "pagamento.exchange";
    public static final String PEDIDO_PAGAMENTO_QUEUE = "pedido.pagamento.queue";

    @Bean
    public FanoutExchange pagamentoFanoutExchange() {
        return new FanoutExchange(PAGAMENTO_EXCHANGE);
    }

    @Bean
    public Queue pedidoPagamentoQueue() {
        return new Queue(PEDIDO_PAGAMENTO_QUEUE, true);
    }

    @Bean
    public Binding pedidoPagamentoBinding(Queue pedidoPagamentoQueue, FanoutExchange pagamentoFanoutExchange) {
        return BindingBuilder.bind(pedidoPagamentoQueue).to(pagamentoFanoutExchange);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

package com.ecommerce.estoque.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String PAGAMENTO_EXCHANGE = "pagamento.exchange";
    public static final String ESTOQUE_QUEUE = "estoque.queue";

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(PAGAMENTO_EXCHANGE);
    }

    @Bean
    public Queue estoqueQueue() {
        return new Queue(ESTOQUE_QUEUE, true);
    }

    @Bean
    public Binding binding(Queue estoqueQueue, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(estoqueQueue).to(fanoutExchange);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

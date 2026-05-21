package com.ecommerce.notificacao.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String PAGAMENTO_EXCHANGE = "pagamento.exchange";
    public static final String NOTIFICACAO_QUEUE = "notificacao.queue";

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(PAGAMENTO_EXCHANGE);
    }

    @Bean
    public Queue notificacaoQueue() {
        return new Queue(NOTIFICACAO_QUEUE, true);
    }

    @Bean
    public Binding binding(Queue notificacaoQueue, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(notificacaoQueue).to(fanoutExchange);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

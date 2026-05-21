package com.ecommerce.pagamento.config;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String PAGAMENTO_EXCHANGE = "pagamento.exchange";

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(PAGAMENTO_EXCHANGE);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

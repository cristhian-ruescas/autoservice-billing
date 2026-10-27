package com.autoservice.billing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.autoservice.billing.messaging.TiposEvento.COMPENSAR_PAGAMENTO;
import static com.autoservice.billing.messaging.TiposEvento.EXCHANGE;
import static com.autoservice.billing.messaging.TiposEvento.ORCAMENTO_SOLICITADO;

@Configuration
@Profile("!test")
public class RabbitConfig {

    public static final String QUEUE = "autoservice.billing";

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue billingQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding bindOrcamento(final Queue billingQueue, final TopicExchange sagaExchange) {
        return BindingBuilder.bind(billingQueue).to(sagaExchange).with(ORCAMENTO_SOLICITADO);
    }

    @Bean
    public Binding bindCompensacao(final Queue billingQueue, final TopicExchange sagaExchange) {
        return BindingBuilder.bind(billingQueue).to(sagaExchange).with(COMPENSAR_PAGAMENTO);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(final ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            final ConnectionFactory connectionFactory,
            final Jackson2JsonMessageConverter converter
    ) {
        final RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setExchange(EXCHANGE);
        template.setMessageConverter(converter);
        return template;
    }
}

package com.sni.bokaticowork.core.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ──────────────────────────── Exchanges ────────────────────────────

    @Bean
    public TopicExchange emailExchange() {
        return ExchangeBuilder.topicExchange("email").durable(true).build();
    }

    @Bean
    public TopicExchange emailDlxExchange() {
        return ExchangeBuilder.topicExchange("email.dlx").durable(true).build();
    }

    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder.topicExchange("notification").durable(true).build();
    }

    @Bean
    public TopicExchange notificationDlxExchange() {
        return ExchangeBuilder.topicExchange("notification.dlx").durable(true).build();
    }

    // ──────────────────────────── Email Queues ────────────────────────

    @Bean
    public Queue emailCriticalQueue() {
        return QueueBuilder.durable("email.critical")
                .withArgument("x-dead-letter-exchange", "email.dlx")
                .withArgument("x-dead-letter-routing-key", "email.dlq")
                .build();
    }

    @Bean
    public Queue emailHighQueue() {
        return QueueBuilder.durable("email.high")
                .withArgument("x-dead-letter-exchange", "email.dlx")
                .withArgument("x-dead-letter-routing-key", "email.dlq")
                .build();
    }

    @Bean
    public Queue emailNormalQueue() {
        return QueueBuilder.durable("email.normal")
                .withArgument("x-dead-letter-exchange", "email.dlx")
                .withArgument("x-dead-letter-routing-key", "email.dlq")
                .build();
    }

    @Bean
    public Queue emailBulkQueue() {
        return QueueBuilder.durable("email.bulk")
                .withArgument("x-dead-letter-exchange", "email.dlx")
                .withArgument("x-dead-letter-routing-key", "email.dlq")
                .build();
    }

    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable("email.dlq").build();
    }

    // ──────────────────────────── Notification Queues ─────────────────

    @Bean
    public Queue notificationInappQueue() {
        return QueueBuilder.durable("notification.inapp")
                .withArgument("x-dead-letter-exchange", "notification.dlx")
                .withArgument("x-dead-letter-routing-key", "notification.dlq")
                .build();
    }

    @Bean
    public Queue notificationAdminQueue() {
        return QueueBuilder.durable("notification.admin")
                .withArgument("x-dead-letter-exchange", "notification.dlx")
                .withArgument("x-dead-letter-routing-key", "notification.dlq")
                .build();
    }

    @Bean
    public Queue notificationWebhookQueue() {
        return QueueBuilder.durable("notification.webhook")
                .withArgument("x-dead-letter-exchange", "notification.dlx")
                .withArgument("x-dead-letter-routing-key", "notification.dlq")
                .build();
    }

    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable("notification.dlq").build();
    }

    // ──────────────────────────── Bindings ────────────────────────────

    @Bean
    public Binding emailCriticalBinding() {
        return BindingBuilder.bind(emailCriticalQueue()).to(emailExchange()).with("email.critical.#");
    }

    @Bean
    public Binding emailHighBinding() {
        return BindingBuilder.bind(emailHighQueue()).to(emailExchange()).with("email.high.#");
    }

    @Bean
    public Binding emailNormalBinding() {
        return BindingBuilder.bind(emailNormalQueue()).to(emailExchange()).with("email.normal.#");
    }

    @Bean
    public Binding emailBulkBinding() {
        return BindingBuilder.bind(emailBulkQueue()).to(emailExchange()).with("email.bulk.#");
    }

    @Bean
    public Binding emailDlqBinding() {
        return BindingBuilder.bind(emailDlq()).to(emailDlxExchange()).with("email.dlq");
    }

    @Bean
    public Binding notificationInappBinding() {
        return BindingBuilder.bind(notificationInappQueue()).to(notificationExchange()).with("notification.inapp.#");
    }

    @Bean
    public Binding notificationAdminBinding() {
        return BindingBuilder.bind(notificationAdminQueue()).to(notificationExchange()).with("notification.admin.#");
    }

    @Bean
    public Binding notificationWebhookBinding() {
        return BindingBuilder.bind(notificationWebhookQueue()).to(notificationExchange()).with("notification.webhook.#");
    }

    @Bean
    public Binding notificationDlqBinding() {
        return BindingBuilder.bind(notificationDlq()).to(notificationDlxExchange()).with("notification.dlq");
    }

    // ──────────────────────────── Message Converter ───────────────────

    @Bean
    @SuppressWarnings("removal")
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // ──────────────────── Listener Container Factories ────────────────

    @Bean
    public SimpleRabbitListenerContainerFactory criticalEmailListenerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setConcurrentConsumers(4);
        factory.setMaxConcurrentConsumers(8);
        factory.setPrefetchCount(5);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory highEmailListenerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(4);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory normalEmailListenerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(2);
        factory.setPrefetchCount(20);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory bulkEmailListenerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setPrefetchCount(50);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory notificationListenerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(4);
        factory.setPrefetchCount(20);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory dlqListenerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setPrefetchCount(5);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}

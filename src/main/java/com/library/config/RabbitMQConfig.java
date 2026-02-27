package com.library.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 */
@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true", matchIfMissing = false)
public class RabbitMQConfig {

    /**
     * 通知队列名称
     */
    public static final String NOTIFICATION_QUEUE = "notification.queue";

    /**
     * 通知交换机名称
     */
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";

    /**
     * 通知路由键
     */
    public static final String NOTIFICATION_ROUTING_KEY = "notification.send";

    /**
     * 死信队列名称
     */
    public static final String NOTIFICATION_DLQ = "notification.dlq";

    /**
     * 死信交换机名称
     */
    public static final String NOTIFICATION_DLX = "notification.dlx";

    /**
     * 死信路由键
     */
    public static final String NOTIFICATION_DLQ_ROUTING_KEY = "notification.dead";

    /**
     * 消息转换器(使用JSON格式)
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * 监听器容器工厂配置
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); // 手动确认
        factory.setPrefetchCount(1); // 每次只拉取一条消息
        return factory;
    }

    /**
     * 通知队列
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .deadLetterExchange(NOTIFICATION_DLX) // 死信交换机
                .deadLetterRoutingKey(NOTIFICATION_DLQ_ROUTING_KEY) // 死信路由键
                .build();
    }

    /**
     * 通知交换机
     */
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    /**
     * 绑定通知队列到交换机
     */
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_ROUTING_KEY);
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue notificationDeadLetterQueue() {
        return new Queue(NOTIFICATION_DLQ, true);
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange notificationDeadLetterExchange() {
        return new DirectExchange(NOTIFICATION_DLX, true, false);
    }

    /**
     * 绑定死信队列到死信交换机
     */
    @Bean
    public Binding notificationDeadLetterBinding() {
        return BindingBuilder.bind(notificationDeadLetterQueue())
                .to(notificationDeadLetterExchange())
                .with(NOTIFICATION_DLQ_ROUTING_KEY);
    }
}

package com.victor.EventDrop.rabbitmq;

import com.azure.storage.common.policy.RetryPolicyType;
import com.victor.EventDrop.filedrops.client.FileDropStorageClient;
import com.victor.EventDrop.occupants.OccupantService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.backoff.Sleeper;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

@Configuration
@RequiredArgsConstructor
public class RabbitConfig {

    private final RabbitConfigProperties rabbitConfigProperties;
    private final AsyncTaskExecutor asyncTaskExecutor;
    @Value("${spring.rabbitmq.prefetch-count}")
    private int prefetchCount;
    @Value("${spring.rabbitmq.reply-timeout}")
    private int replyTimeout;

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RetryTemplate rabbitRetryTemplate(){
        RetryTemplate retryTemplate = new RetryTemplate();
        RetryPolicy retryPolicy = new SimpleRetryPolicy(5);
        retryTemplate.setRetryPolicy(retryPolicy);

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(2000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;

    }

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory cachingConnectionFactory, Jackson2JsonMessageConverter jackson2JsonMessageConverter, RetryTemplate rabbitRetryTemplate){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);
        rabbitTemplate.setReplyTimeout(replyTimeout);
        rabbitTemplate.setRetryTemplate(rabbitRetryTemplate);
        rabbitTemplate.setTaskExecutor(asyncTaskExecutor);
        return rabbitTemplate;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(CachingConnectionFactory cachingConnectionFactory){
        return new RabbitAdmin(cachingConnectionFactory);
    }

    @Bean
    @Profile("prod")
    public SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory(CachingConnectionFactory cachingConnectionFactory,RetryTemplate rabbitRetryTemplate){
        SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory = new SimpleRabbitListenerContainerFactory();
        simpleRabbitListenerContainerFactory.setConnectionFactory(cachingConnectionFactory);
        simpleRabbitListenerContainerFactory.setPrefetchCount(prefetchCount);
        simpleRabbitListenerContainerFactory.setTaskExecutor(asyncTaskExecutor);
        simpleRabbitListenerContainerFactory.setRetryTemplate(rabbitRetryTemplate);
        return simpleRabbitListenerContainerFactory;

    }

    @Bean
    @Profile("prod")
    public CachingConnectionFactory cachingConnectionFactory() throws NoSuchAlgorithmException, KeyManagementException {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setPassword(rabbitConfigProperties.getPassword());
        connectionFactory.setUsername(rabbitConfigProperties.getUsername());
        connectionFactory.setPort(rabbitConfigProperties.getPort());
        connectionFactory.setVirtualHost(rabbitConfigProperties.getVirtualHost());
        connectionFactory.setHost(rabbitConfigProperties.getHost());
        connectionFactory.getRabbitConnectionFactory().useSslProtocol();
        return connectionFactory;
    }


}

package com.victor.EventDrop.rabbitmq;

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
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

@Configuration
@RequiredArgsConstructor
public class RabbitConfig {

    private final RabbitConfigProperties rabbitConfigProperties;
    private AsyncTaskExecutor asyncTaskExecutor;
    @Value("${spring.rabbitmq.prefetch-count}")
    private int prefetchCount;

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter jackson2JsonMessageConverter){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);

        return rabbitTemplate;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(CachingConnectionFactory cachingConnectionFactory){
        return new RabbitAdmin(cachingConnectionFactory);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(CachingConnectionFactory cachingConnectionFactory){
        SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory = new SimpleRabbitListenerContainerFactory();
        simpleRabbitListenerContainerFactory.setConnectionFactory(cachingConnectionFactory);
        simpleRabbitListenerContainerFactory.setPrefetchCount(prefetchCount);
        simpleRabbitListenerContainerFactory.setTaskExecutor(asyncTaskExecutor);
        return simpleRabbitListenerContainerFactory;

    }

    @Bean
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

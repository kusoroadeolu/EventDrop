package com.victor.EventDrop.filedrops.config;

import org.apache.coyote.ProtocolHandler;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.CompositeTaskDecorator;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Configuration
public class FileDropConfig {
    @Bean
    public ExecutorService virtualExecutorService(){
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    @Primary
    public AsyncTaskExecutor asyncTaskExecutor(ExecutorService virtualExecutorService){
        return new DelegatingSecurityContextAsyncTaskExecutor(new TaskExecutorAdapter(virtualExecutorService));
    }

    @Bean
    public TomcatProtocolHandlerCustomizer<? extends ProtocolHandler> tomcatProtocolHandlerCustomizer(ExecutorService virtualExecutorService){
        return protocolHandler -> {
            protocolHandler.setExecutor(virtualExecutorService);
        };
    }



}

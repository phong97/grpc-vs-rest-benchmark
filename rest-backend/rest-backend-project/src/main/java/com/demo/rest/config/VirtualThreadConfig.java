package com.demo.rest.config;

import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

/**
 *
 * @author phonghv
 */
@Configuration
public class VirtualThreadConfig {

    @Bean
    public WebServerFactoryCustomizer<JettyServletWebServerFactory> customizer() {
        return factory -> {
            QueuedThreadPool threadPool = new QueuedThreadPool();
            threadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());
            factory.setThreadPool(threadPool);
        };
    }
}

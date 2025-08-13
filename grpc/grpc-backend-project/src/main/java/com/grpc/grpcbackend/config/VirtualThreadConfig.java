package com.grpc.grpcbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 *
 * @author phonghv
 */
@Configuration
public class VirtualThreadConfig {

    @Bean
    public Executor grpcExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
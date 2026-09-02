package com.turing.app.api.notification.config;

import java.util.concurrent.*;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class NotificationExecutorConfig {
  @Bean("emailExecutor")
  Executor emailExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("email-");
    executor.initialize();
    return executor;
  }
}

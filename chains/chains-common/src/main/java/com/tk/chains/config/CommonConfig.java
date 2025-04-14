package com.tk.chains.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class CommonConfig {

    private static final AtomicInteger chainBlockIndex = new AtomicInteger(0);

    private static final ExecutorService executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 3,
            Runtime.getRuntime().availableProcessors() * 3,
            0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            r -> new Thread(r, "chain-block-" + chainBlockIndex.incrementAndGet()));

    @Bean
    @Qualifier("chainBlock")
    public ExecutorService getExecutorService() {
        return executorService;
    }

}

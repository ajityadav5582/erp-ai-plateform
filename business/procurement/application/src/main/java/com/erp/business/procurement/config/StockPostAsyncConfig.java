package com.erp.business.procurement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Enables asynchronous execution and provides the executor the goods-receipt
 * stock post runs on.
 *
 * <p>The stock post to Inventory runs after the receipt transaction commits. Making
 * it {@code @Async} on this dedicated pool means the caller's request returns as
 * soon as the receipt is durable, instead of blocking on the (up to several second)
 * Inventory round-trip - and a slow or down Inventory no longer holds up receiving.
 *
 * <p>The pool is deliberately small and bounded: stock posts are low-volume, quick
 * HTTP calls. {@link ThreadPoolExecutor.CallerRunsPolicy} is the backpressure valve -
 * if the pool and queue are ever saturated the committing thread runs the post
 * itself rather than dropping it, so a receipt is never silently left unposted.
 */
@Configuration
@EnableAsync
public class StockPostAsyncConfig {

    @Bean("stockPostExecutor")
    public ThreadPoolTaskExecutor stockPostExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("stock-post-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

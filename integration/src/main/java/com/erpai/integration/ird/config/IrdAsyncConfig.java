package com.erpai.integration.ird.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class IrdAsyncConfig {

    @Bean(name = "irdVirtualThreadExecutor")
    public Executor irdVirtualThreadExecutor() {
        ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        TaskDecorator mdcDecorator = new MdcPropagatingTaskDecorator();
        return new DecoratingExecutor(virtualThreadExecutor, mdcDecorator);
    }

    private static final class DecoratingExecutor implements Executor {
        private final Executor delegate;
        private final TaskDecorator decorator;

        private DecoratingExecutor(Executor delegate, TaskDecorator decorator) {
            this.delegate = delegate;
            this.decorator = decorator;
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(decorator.decorate(command));
        }
    }

    private static final class MdcPropagatingTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previous = MDC.getCopyOfContextMap();
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                } else {
                    MDC.clear();
                }
                try {
                    runnable.run();
                } finally {
                    if (previous != null) {
                        MDC.setContextMap(previous);
                    } else {
                        MDC.clear();
                    }
                }
            };
        }
    }
}

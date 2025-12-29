package project.airbnb.clone.config.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public ThreadPoolTaskExecutor executor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        int cores = Runtime.getRuntime().availableProcessors();

        executor.setCorePoolSize(cores);
        executor.setMaxPoolSize(cores);
        executor.setThreadNamePrefix("async-");
        executor.setQueueCapacity(100);
        executor.setAwaitTerminationSeconds(5);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        executor.getThreadPoolExecutor().prestartAllCoreThreads();

        return executor;
    }

    @Bean(name = "viewHistoryExecutor")
    public Executor viewHistoryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        int cores = Runtime.getRuntime().availableProcessors();

        executor.setCorePoolSize(cores);
        executor.setMaxPoolSize(cores * 2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("view-history-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        executor.getThreadPoolExecutor().prestartAllCoreThreads();

        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return executor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("Error on async execution: {}", method.toGenericString(), ex);
        };
    }
}

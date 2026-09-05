package com.sellam.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
// @EnableAsync crée automatiquement un TaskExecutor par défaut, mais avec des executors WebSocket
// multiples, cela crée une ambiguïté. Voir SecurityConfig.taskExecutor() pour la configuration explicite.
@EnableAsync
@EnableScheduling
@EnableJpaAuditing
@EntityScan("com.sellam.store")
@EnableJpaRepositories(basePackages = "com.sellam.store")
@SpringBootApplication(scanBasePackages = {"com.sellam.store"})
public class ProductsBackendApplication implements AsyncConfigurer {

	public static void main(String[] args) {
		SpringApplication.run(ProductsBackendApplication.class, args);
	}

	@Override
	public Executor getAsyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("async-task-");
		executor.setAwaitTerminationSeconds(60);
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.initialize();
		return executor;
	}

}

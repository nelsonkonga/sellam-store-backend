package com.sellam.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaAuditing
@EntityScan("com.sellam.store")
@EnableJpaRepositories(basePackages = "com.sellam.store")
@SpringBootApplication(scanBasePackages = {"com.sellam.store"})
public class ProductsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductsBackendApplication.class, args);
	}

}

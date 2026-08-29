package com.sellam.store;

import com.sellam.store.ProductsBackendApplication;
import nl.martijndwars.webpush.PushService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.mock;

@SpringBootTest(classes = ProductsBackendApplication.class)
@org.springframework.test.context.ActiveProfiles("test")
@Import(ProductsBackendApplicationTests.TestPushConfiguration.class)
class ProductsBackendApplicationTests {

	@TestConfiguration
	static class TestPushConfiguration {
		@Bean
		PushService pushService() {
			return mock(PushService.class);
		}
	}

	@Test
	void contextLoads() {
	}

}

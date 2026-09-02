package com.helpdesk.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Context-loads smoke test backed by a disposable PostgreSQL container so it
 * never depends on the live Supabase instance (and runs anywhere, including CI).
 * The embedded AMQP message broker also permits a broker-free context because
 * RabbitAdmin declares topology lazily on first publish.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ApiApplicationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> postgres =
			new PostgreSQLContainer<>("postgres:16-alpine");

	@Test
	void contextLoads() {
	}

}

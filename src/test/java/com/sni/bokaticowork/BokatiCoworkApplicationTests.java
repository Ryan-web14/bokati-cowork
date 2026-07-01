package com.sni.bokaticowork;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Requires full Docker stack (PostgreSQL, Redis, RabbitMQ, MinIO) — run locally only.
@SpringBootTest
@ActiveProfiles("dev")
@Disabled("Integration smoke test — requires Docker infrastructure, excluded from CI builds")
class BokatiCoworkApplicationTests {

	@Test
	void contextLoads() {
	}

}

package ru.marketplace.finance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class DatabaseMigrationIntegrationTest {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

	@DynamicPropertySource
	static void configurePostgres(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		registry.add("spring.flyway.enabled", () -> "true");
	}

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void flywayCreatesExpectedTablesAndIndexes() {
		assertThat(tableNames()).contains(
				"users",
				"marketplace_credentials",
				"product_costs",
				"sync_jobs",
				"financial_operations_raw",
				"daily_finance_entries",
				"flyway_schema_history");

		assertThat(indexNames()).contains(
				"idx_product_cost_lookup",
				"idx_sync_jobs_user_requested",
				"idx_sync_jobs_user_status",
				"idx_raw_user_date",
				"idx_raw_user_srid_date",
				"idx_raw_user_nm_date",
				"uq_daily_product",
				"uq_daily_common",
				"idx_daily_report_period");

		assertThat(appliedMigrationCount()).isEqualTo(7);
	}

	private List<String> tableNames() {
		return jdbcTemplate.queryForList("""
				SELECT table_name
				FROM information_schema.tables
				WHERE table_schema = 'public'
				""", String.class);
	}

	private List<String> indexNames() {
		return jdbcTemplate.queryForList("""
				SELECT indexname
				FROM pg_indexes
				WHERE schemaname = 'public'
				""", String.class);
	}

	private Integer appliedMigrationCount() {
		return jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM flyway_schema_history
				WHERE success = true
				""", Integer.class);
	}
}

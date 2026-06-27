package ru.marketplace.finance.finance.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.UserRepository;
import ru.marketplace.finance.finance.domain.DailyFinanceEntry;
import ru.marketplace.finance.finance.infrastructure.persistence.DailyFinanceEntryRepository;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class DailyFinanceExportControllerTest {

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
	MockMvc mockMvc;

	@Autowired
	UserRepository userRepository;

	@Autowired
	DailyFinanceEntryRepository dailyRepository;

	@Test
	void exportsDailyReportAsCsv() throws Exception {
		User user = userRepository.saveAndFlush(new User(
				"seller-csv-export@example.com",
				"$2a$10$hash",
				"Seller"));
		dailyRepository.saveAndFlush(productRow(user.getId(), LocalDate.of(2026, 6, 21)));
		dailyRepository.saveAndFlush(commonRow(user.getId(), LocalDate.of(2026, 6, 21)));

		byte[] response = mockMvc.perform(get("/api/reports/daily/export.csv")
						.with(user("seller"))
						.param("userId", user.getId().toString())
						.param("dateFrom", "2026-06-21")
						.param("dateTo", "2026-06-21"))
				.andExpect(status().isOk())
				.andExpect(header().string(
						"Content-Disposition",
						"attachment; filename=\"daily-finance-2026-06-21-2026-06-21.csv\""))
				.andReturn()
				.getResponse()
				.getContentAsByteArray();

		String csv = new String(response, StandardCharsets.UTF_8);
		assertThat(csv).startsWith("\uFEFFbusiness_date;nm_id;product_name");
		assertThat(csv).containsSubsequence(
				"2026-06-21;;;0;0;0",
				"2026-06-21;123456789;Test Product;1;0;1;1000");
		assertThat(csv).contains("2026-06-21;123456789;Test Product;1;0;1;1000");
		assertThat(csv).contains(";100;0;700;true");
	}

	@Test
	void exportsDailyReportAsXlsx() throws Exception {
		User user = userRepository.saveAndFlush(new User(
				"seller-xlsx-export@example.com",
				"$2a$10$hash",
				"Seller"));
		dailyRepository.saveAndFlush(productRow(user.getId(), LocalDate.of(2026, 6, 21)));
		dailyRepository.saveAndFlush(commonRow(user.getId(), LocalDate.of(2026, 6, 21)));

		byte[] response = mockMvc.perform(get("/api/reports/daily/export.xlsx")
						.with(user("seller"))
						.param("userId", user.getId().toString())
						.param("dateFrom", "2026-06-21")
						.param("dateTo", "2026-06-21"))
				.andExpect(status().isOk())
				.andExpect(header().string(
						"Content-Disposition",
						"attachment; filename=\"daily-finance-2026-06-21-2026-06-21.xlsx\""))
				.andReturn()
				.getResponse()
				.getContentAsByteArray();

		try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response))) {
			Sheet sheet = workbook.getSheet("Report");
			assertThat(sheet).isNotNull();
			assertThat(sheet.getRow(0).getCell(0).getStringCellValue())
					.isEqualTo("Финансовая аналитика - расширенный отчет");
			assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Товар");
			assertThat(sheet.getRow(4).getCell(0).getStringCellValue()).isEqualTo("Test Product");
			assertThat(sheet.getRow(4).getCell(1).getNumericCellValue()).isEqualTo(123456789D);
			assertThat(sheet.getRow(4).getCell(10).getNumericCellValue()).isEqualTo(700D);
			assertThat(sheet.getRow(5).getCell(0).getStringCellValue()).isEqualTo("Общие удержания");
			assertThat(sheet.getRow(5).getCell(7).getNumericCellValue()).isEqualTo(250D);
		}
	}

	private static DailyFinanceEntry commonRow(Long userId, LocalDate businessDate) {
		DailyFinanceEntry entry = DailyFinanceEntry.commonRow(userId, businessDate, 1);
		entry.replaceCommonExpenses(
				BigDecimal.ZERO,
				new BigDecimal("50.00"),
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				new BigDecimal("200.00"));
		return entry;
	}

	private static DailyFinanceEntry productRow(Long userId, LocalDate businessDate) {
		DailyFinanceEntry entry = DailyFinanceEntry.productRow(userId, businessDate, 123456789L, "Test Product", 1);
		entry.replaceProductTotals(
				1,
				0,
				new BigDecimal("1000.00"),
				BigDecimal.ZERO,
				new BigDecimal("180.00"),
				BigDecimal.ZERO,
				new BigDecimal("20.00"),
				new BigDecimal("100.00"),
				BigDecimal.ZERO,
				new BigDecimal("700.00"),
				true);
		return entry;
	}
}

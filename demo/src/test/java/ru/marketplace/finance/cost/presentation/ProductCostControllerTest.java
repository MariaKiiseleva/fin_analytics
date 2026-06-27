package ru.marketplace.finance.cost.presentation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.UserRepository;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ProductCostControllerTest {

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

	@Test
	void savesProductCostAndReturnsHistory() throws Exception {
		User user = userRepository.saveAndFlush(new User(
				"seller-product-cost-controller@example.com",
				"$2a$10$hash",
				"Seller"));

		mockMvc.perform(post("/api/product-costs")
						.with(user("seller"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": %d,
								  "nmId": 125167917,
								  "productName": "Test product",
								  "validFrom": "2026-06-01",
								  "costAmount": 650.00
								}
								""".formatted(user.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(user.getId()))
				.andExpect(jsonPath("$.nmId").value(125167917))
				.andExpect(jsonPath("$.productName").value("Test product"))
				.andExpect(jsonPath("$.validFrom").value("2026-06-01"))
				.andExpect(jsonPath("$.costAmount").value(650.00));

		mockMvc.perform(get("/api/product-costs")
						.with(user("seller"))
						.param("userId", user.getId().toString())
						.param("nmId", "125167917"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].userId").value(user.getId()))
				.andExpect(jsonPath("$[0].nmId").value(125167917))
				.andExpect(jsonPath("$[0].costAmount").value(650.00));
	}

	@Test
	void savesProductCostsBatch() throws Exception {
		User user = userRepository.saveAndFlush(new User(
				"seller-product-cost-batch-controller@example.com",
				"$2a$10$hash",
				"Seller"));

		mockMvc.perform(post("/api/product-costs/batch")
						.with(user("seller"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": %d,
								  "items": [
								    {
								      "nmId": 125167917,
								      "productName": "First product",
								      "validFrom": "2026-06-01",
								      "costAmount": 650.00
								    },
								    {
								      "nmId": 164230893,
								      "productName": "Second product",
								      "validFrom": "2026-06-01",
								      "costAmount": 300.00
								    }
								  ]
								}
								""".formatted(user.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$[0].userId").value(user.getId()))
				.andExpect(jsonPath("$[0].nmId").value(125167917))
				.andExpect(jsonPath("$[0].costAmount").value(650.00))
				.andExpect(jsonPath("$[1].nmId").value(164230893))
				.andExpect(jsonPath("$[1].costAmount").value(300.00));
	}

	@Test
	void importsProductCostsFromCsv() throws Exception {
		User user = userRepository.saveAndFlush(new User(
				"seller-product-cost-csv-controller@example.com",
				"$2a$10$hash",
				"Seller"));
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"missing-product-costs.csv",
				"text/csv",
				("""
						\uFEFFnm_id;product_name;first_business_date;last_business_date;rows_count;net_quantity;valid_from;cost_amount_to_fill
						164230893;First product;2026-06-21;2026-06-21;1;1;2026-06-01;300,50
						183795647;"Second; product";2026-06-21;2026-06-21;1;1;2026-06-01;120
						221236691;No cost product;2026-06-21;2026-06-21;1;1;2026-06-01;
						""").getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/product-costs/import.csv")
						.file(file)
						.param("userId", user.getId().toString())
						.with(user("seller"))
						.with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.parsedRows").value(2))
				.andExpect(jsonPath("$.savedRows").value(2))
				.andExpect(jsonPath("$.savedCosts[0].nmId").value(164230893))
				.andExpect(jsonPath("$.savedCosts[0].costAmount").value(300.50))
				.andExpect(jsonPath("$.savedCosts[1].productName").value("Second; product"));
	}

	@Test
	void importsProductCostsFromExcel() throws Exception {
		User user = userRepository.saveAndFlush(new User(
				"seller-product-cost-excel-controller@example.com",
				"$2a$10$hash",
				"Seller"));
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"product-costs.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				productCostsWorkbook());

		mockMvc.perform(multipart("/api/product-costs/import.xlsx")
						.file(file)
						.param("userId", user.getId().toString())
						.with(user("seller"))
						.with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.parsedRows").value(2))
				.andExpect(jsonPath("$.savedRows").value(2))
				.andExpect(jsonPath("$.savedCosts[0].nmId").value(164230893))
				.andExpect(jsonPath("$.savedCosts[0].costAmount").value(300.50))
				.andExpect(jsonPath("$.savedCosts[1].productName").value("Second product"));
	}

	private static byte[] productCostsWorkbook() throws IOException {
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Costs");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("nm_id");
			header.createCell(1).setCellValue("product_name");
			header.createCell(2).setCellValue("cost_price");
			header.createCell(3).setCellValue("effective_from");

			Row first = sheet.createRow(1);
			first.createCell(0).setCellValue(164230893);
			first.createCell(1).setCellValue("First product");
			first.createCell(2).setCellValue(300.50);
			first.createCell(3).setCellValue("2026-06-01");

			Row second = sheet.createRow(2);
			second.createCell(0).setCellValue(183795647);
			second.createCell(1).setCellValue("Second product");
			second.createCell(2).setCellValue("120,25");
			second.createCell(3).setCellValue("01.06.2026");

			Row skipped = sheet.createRow(3);
			skipped.createCell(0).setCellValue(221236691);
			skipped.createCell(1).setCellValue("Skipped product");
			skipped.createCell(3).setCellValue("2026-06-01");

			workbook.write(output);
			return output.toByteArray();
		}
	}
}

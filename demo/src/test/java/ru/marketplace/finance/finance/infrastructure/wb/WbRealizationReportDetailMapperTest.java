package ru.marketplace.finance.finance.infrastructure.wb;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import ru.marketplace.finance.finance.application.RawFinancialOperationImportRow;

class WbRealizationReportDetailMapperTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final WbRealizationReportDetailMapper mapper = new WbRealizationReportDetailMapper(objectMapper);

	@Test
	void mapsRealizationReportDetailRowIntoRawImportRow() throws Exception {
		JsonNode source = objectMapper.readTree("""
				{
				  "rrd_id": 123456789,
				  "srid": "order-srid-1",
				  "nm_id": 987654321,
				  "supplier_oper_name": "Продажа",
				  "doc_type_name": "Продажа",
				  "order_dt": "2026-06-15T10:15:30",
				  "sale_dt": "2026-06-15T11:20:00",
				  "rr_dt": "2026-06-16T00:30:00",
				  "create_dt": "2026-06-16T01:00:00",
				  "quantity": 2,
				  "retail_amount": "1 500,50",
				  "retail_price_withdisc_rub": 1400.25,
				  "ppvz_for_pay": 1200.10,
				  "ppvz_sales_commission": 180.15,
				  "delivery_rub": 35.40,
				  "rebill_logistic_cost": 5.60,
				  "ppvz_reward": 7.80,
				  "acquiring_fee": 20,
				  "storage_fee": 3.50,
				  "acceptance": 4.50,
				  "penalty": 10,
				  "deduction": 11
				}
				""");

		RawFinancialOperationImportRow result = mapper.map(source);

		assertThat(result.externalOperationId()).isEqualTo("123456789");
		assertThat(result.srid()).isEqualTo("order-srid-1");
		assertThat(result.nmId()).isEqualTo(987654321L);
		assertThat(result.supplierOperationName()).isEqualTo("Продажа");
		assertThat(result.documentType()).isEqualTo("Продажа");
		assertThat(result.orderAt()).isEqualTo(Instant.parse("2026-06-15T07:15:30Z"));
		assertThat(result.saleAt()).isEqualTo(Instant.parse("2026-06-15T08:20:00Z"));
		assertThat(result.reportAt()).isEqualTo(Instant.parse("2026-06-15T21:30:00Z"));
		assertThat(result.createdAt()).isEqualTo(Instant.parse("2026-06-15T22:00:00Z"));
		assertThat(result.quantity()).isEqualTo(2);
		assertThat(result.retailAmount()).isEqualByComparingTo("1500.50");
		assertThat(result.retailAmountWithDiscount()).isEqualByComparingTo("1400.25");
		assertThat(result.sellerAmount()).isEqualByComparingTo("1200.10");
		assertThat(result.commissionAmount()).isEqualByComparingTo("180.15");
		assertThat(result.logisticsAmount()).isEqualByComparingTo("35.40");
		assertThat(result.rebillLogisticsAmount()).isEqualByComparingTo("5.60");
		assertThat(result.pvzRewardAmount()).isEqualByComparingTo("7.80");
		assertThat(result.acquiringAmount()).isEqualByComparingTo("20");
		assertThat(result.storageAmount()).isEqualByComparingTo("3.50");
		assertThat(result.acceptanceAmount()).isEqualByComparingTo("4.50");
		assertThat(result.penaltyAmount()).isEqualByComparingTo("10");
		assertThat(result.deductionAmount()).isEqualByComparingTo("11");
		assertThat(result.rawPayload()).contains("\"supplier_oper_name\":\"Продажа\"");
	}

	@Test
	void fallsBackFromRetailAmountToRetailPrice() throws Exception {
		JsonNode source = objectMapper.readTree("""
				{
				  "rrd_id": 1,
				  "retail_price": 999.99
				}
				""");

		RawFinancialOperationImportRow result = mapper.map(source);

		assertThat(result.retailAmount()).isEqualByComparingTo("999.99");
	}
}

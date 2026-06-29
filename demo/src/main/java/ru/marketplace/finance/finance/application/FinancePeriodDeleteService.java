package ru.marketplace.finance.finance.application;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.marketplace.finance.account.infrastructure.UserRepository;
import ru.marketplace.finance.finance.infrastructure.persistence.DailyFinanceEntryRepository;
import ru.marketplace.finance.finance.infrastructure.persistence.RawFinancialOperationRepository;

@Service
public class FinancePeriodDeleteService {

	private final UserRepository userRepository;
	private final RawFinancialOperationRepository rawRepository;
	private final DailyFinanceEntryRepository dailyRepository;

	public FinancePeriodDeleteService(
			UserRepository userRepository,
			RawFinancialOperationRepository rawRepository,
			DailyFinanceEntryRepository dailyRepository) {
		this.userRepository = userRepository;
		this.rawRepository = rawRepository;
		this.dailyRepository = dailyRepository;
	}

	@Transactional
	public FinancePeriodDeleteResult deletePeriod(Long userId, LocalDate dateFrom, LocalDate dateTo) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("userId must be positive");
		}
		if (!userRepository.existsById(userId)) {
			throw new IllegalArgumentException("User not found: " + userId);
		}
		if (dateFrom == null || dateTo == null) {
			throw new IllegalArgumentException("dateFrom and dateTo must not be null");
		}
		if (dateFrom.isAfter(dateTo)) {
			throw new IllegalArgumentException("dateFrom must not be after dateTo");
		}

		long deletedDailyRows = dailyRepository.deleteByUserIdAndBusinessDateBetween(userId, dateFrom, dateTo);
		long deletedRawRows = rawRepository.deleteByUserIdAndBusinessDateBetween(userId, dateFrom, dateTo);
		return new FinancePeriodDeleteResult(deletedRawRows, deletedDailyRows);
	}
}

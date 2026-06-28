package ru.marketplace.finance.synchronization.infrastructure;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.marketplace.finance.synchronization.domain.SyncJob;
import ru.marketplace.finance.synchronization.domain.SyncStatus;

public interface SyncJobRepository extends JpaRepository<SyncJob, Long> {

	List<SyncJob> findByUserIdOrderByRequestedAtDesc(Long userId);

	List<SyncJob> findByUserIdAndDateFromLessThanEqualAndDateToGreaterThanEqualOrderByRequestedAtAsc(
			Long userId,
			java.time.LocalDate dateTo,
			java.time.LocalDate dateFrom);

	boolean existsByUserIdAndStatusIn(Long userId, Collection<SyncStatus> statuses);
}

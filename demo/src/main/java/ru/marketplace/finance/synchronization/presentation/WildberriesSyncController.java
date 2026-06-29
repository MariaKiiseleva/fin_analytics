package ru.marketplace.finance.synchronization.presentation;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.synchronization.application.WbFinanceSyncResult;
import ru.marketplace.finance.synchronization.application.WbFinanceSyncService;
import ru.marketplace.finance.synchronization.infrastructure.SyncJobRepository;
import ru.marketplace.finance.security.CurrentUserService;

@RestController
@RequestMapping("/api/sync")
@Validated
public class WildberriesSyncController {

	private final WbFinanceSyncService syncService;
	private final SyncJobRepository syncJobRepository;
	private final CurrentUserService currentUserService;

	public WildberriesSyncController(
			WbFinanceSyncService syncService,
			SyncJobRepository syncJobRepository,
			CurrentUserService currentUserService) {
		this.syncService = syncService;
		this.syncJobRepository = syncJobRepository;
		this.currentUserService = currentUserService;
	}

	@PostMapping("/wildberries")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public WbFinanceSyncResult startWildberriesSync(
			@Valid @RequestBody StartWildberriesSyncRequest request,
			HttpSession session) {
		currentUserService.requireSameUser(session, request.userId());
		return syncService.syncWithSavedToken(request.userId(), request.dateFrom(), request.dateTo());
	}

	@GetMapping("/jobs")
	public List<SyncJobView> findSyncJobs(@RequestParam @Positive Long userId, HttpSession session) {
		currentUserService.requireSameUser(session, userId);
		return syncJobRepository.findByUserIdOrderByRequestedAtDesc(userId).stream()
				.map(SyncJobView::from)
				.toList();
	}
}

package ru.marketplace.finance.account.presentation;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.account.application.AdminUserCardView;
import ru.marketplace.finance.account.application.AdminUserService;
import ru.marketplace.finance.security.CurrentUserService;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

	private final AdminUserService adminUserService;
	private final CurrentUserService currentUserService;

	public AdminUserController(AdminUserService adminUserService, CurrentUserService currentUserService) {
		this.adminUserService = adminUserService;
		this.currentUserService = currentUserService;
	}

	@GetMapping
	public List<AdminUserCardView> findUsers(HttpSession session) {
		currentUserService.requireAdmin(session);
		return adminUserService.findUsers();
	}
}

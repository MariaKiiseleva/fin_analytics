package ru.marketplace.finance.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.marketplace.finance.account.application.AuthService;
import ru.marketplace.finance.account.application.AuthUserView;
import ru.marketplace.finance.account.domain.UserRole;

@Service
public class CurrentUserService {

	public static final String USER_ID_SESSION_ATTRIBUTE = "AUTH_USER_ID";

	private final AuthService authService;

	public CurrentUserService(AuthService authService) {
		this.authService = authService;
	}

	public AuthUserView requireCurrentUser(HttpSession session) {
		AuthUserView currentUser = authService.findCurrentUser((Long) session.getAttribute(USER_ID_SESSION_ATTRIBUTE));
		if (currentUser == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		return currentUser;
	}

	public Long requireCurrentUserId(HttpSession session) {
		return requireCurrentUser(session).userId();
	}

	public Long requireSameUser(HttpSession session, Long requestedUserId) {
		Long currentUserId = requireCurrentUserId(session);
		if (!currentUserId.equals(requestedUserId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access to another user is forbidden");
		}
		return currentUserId;
	}

	public void requireAdmin(HttpSession session) {
		AuthUserView currentUser = requireCurrentUser(session);
		if (currentUser.role() != UserRole.ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
		}
	}
}

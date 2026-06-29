package ru.marketplace.finance.account.presentation;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.account.application.AuthService;
import ru.marketplace.finance.account.application.AuthUserView;
import ru.marketplace.finance.security.CurrentUserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public AuthUserView register(@Valid @RequestBody RegisterRequest request, HttpSession session) {
		AuthUserView user = authService.register(request.email(), request.displayName(), request.password());
		session.setAttribute(CurrentUserService.USER_ID_SESSION_ATTRIBUTE, user.userId());
		return user;
	}

	@PostMapping("/login")
	public AuthUserView login(@Valid @RequestBody LoginRequest request, HttpSession session) {
		AuthUserView user = authService.login(request.email(), request.password());
		session.setAttribute(CurrentUserService.USER_ID_SESSION_ATTRIBUTE, user.userId());
		return user;
	}

	@GetMapping("/me")
	public AuthUserView me(HttpSession session) {
		return authService.findCurrentUser((Long) session.getAttribute(CurrentUserService.USER_ID_SESSION_ATTRIBUTE));
	}

	@PostMapping("/logout")
	public void logout(HttpSession session) {
		session.invalidate();
	}
}

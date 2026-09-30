package com.learning.atm.controller;

import com.learning.atm.dto.LoginRequest;
import com.learning.atm.dto.LoginResponse;
import com.learning.atm.dto.SessionInfo;
import com.learning.atm.security.AuthInterceptor;
import com.learning.atm.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login / logout — the only endpoints reachable without a token (see WebConfig). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest) {
		// The user agent is stored so the "active sessions" screen can say which device this is.
		return authService.login(request, httpRequest.getHeader("User-Agent"));
	}

	@PostMapping("/logout")
	public Map<String, String> logout(@RequestAttribute(AuthInterceptor.TOKEN_ATTRIBUTE) String token) {
		authService.logout(token);
		return Map.of("message", "Logged out");
	}

	/**
	 * Every device currently signed in to the caller's account.
	 *
	 * <p>Learning note: the service's internal {@code Session} type is mapped to a DTO before
	 * it leaves the service, so nothing about the session's internal shape (including the
	 * tokens) can become part of the public API contract by accident.
	 */
	@GetMapping("/sessions")
	public List<SessionInfo> sessions(
			@RequestAttribute(AuthInterceptor.ACCOUNT_ID_ATTRIBUTE) long accountId,
			@RequestAttribute(AuthInterceptor.TOKEN_ATTRIBUTE) String token) {
		return authService.sessionViews(accountId, token);
	}
}

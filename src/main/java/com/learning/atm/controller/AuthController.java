package com.learning.atm.controller;

import com.learning.atm.dto.LoginRequest;
import com.learning.atm.dto.LoginResponse;
import com.learning.atm.security.AuthInterceptor;
import com.learning.atm.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
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
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/logout")
	public Map<String, String> logout(@RequestAttribute(AuthInterceptor.TOKEN_ATTRIBUTE) String token) {
		authService.logout(token);
		return Map.of("message", "Logged out");
	}
}

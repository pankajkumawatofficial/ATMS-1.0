package com.learning.atm.security;

import com.learning.atm.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Guards every API path registered in {@link com.learning.atm.config.WebConfig}.
 *
 * <p>Learning notes: an interceptor runs <b>before</b> the controller. Here it demands an
 * {@code Authorization: Bearer <token>} header, checks the token with {@link AuthService}
 * and — when everything is fine — copies the account id into a request attribute that the
 * controllers can read with {@code @RequestAttribute}. When the token is missing or unknown
 * it short-circuits with a 401 and the controller is never reached.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

	public static final String ACCOUNT_ID_ATTRIBUTE = "authenticatedAccountId";
	public static final String TOKEN_ATTRIBUTE = "authenticatedToken";

	private final AuthService authService;

	public AuthInterceptor(AuthService authService) {
		this.authService = authService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		String header = request.getHeader("Authorization");
		String token = (header != null && header.startsWith("Bearer "))
				? header.substring(7).trim()
				: null;

		Long accountId = authService.authenticate(token);
		if (accountId == null) {
			// Written by hand: the interceptor runs before message converters are involved.
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write(
					"{\"status\":401,\"error\":\"Unauthorized\","
					+ "\"message\":\"Missing or invalid bearer token\"}");
			return false;
		}

		request.setAttribute(ACCOUNT_ID_ATTRIBUTE, accountId);
		request.setAttribute(TOKEN_ATTRIBUTE, token);
		return true;
	}
}

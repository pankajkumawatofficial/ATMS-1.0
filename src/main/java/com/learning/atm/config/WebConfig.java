package com.learning.atm.config;

import com.learning.atm.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Wires the {@link AuthInterceptor} into Spring MVC.
 *
 * <p>Everything under {@code /api/**} requires a token except the two entry points that
 * must be reachable without one: logging in, and opening an account.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final AuthInterceptor authInterceptor;

	public WebConfig(AuthInterceptor authInterceptor) {
		this.authInterceptor = authInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(authInterceptor)
				.addPathPatterns("/api/**")
				// The only two paths reachable without a token: logging in, and opening an
				// account. Every sub-path of /api/accounts (/{id}/balance, /{id}/statement, …)
				// still goes through the interceptor.
				.excludePathPatterns("/api/auth/login", "/api/accounts");
	}
}

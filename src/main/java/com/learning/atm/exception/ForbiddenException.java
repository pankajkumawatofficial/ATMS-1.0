package com.learning.atm.exception;

/**
 * Thrown when a caller tries to touch an account that does not belong to the logged-in
 * session — mapped to HTTP 403. This is the "authorization" counterpart of authentication.
 */
public class ForbiddenException extends RuntimeException {

	public ForbiddenException(String message) {
		super(message);
	}
}

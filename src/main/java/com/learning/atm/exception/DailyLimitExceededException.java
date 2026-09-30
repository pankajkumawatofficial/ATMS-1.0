package com.learning.atm.exception;

import java.math.BigDecimal;

/**
 * Thrown when a withdrawal would push the account past its daily cash limit.
 *
 * <p>Mapped to HTTP 400. The message reports the whole remaining allowance for the day, not
 * just this attempt, so the customer knows exactly how much they can still take out.
 */
public class DailyLimitExceededException extends RuntimeException {

	public DailyLimitExceededException(BigDecimal limit, BigDecimal withdrawnToday,
			BigDecimal requested) {
		super("Daily withdrawal limit of " + limit + " reached — already withdrawn "
				+ withdrawnToday + " today, " + requested + " requested, "
				+ withdrawnToday.subtract(requested).max(BigDecimal.ZERO)
				+ " still available today");
	}
}

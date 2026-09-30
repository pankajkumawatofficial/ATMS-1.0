package com.learning.atm.dto;

import java.math.BigDecimal;

/**
 * Balance plus the limit information the ATM screen shows.
 *
 * @param withdrawnToday cash already taken out since midnight — the running total the daily
 *                       limit is checked against
 * @param remainingToday how much more can be withdrawn today before the limit bites;
 *                       {@code null} when the account has no limit
 */
public record BalanceResponse(
		long accountId,
		String accountNumber,
		BigDecimal balance,
		BigDecimal dailyLimit,
		BigDecimal withdrawnToday,
		BigDecimal remainingToday) {

	/**
	 * Builds the response from the three facts the service has.
	 *
	 * <p>A {@code null} limit is passed through as {@code null} for {@code remainingToday} too —
	 * the UI shows "no limit" instead of an absurdly large number.
	 */
	public static BalanceResponse of(long accountId, String accountNumber, BigDecimal balance,
			BigDecimal dailyLimit, BigDecimal withdrawnToday) {
		BigDecimal remaining = (dailyLimit == null)
				? null
				: dailyLimit.subtract(withdrawnToday).max(BigDecimal.ZERO);
		return new BalanceResponse(accountId, accountNumber, balance, dailyLimit,
				withdrawnToday, remaining);
	}
}

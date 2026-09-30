package com.learning.atm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * A bank account. Maps to the {@code accounts} table.
 *
 * <p>Learning notes:
 * <ul>
 *   <li>Money is stored as {@link BigDecimal}, never {@code double} — doubles introduce
 *       rounding errors (0.1 + 0.2 != 0.3).</li>
 *   <li>The column constraints ({@code nullable}, {@code unique}, {@code length},
 *       {@code precision}/{@code scale}) are applied to the database by Hibernate
 *       because {@code spring.jpa.hibernate.ddl-auto=update}.</li>
 *   <li>JPA requires a no-arg constructor; the other constructor is for application code.</li>
 * </ul>
 */
@Entity
@Table(name = "accounts")
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "account_number", nullable = false, unique = true, length = 20)
	private String accountNumber;

	/**
	 * Demo only: a real system must store a salted hash (e.g. BCrypt), never the PIN itself.
	 */
	@Column(nullable = false, length = 6)
	private String pin;

	@Column(name = "owner_name", nullable = false, length = 100)
	private String ownerName;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal balance;

	/**
	 * Most that can be withdrawn in one calendar day. Nullable on purpose: rows created
	 * before this column existed stay {@code null}, and {@code null} is treated as
	 * "no limit" everywhere ({@code effectiveDailyLimit()}). A NOT NULL column would have
	 * forced a backfill migration on the existing demo data.
	 */
	@Column(name = "daily_withdrawal_limit", precision = 12, scale = 2)
	private BigDecimal dailyWithdrawalLimit;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected Account() {
		// used by JPA
	}

	public Account(String accountNumber, String pin, String ownerName, BigDecimal balance) {
		this(accountNumber, pin, ownerName, balance, null);
	}

	public Account(String accountNumber, String pin, String ownerName, BigDecimal balance,
			BigDecimal dailyWithdrawalLimit) {
		this.accountNumber = accountNumber;
		this.pin = pin;
		this.ownerName = ownerName;
		this.balance = balance;
		this.dailyWithdrawalLimit = dailyWithdrawalLimit;
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		if (balance == null) {
			balance = BigDecimal.ZERO;
		}
		this.balance = this.balance.setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * The limit to enforce, normalized to 2 decimals.
	 *
	 * <p>{@code null} means <b>no limit</b>. Returning a huge sentinel number instead would be
	 * a trap: {@code BigDecimal} has no {@code MAX_VALUE}, and any stand-in would eventually
	 * be printed to a customer or stored in the ledger.
	 */
	public BigDecimal effectiveDailyLimit() {
		return dailyWithdrawalLimit;
	}

	// -------------------------------------------------------------------------
	// Getters / setters
	// -------------------------------------------------------------------------

	public Long getId() {
		return id;
	}

	/** Only used so unit tests can simulate rows that already exist in the database. */
	public void setId(Long id) {
		this.id = id;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public String getPin() {
		return pin;
	}

	public void setPin(String pin) {
		this.pin = pin;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public BigDecimal getDailyWithdrawalLimit() {
		return dailyWithdrawalLimit;
	}

	public void setDailyWithdrawalLimit(BigDecimal dailyWithdrawalLimit) {
		this.dailyWithdrawalLimit = dailyWithdrawalLimit;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}

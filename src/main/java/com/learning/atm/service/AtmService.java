package com.learning.atm.service;

import com.learning.atm.dto.TransactionResponse;
import com.learning.atm.exception.AccountNotFoundException;
import com.learning.atm.exception.DailyLimitExceededException;
import com.learning.atm.exception.InsufficientFundsException;
import com.learning.atm.exception.InvalidPinException;
import com.learning.atm.model.Account;
import com.learning.atm.model.Transaction;
import com.learning.atm.model.TransactionType;
import com.learning.atm.repository.AccountRepository;
import com.learning.atm.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Money movement: deposit, withdraw, transfer.
 *
 * <p>Learning notes — this is where an ATM gets interesting:
 * <ul>
 *   <li>Every method runs inside {@code @Transactional}: the balance change and the ledger
 *       row commit together or not at all (atomicity — the "A" in ACID).</li>
 *   <li>Accounts are loaded with {@code findByIdForUpdate} ({@code SELECT ... FOR UPDATE}),
 *       so two concurrent withdrawals queue up instead of both reading the old balance.</li>
 *   <li>Transfers lock both accounts <b>in ascending id order</b> to avoid deadlocks when
 *       two people transfer to each other at the same instant.</li>
 *   <li>Withdrawals and transfers re-check the PIN, exactly like a real ATM asks again
 *       before dispensing cash.</li>
 * </ul>
 */
@Service
public class AtmService {

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;

	public AtmService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
	}

	@Transactional
	public TransactionResponse deposit(long accountId, BigDecimal amount) {
		Account account = lockAccount(accountId);
		account.setBalance(account.getBalance().add(amount));
		return record(account, TransactionType.DEPOSIT, amount, null);
	}

	@Transactional
	public TransactionResponse withdraw(long accountId, BigDecimal amount, String pin) {
		Account account = lockAccount(accountId);
		verifyPin(account, pin);
		if (account.getBalance().compareTo(amount) < 0) {
			throw new InsufficientFundsException(account.getBalance(), amount);
		}
		enforceDailyLimit(account, amount);
		account.setBalance(account.getBalance().subtract(amount));
		return record(account, TransactionType.WITHDRAWAL, amount, null);
	}

	/**
	 * Cash already taken out today. Public because the balance endpoint reports it so the UI
	 * can show "₹3,000 of ₹10,000 used today" without a second request.
	 *
	 * @param accountId the account to total; does not check ownership — callers must
	 *                  authorize the account id themselves
	 */
	@Transactional(readOnly = true)
	public BigDecimal withdrawnToday(long accountId) {
		return Transaction.money(transactionRepository.sumAmountSince(
				accountId, TransactionType.WITHDRAWAL, startOfToday()));
	}

	/**
	 * Blocks a withdrawal that would exceed the account's daily cash limit.
	 *
	 * <p>Learning notes:
	 * <ul>
	 *   <li>The day boundary is <b>midnight tonight</b>, so the allowance resets on the
	 *       calendar rather than 24 hours after the last withdrawal.</li>
	 *   <li>It runs <b>after</b> the funds check on purpose. A customer with a ₹500 balance and
	 *       a ₹10,000 daily limit who asks for ₹900 should be told "insufficient funds", not
	 *       "limit reached" — the more specific, more actionable message comes first.</li>
	 *   <li>The account is already locked by the caller, so the sum-then-check cannot race with
	 *       another withdrawal in another thread.</li>
	 * </ul>
	 */
	private void enforceDailyLimit(Account account, BigDecimal amount) {
		BigDecimal limit = account.effectiveDailyLimit();
		if (limit == null) {
			return; // no limit configured on this account
		}
		BigDecimal withdrawnToday = withdrawnToday(account.getId());
		if (withdrawnToday.add(amount).compareTo(limit) > 0) {
			throw new DailyLimitExceededException(limit, withdrawnToday, amount);
		}
	}

	private static LocalDateTime startOfToday() {
		return LocalDate.now().atStartOfDay();
	}

	/**
	 * Moves money between two accounts in a single transaction: either both sides change,
	 * or neither does (a crash in the middle can never create or destroy money).
	 *
	 * @return both ledger entries (the debit on the sender and the credit on the receiver)
	 */
	@Transactional
	public List<TransactionResponse> transfer(long fromId, String toAccountNumber,
			BigDecimal amount, String pin) {

		Account target = accountRepository.findByAccountNumber(toAccountNumber)
				.orElseThrow(() -> new AccountNotFoundException(toAccountNumber));
		if (target.getId() == fromId) {
			throw new IllegalArgumentException("Cannot transfer to the same account");
		}

		// Lock in ascending id order so opposite transfers can never deadlock each other.
		long firstId = Math.min(fromId, target.getId());
		long secondId = Math.max(fromId, target.getId());
		Account first = lockAccount(firstId);
		Account second = lockAccount(secondId);
		Account from = (fromId == firstId) ? first : second;
		Account to = (fromId == firstId) ? second : first;

		verifyPin(from, pin);
		if (from.getBalance().compareTo(amount) < 0) {
			throw new InsufficientFundsException(from.getBalance(), amount);
		}

		from.setBalance(from.getBalance().subtract(amount));
		to.setBalance(to.getBalance().add(amount));

		TransactionResponse debit = record(from, TransactionType.TRANSFER_OUT, amount, to.getAccountNumber());
		TransactionResponse credit = record(to, TransactionType.TRANSFER_IN, amount, from.getAccountNumber());
		return List.of(debit, credit);
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private Account lockAccount(long id) {
		return accountRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new AccountNotFoundException(String.valueOf(id)));
	}

	private void verifyPin(Account account, String pin) {
		if (!account.getPin().equals(pin)) {
			throw new InvalidPinException();
		}
	}

	/**
	 * Writes one ledger row with the balance snapshot and normalizes the money to 2 decimals.
	 * The returned DTO has its id because {@code IDENTITY} columns are populated on save.
	 */
	private TransactionResponse record(Account account, TransactionType type, BigDecimal amount,
			String counterpartyAccountNumber) {
		BigDecimal balanceAfter = Transaction.money(account.getBalance());
		account.setBalance(balanceAfter);
		Transaction transaction = new Transaction(account, type, Transaction.money(amount),
				balanceAfter, counterpartyAccountNumber);
		transactionRepository.save(transaction);
		return TransactionResponse.from(transaction);
	}
}

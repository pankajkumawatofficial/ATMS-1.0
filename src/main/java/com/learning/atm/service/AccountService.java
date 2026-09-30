package com.learning.atm.service;

import com.learning.atm.dto.AccountLookupResponse;
import com.learning.atm.dto.AccountResponse;
import com.learning.atm.dto.AddBeneficiaryRequest;
import com.learning.atm.dto.BalanceResponse;
import com.learning.atm.dto.CreateAccountRequest;
import com.learning.atm.dto.PageResponse;
import com.learning.atm.dto.TransactionResponse;
import com.learning.atm.exception.AccountNotFoundException;
import com.learning.atm.exception.DuplicateAccountNumberException;
import com.learning.atm.exception.InvalidPinException;
import com.learning.atm.model.Account;
import com.learning.atm.model.Transaction;
import com.learning.atm.model.TransactionType;
import com.learning.atm.repository.AccountRepository;
import com.learning.atm.repository.TransactionRepository;
import com.learning.atm.repository.TransactionSpecifications;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side operations plus account creation. */
@Service
public class AccountService {

	/** How many statements one page may hold. A hard ceiling stops a client asking for 100000. */
	static final int MAX_PAGE_SIZE = 100;

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final AtmService atmService;

	public AccountService(AccountRepository accountRepository,
			TransactionRepository transactionRepository,
			AtmService atmService) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.atmService = atmService;
	}

	/**
	 * Creates an account. {@code @Transactional} makes the whole method one database
	 * transaction: if anything throws, every change inside is rolled back.
	 */
	@Transactional
	public AccountResponse create(CreateAccountRequest request) {
		if (accountRepository.findByAccountNumber(request.accountNumber()).isPresent()) {
			throw new DuplicateAccountNumberException(request.accountNumber());
		}
		BigDecimal initial = request.initialBalance() == null ? BigDecimal.ZERO : request.initialBalance();
		Account saved = accountRepository.save(
				new Account(request.accountNumber(), request.pin(), request.ownerName(), initial));
		return AccountResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public AccountResponse get(long id) {
		return AccountResponse.from(findAccount(id));
	}

	/**
	 * Balance plus today's usage of the daily withdrawal limit, so the home screen can show
	 * the remaining allowance without a second round trip.
	 */
	@Transactional(readOnly = true)
	public BalanceResponse balance(long id) {
		Account account = findAccount(id);
		return BalanceResponse.of(
				account.getId(),
				account.getAccountNumber(),
				account.getBalance(),
				account.getDailyWithdrawalLimit(),
				atmService.withdrawnToday(id));
	}

	/** The last {@code limit} movements — the "recent activity" strip on the home screen. */
	@Transactional(readOnly = true)
	public List<TransactionResponse> recent(long id, int limit) {
		findAccount(id); // 404 for unknown accounts instead of an empty list
		int capped = Math.clamp(limit, 1, 20);
		PageRequest newestFirst = PageRequest.of(0, capped);
		return transactionRepository
				.findByAccountIdOrderByCreatedAtDesc(id, newestFirst).stream()
				.map(TransactionResponse::from)
				.toList();
	}

	/** Unpaged statement — kept for the simple case and for the existing tests. */
	@Transactional(readOnly = true)
	public List<TransactionResponse> history(long id) {
		findAccount(id); // 404 for unknown accounts instead of an empty list
		return transactionRepository.findByAccountIdOrderByCreatedAtDesc(id).stream()
				.map(TransactionResponse::from)
				.toList();
	}

	/**
	 * One page of the statement, newest first, narrowed by whichever filters were supplied.
	 *
	 * <p>Learning notes:
	 * <ul>
	 *   <li>Sorting is declared here rather than in the method name. It is applied by
	 *       {@code PageRequest}, which is what lets the database return rows already ordered
	 *       before the count and the page slice happen.</li>
	 *   <li>{@code size} is clamped instead of trusted: without the ceiling, a request for
	 *       {@code size=1000000} would ask the database to load a whole ledger into memory.</li>
	 *   <li>The account is looked up first so an unknown id is a 404 rather than a cheerful
	 *       "empty statement" — the client can tell a real mistake from a real empty account.</li>
	 * </ul>
	 */
	@Transactional(readOnly = true)
	public PageResponse<TransactionResponse> statement(long id, int page, int size,
			TransactionType type, LocalDate from, LocalDate to,
			BigDecimal minAmount, BigDecimal maxAmount, String search) {

		findAccount(id);

		int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
		int safePage = Math.max(page, 0);
		Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"));

		Page<Transaction> result = transactionRepository.findAll(
				TransactionSpecifications.filter(id, type, from, to, minAmount, maxAmount, search),
				PageRequest.of(safePage, safeSize, newestFirst));

		return PageResponse.from(result, TransactionResponse::from);
	}

	/**
	 * Rotates the PIN after checking the current one.
	 *
	 * <p>Learning note: this is deliberately <b>not</b> a plain setter. A PIN change is the
	 * recovery path for a compromised account, so it has to prove the caller already knew the
	 * old PIN — otherwise anyone holding a stolen token could lock the real owner out by
	 * changing it.
	 */
	@Transactional
	public void changePin(long accountId, String currentPin, String newPin) {
		if (newPin.equals(currentPin)) {
			throw new IllegalArgumentException("The new PIN must be different from the current one");
		}
		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new AccountNotFoundException(String.valueOf(accountId)));
		if (!account.getPin().equals(currentPin)) {
			throw new InvalidPinException();
		}
		account.setPin(newPin);
	}

	/**
	 * Confirms whether an account number exists, and who it belongs to.
	 *
	 * <p>Learning note: a miss returns {@code exists=false} with a 200 rather than a 404. The
	 * transfer screen needs to say "no such account" as normal flow, and distinguishing
	 * "not found" from "found" is the entire point of the lookup.
	 */
	@Transactional(readOnly = true)
	public AccountLookupResponse lookup(String accountNumber) {
		return accountRepository.findByAccountNumber(accountNumber)
				.map(AccountLookupResponse::found)
				.orElseGet(() -> AccountLookupResponse.missing(accountNumber));
	}

	Account findAccount(long id) {
		return accountRepository.findById(id)
				.orElseThrow(() -> new AccountNotFoundException(String.valueOf(id)));
	}
}

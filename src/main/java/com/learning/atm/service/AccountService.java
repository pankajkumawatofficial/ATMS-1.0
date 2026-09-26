package com.learning.atm.service;

import com.learning.atm.dto.AccountResponse;
import com.learning.atm.dto.BalanceResponse;
import com.learning.atm.dto.CreateAccountRequest;
import com.learning.atm.dto.TransactionResponse;
import com.learning.atm.exception.AccountNotFoundException;
import com.learning.atm.exception.DuplicateAccountNumberException;
import com.learning.atm.model.Account;
import com.learning.atm.repository.AccountRepository;
import com.learning.atm.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side operations plus account creation. */
@Service
public class AccountService {

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;

	public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
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

	@Transactional(readOnly = true)
	public BalanceResponse balance(long id) {
		Account account = findAccount(id);
		return new BalanceResponse(account.getId(), account.getAccountNumber(), account.getBalance());
	}

	@Transactional(readOnly = true)
	public List<TransactionResponse> history(long id) {
		findAccount(id); // 404 for unknown accounts instead of an empty list
		return transactionRepository.findByAccountIdOrderByCreatedAtDesc(id).stream()
				.map(TransactionResponse::from)
				.toList();
	}

	Account findAccount(long id) {
		return accountRepository.findById(id)
				.orElseThrow(() -> new AccountNotFoundException(String.valueOf(id)));
	}
}

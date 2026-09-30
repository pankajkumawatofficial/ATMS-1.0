package com.learning.atm.config;

import com.learning.atm.model.Account;
import com.learning.atm.repository.AccountRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Loads demo data on startup so the API is testable immediately after the first run.
 * It only seeds when the table is empty, so restarting never duplicates data.
 */
@Component
public class DataSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

	private final AccountRepository accountRepository;

	public DataSeeder(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	@Override
	public void run(String... args) {
		if (accountRepository.count() > 0) {
			return;
		}
		BigDecimal dailyLimit = new BigDecimal("10000.00");
		accountRepository.save(new Account("1001", "1234", "Alice Sharma",
				new BigDecimal("5000.00"), dailyLimit));
		accountRepository.save(new Account("1002", "5678", "Bob Verma",
				new BigDecimal("2500.00"), dailyLimit));
		accountRepository.save(new Account("1003", "4321", "Carol Singh",
				new BigDecimal("1000.00"), dailyLimit));
		log.info("Seeded 3 demo accounts -> 1001/1234, 1002/5678, 1003/4321 "
				+ "(daily withdrawal limit {})", dailyLimit);
	}
}

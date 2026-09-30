package com.learning.atm.service;

import com.learning.atm.dto.AddBeneficiaryRequest;
import com.learning.atm.dto.BeneficiaryResponse;
import com.learning.atm.exception.AccountNotFoundException;
import com.learning.atm.exception.BeneficiaryNotFoundException;
import com.learning.atm.exception.DuplicateBeneficiaryException;
import com.learning.atm.model.Account;
import com.learning.atm.model.Beneficiary;
import com.learning.atm.repository.AccountRepository;
import com.learning.atm.repository.BeneficiaryRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saved payees ("beneficiaries") — the shortcut list behind a one-tap transfer.
 *
 * <p>Learning notes:
 * <ul>
 *   <li>Every method takes the <b>owner id from the session</b>, never from the request, so a
 *       caller can only ever see or touch their own payees.</li>
 *   <li>Adding a payee verifies the target account actually exists <b>and</b> is not the
 *       customer themselves — saving yourself is always a bug, not a feature.</li>
 *   <li>The duplicate check happens twice: a friendly pre-check for a clean 409, and a catch
 *       of the unique-constraint violation for the concurrent case where two clicks race
 *       through the pre-check together.</li>
 * </ul>
 */
@Service
public class BeneficiaryService {

	private final AccountRepository accountRepository;
	private final BeneficiaryRepository beneficiaryRepository;

	public BeneficiaryService(AccountRepository accountRepository,
			BeneficiaryRepository beneficiaryRepository) {
		this.accountRepository = accountRepository;
		this.beneficiaryRepository = beneficiaryRepository;
	}

	@Transactional(readOnly = true)
	public List<BeneficiaryResponse> list(long ownerId) {
		return beneficiaryRepository.findByOwnerIdOrderByNicknameAsc(ownerId).stream()
				.map(BeneficiaryResponse::from)
				.toList();
	}

	@Transactional
	public BeneficiaryResponse add(long ownerId, AddBeneficiaryRequest request) {
		Account owner = accountRepository.findById(ownerId)
				.orElseThrow(() -> new AccountNotFoundException(String.valueOf(ownerId)));

		Account target = accountRepository.findByAccountNumber(request.accountNumber())
				.orElseThrow(() -> new AccountNotFoundException(request.accountNumber()));

		if (target.getId().equals(ownerId)) {
			throw new IllegalArgumentException("You cannot add your own account as a beneficiary");
		}
		if (beneficiaryRepository.existsByOwnerIdAndAccountNumber(ownerId,
				request.accountNumber())) {
			throw new DuplicateBeneficiaryException(request.accountNumber());
		}

		try {
			Beneficiary saved = beneficiaryRepository.save(
					new Beneficiary(owner, target.getAccountNumber(), target.getOwnerName(),
							request.nickname().trim()));
			return BeneficiaryResponse.from(saved);
		} catch (DataIntegrityViolationException ex) {
			// Lost the race against a concurrent identical insert — the unique constraint held.
			throw new DuplicateBeneficiaryException(request.accountNumber());
		}
	}

	@Transactional
	public void remove(long ownerId, long beneficiaryId) {
		int deleted = beneficiaryRepository.deleteByOwnerIdAndId(ownerId, beneficiaryId);
		if (deleted == 0) {
			// Either it does not exist, or it belongs to someone else. Both are a 404 to the
			// caller: revealing "that payee exists but isn't yours" would leak information.
			throw new BeneficiaryNotFoundException(beneficiaryId);
		}
	}
}

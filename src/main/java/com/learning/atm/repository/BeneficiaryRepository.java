package com.learning.atm.repository;

import com.learning.atm.model.Beneficiary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

	List<Beneficiary> findByOwnerIdOrderByNicknameAsc(Long ownerId);

	Optional<Beneficiary> findByOwnerIdAndId(Long ownerId, Long id);

	boolean existsByOwnerIdAndAccountNumber(Long ownerId, String accountNumber);

	/**
	 * Deletes by owner + id in one statement instead of loading the row and calling
	 * {@code delete(entity)}.
	 *
	 * <p>Learning note: scoping the DELETE by {@code ownerId} is what stops a request for
	 * {@code /api/accounts/1/beneficiaries/7} from removing account 2's payee — the ownership
	 * check happens in SQL rather than in Java, so there is no window between read and write.
	 */
	@Modifying
	@Query("delete from Beneficiary b where b.owner.id = :ownerId and b.id = :id")
	int deleteByOwnerIdAndId(@Param("ownerId") Long ownerId, @Param("id") Long id);
}

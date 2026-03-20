package com.be_employment_contract.repository;

import com.be_employment_contract.constant.ContractStatus;
import com.be_employment_contract.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, String> {

	Optional<Contract> findByContractCodeAndStaffAccountUsernameAndStatus(String contractCode, String username, ContractStatus status);

	@Query("""
			select c
			from Contract c
			join fetch c.staff s
			join fetch c.branch b
			order by c.createAt desc
		""")
	List<Contract> findAllWithStaffAndBranch();

	@Query("""
			select c
			from Contract c
			join fetch c.staff s
			join fetch c.branch b
			where c.contractCode = :contractCode
		""")
	Optional<Contract> findDetailByContractCode(@Param("contractCode") String contractCode);
}

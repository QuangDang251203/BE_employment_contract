package com.be_employment_contract.repository;

import com.be_employment_contract.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

	Optional<Staff> findTopByOrderByIdDesc();
}

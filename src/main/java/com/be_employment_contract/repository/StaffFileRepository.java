package com.be_employment_contract.repository;

import com.be_employment_contract.entity.StaffFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffFileRepository extends JpaRepository<StaffFile, Long> {

	List<StaffFile> findByStaffIdOrderByIdAsc(Long staffId);

	Optional<StaffFile> findByIdAndStaffId(Long id, Long staffId);
}

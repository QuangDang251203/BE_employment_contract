package com.be_employment_contract.repository;

import com.be_employment_contract.entity.ContractFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractFileRepository extends JpaRepository<ContractFile, Long> {

    List<ContractFile> findByContractContractCodeOrderBySignedAtDesc(String contractCode);
}


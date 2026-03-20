package com.be_employment_contract.dto;

import com.be_employment_contract.constant.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractDTO {

	private String contractCode;
	private String decisionNumber;
	private LocalDate decisionDate;
	private String email;
	private LocalDate startDate;
	private LocalDate endDate;
	private ContractStatus status;
	private Long branchId;
	private String level;
	private String salaryRank;
	private BigDecimal percentageOfSalary;
	private BigDecimal probationarySalary;
	private LocalDateTime createAt;
	private StaffDTO staff;
}

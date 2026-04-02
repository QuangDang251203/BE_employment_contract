package com.be_employment_contract.entity;

import com.be_employment_contract.constant.ContractStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "contract")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

	@Id
	@Column(name = "contract_code", length = 20)
	private String contractCode;

	@Column(name = "decision_number", nullable = false, length = 5)
	private String decisionNumber;

	@Column(name = "decision_date", nullable = false)
	private LocalDate decisionDate;

	@Column(name = "email", nullable = false, length = 50)
	private String email;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Enumerated(EnumType.ORDINAL)
	@Column(name = "status", nullable = false)
	private ContractStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "staff_id", nullable = false)
	private Staff staff;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "branch_id", nullable = false)
	private Branch branch;

	private String jobPosition;

	@Column(name = "level", nullable = false, length = 20)
	private String level;

	@Column(name = "salary_rank", nullable = false, length = 20)
	private String salaryRank;

	@Column(name = "percentage_of_salary", nullable = false, precision = 5, scale = 2)
	private BigDecimal percentageOfSalary;

	@Column(name = "probationary_salary", nullable = false, precision = 15, scale = 2)
	private BigDecimal probationarySalary;

	@Column(name = "create_at", insertable = false, updatable = false)
	private LocalDateTime createAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "contract")
	private List<ContractFile> contractFiles;
}

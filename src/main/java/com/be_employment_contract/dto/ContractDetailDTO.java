package com.be_employment_contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractDetailDTO {

    private String decisionNumber;
    private LocalDate decisionDate;
    private String staffFullName;
    private LocalDate dateOfBirth;
    private Long citizenIdNumber;
    private LocalDate dateIssued;
    private String issuingLocation;
    private String email;
    private String address;
    private String levelOfTraining;
    private List<StaffDocumentDTO> staffDocuments;
    private LocalDate startDate;
    private Long probationDays;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private String branchName;
    private String salaryRank;
    private String level;
    private BigDecimal percentageOfSalary;
    private BigDecimal probationarySalary;
}

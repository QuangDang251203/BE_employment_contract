package com.be_employment_contract.dto;

import com.be_employment_contract.constant.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractListItemDTO {

    private String contractCode;
    private String staffFullName;
    private Long citizenIdNumber;
    private LocalDate dateOfBirth;
    private String permanentAddress;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private String branchName;
    private String jobPosition;
    private ContractStatus contractStatus;
}

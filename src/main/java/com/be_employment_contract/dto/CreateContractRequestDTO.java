package com.be_employment_contract.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateContractRequestDTO {

    @NotBlank(message = "staff fullName is required")
    private String fullName;

    @NotNull(message = "dateOfBirth is required")
    private LocalDate dateOfBirth;

    @NotBlank(message = "address is required")
    private String address;

    @NotNull(message = "dateIssued is required")
    private LocalDate dateIssued;

    @NotBlank(message = "so CCCD is required")
    @Size(max = 12, message = "so CCCD max length is 12")
    private String soCCCD;

    @NotBlank(message = "issuingLocation is required")
    private String issuingLocation;

    @NotBlank(message = "levelOfTraining is required")
    private String levelOfTraining;

    @NotNull(message = "branchId is required")
    private Long branchId;

    @NotBlank(message = "jobPosition is required")
    private String jobPosition;

    @Email(message = "email is invalid")
    @NotBlank(message = "email is required")
    private String email;

    @NotBlank(message = "decisionNumber is required")
    @Size(max = 5, message = "decisionNumber max length is 5")
    private String decisionNumber;

    @NotNull(message = "decisionDate is required")
    private LocalDate decisionDate;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    @Future(message = "endDate must be in the future")
    private LocalDate endDate;

    @NotBlank(message = "level is required")
    private String level;

    @NotBlank(message = "salaryRank is required")
    private String salaryRank;

    @NotNull(message = "percentageOfSalary is required")
    @Positive(message = "percentageOfSalary must be positive")
    private BigDecimal percentageOfSalary;

    @NotNull(message = "probationarySalary is required")
    @Positive(message = "probationarySalary must be positive")
    private BigDecimal probationarySalary;

    @Valid
    private List<CreateStaffDocumentRequestDTO> staffDocuments;
}


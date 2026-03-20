package com.be_employment_contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpIssueResponseDTO {

    private String contractCode;
    private long otpExpiresInSeconds;
}


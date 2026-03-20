package com.be_employment_contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OTPDTO {

    @NotBlank(message = "username is required")
    private String username;

    @NotBlank(message = "contractCode is required")
    private String contractCode;

    @NotBlank(message = "otpCode is required")
    private String otpCode;
}

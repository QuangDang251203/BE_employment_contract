package com.be_employment_contract.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OTP {

    private String username;
    private Long contractId;
    private String code;
    private LocalDateTime expiresAt;

}

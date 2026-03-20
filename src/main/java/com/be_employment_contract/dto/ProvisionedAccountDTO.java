package com.be_employment_contract.dto;

import com.be_employment_contract.entity.Staff;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvisionedAccountDTO {

    private Staff staff;
    private String rawPassword;
}


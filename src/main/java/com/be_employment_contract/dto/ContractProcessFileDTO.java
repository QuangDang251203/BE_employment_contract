package com.be_employment_contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractProcessFileDTO {

    private Long id;
    private String fileType;
    private LocalDateTime signAt;
}


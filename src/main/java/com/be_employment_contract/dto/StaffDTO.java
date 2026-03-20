package com.be_employment_contract.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StaffDTO {

	private Long id;
	private String fullName;
	private java.time.LocalDate dateOfBirth;
	private String address;
	private java.time.LocalDate dateIssued;
	private String issuingLocation;
	private String levelOfTraining;
	private Long branchId;
	private String email;
	private String username;
}

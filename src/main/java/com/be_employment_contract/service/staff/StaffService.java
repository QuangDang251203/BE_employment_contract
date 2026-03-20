package com.be_employment_contract.service.staff;

import com.be_employment_contract.dto.CreateContractRequestDTO;
import com.be_employment_contract.dto.ProvisionedAccountDTO;

public interface StaffService {

	ProvisionedAccountDTO createStaffWithAccount(CreateContractRequestDTO request);
}

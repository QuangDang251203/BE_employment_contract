package com.be_employment_contract.service.contract;

import com.be_employment_contract.dto.ContractDetailDTO;
import com.be_employment_contract.dto.ContractDTO;
import com.be_employment_contract.dto.ContractListItemDTO;
import com.be_employment_contract.dto.CreateContractRequestDTO;
import com.be_employment_contract.dto.LoginRequestDTO;
import com.be_employment_contract.dto.OTPDTO;
import com.be_employment_contract.dto.OtpIssueResponseDTO;

import java.util.List;

public interface ContractService {

	ContractDTO createContract(CreateContractRequestDTO request);

	OtpIssueResponseDTO loginAndIssueOtp(LoginRequestDTO request);

	ContractDTO verifyOtpAndComplete(OTPDTO otpDTO);

	List<ContractListItemDTO> getAllContracts();

	ContractDetailDTO getContractDetail(String contractCode);
}

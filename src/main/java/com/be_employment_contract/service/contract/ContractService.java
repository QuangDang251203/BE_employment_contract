package com.be_employment_contract.service.contract;

import com.be_employment_contract.dto.ContractDetailDTO;
import com.be_employment_contract.dto.ContractDTO;
import com.be_employment_contract.dto.ContractFileDTO;
import com.be_employment_contract.dto.ContractListItemDTO;
import com.be_employment_contract.dto.CreateContractRequestDTO;
import com.be_employment_contract.dto.LoginRequestDTO;
import com.be_employment_contract.dto.OTPDTO;
import com.be_employment_contract.dto.OtpIssueResponseDTO;
import com.be_employment_contract.dto.StaffDocumentDTO;
import com.be_employment_contract.dto.ContractProcessFileDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ContractService {

	ContractDTO createContract(CreateContractRequestDTO request, List<MultipartFile> attachments);

	OtpIssueResponseDTO loginAndIssueOtp(LoginRequestDTO request);

	ContractDTO verifyOtpAndComplete(OTPDTO otpDTO, MultipartFile signatureImage);

	ContractDTO stampContract(String contractCode);

	ContractFileDTO getPreferredContractFile(String contractCode);

	ContractFileDTO getPreferredContractFileForStaff(String contractCode, String username);

	StaffDocumentDTO getStaffDocumentFile(String contractCode, Long staffFileId);

	List<ContractListItemDTO> getAllContracts();

	List<ContractDTO> getAllContract();

	List<ContractDTO> getContractsByBranchId(Long branchId);

	ContractDetailDTO getContractDetail(String contractCode);

	List<ContractProcessFileDTO> getContractProcessFiles(String contractCode);
}

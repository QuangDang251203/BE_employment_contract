package com.be_employment_contract.controller;

import com.be_employment_contract.constant.ApiCode;
import com.be_employment_contract.dto.ContractDetailDTO;
import com.be_employment_contract.dto.ContractDTO;
import com.be_employment_contract.dto.ContractListItemDTO;
import com.be_employment_contract.dto.CreateContractRequestDTO;
import com.be_employment_contract.dto.LoginRequestDTO;
import com.be_employment_contract.dto.OTPDTO;
import com.be_employment_contract.dto.OtpIssueResponseDTO;
import com.be_employment_contract.response.ApiResponse;
import com.be_employment_contract.service.contract.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private static final Logger log = LoggerFactory.getLogger(ContractController.class);
    private final ContractService contractService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ContractListItemDTO>>> getAllContracts() {
        log.info("API get all contracts");
        List<ContractListItemDTO> contracts = contractService.getAllContracts();
        return ResponseEntity.ok(ApiResponse.success(ApiCode.SUCCESS, "Get all contracts successfully", contracts));
    }

    @GetMapping("/{contractCode}")
    public ResponseEntity<ApiResponse<ContractDetailDTO>> getDetailContract(@PathVariable String contractCode) {
        log.info("API get contract detail for contractCode={}", contractCode);
        ContractDetailDTO contractDetail = contractService.getContractDetail(contractCode);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.SUCCESS, "Get contract detail successfully", contractDetail));
    }

    @PostMapping("/init")
    public ResponseEntity<ApiResponse<ContractDTO>> initContract(@Valid @RequestBody CreateContractRequestDTO request) {
        log.info("API init contract for email={}", request.getEmail());
        ContractDTO contract = contractService.createContract(request);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.SUCCESS, "Contract created with PENDING_SIGN status", contract));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<OtpIssueResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("API login for username={}", request.getUsername());
        OtpIssueResponseDTO result = contractService.loginAndIssueOtp(request);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.SUCCESS, "OTP generated and sent to email", result));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<ContractDTO>> verifyOtp(@Valid @RequestBody OTPDTO otpDTO) {
        log.info("API verify OTP for username={} contractCode={}", otpDTO.getUsername(), otpDTO.getContractCode());
        ContractDTO result = contractService.verifyOtpAndComplete(otpDTO);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.SUCCESS, "Contract status moved to COMPLETED", result));
    }
}

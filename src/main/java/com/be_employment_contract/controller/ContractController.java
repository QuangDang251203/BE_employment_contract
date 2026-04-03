package com.be_employment_contract.controller;

import com.be_employment_contract.constant.ApiCode;
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
import com.be_employment_contract.exception.BusinessException;
import com.be_employment_contract.response.ApiResponse;
import com.be_employment_contract.service.contract.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000",
        allowCredentials = "true" )
public class ContractController {

    private static final Logger log = LoggerFactory.getLogger(ContractController.class);
    private final ContractService contractService;

    @GetMapping("/getAllContracts")
    public ResponseEntity<ApiResponse<List<ContractListItemDTO>>> getAllContracts() {
        log.info("API get all contracts");
        List<ContractListItemDTO> contracts = contractService.getAllContracts();
        return ResponseEntity.ok(ApiResponse.success(ApiCode.SUCCESS, "Get all contracts successfully", contracts));
    }

    @GetMapping("/getAllContract")
    public ResponseEntity<ApiResponse<List<ContractDTO>>> getAllContract() {
        log.info("API get all contracts full data");
        List<ContractDTO> contracts = contractService.getAllContract();
        return ResponseEntity.ok(ApiResponse.success(ApiCode.SUCCESS, "Get all contracts successfully", contracts));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<List<ContractDTO>>> getContractsByBranchId(@PathVariable Long branchId) {
        log.info("API get contracts by branchId={}", branchId);
        List<ContractDTO> contracts = contractService.getContractsByBranchId(branchId);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.SUCCESS, "Get contracts by branch successfully", contracts));
    }

    @GetMapping("/{contractCode}")
    public ResponseEntity<ApiResponse<ContractDetailDTO>> getDetailContract(@PathVariable String contractCode) {
        log.info("API get contract detail for contractCode={}", contractCode);
        ContractDetailDTO contractDetail = contractService.getContractDetail(contractCode);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.SUCCESS, "Get contract detail successfully", contractDetail));
    }

    @GetMapping(value = "/{contractCode}/staff-view", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> getPreferredContractFileForStaff(@PathVariable String contractCode) {
        log.info("API stream preferred contract file for contractCode={}", contractCode);
        ContractFileDTO file = contractService.getPreferredContractFile(contractCode);

        try {
            Path path = Paths.get(file.getFilePath()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                throw new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Contract file not found");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                    .body(resource);
        } catch (MalformedURLException exception) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot read contract file");
        }
    }

    @PostMapping(value = "/init", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ContractDTO>> initContract(@Valid @RequestBody CreateContractRequestDTO request) {
        log.info("API init contract for email={}", request.getEmail());
        ContractDTO contract = contractService.createContract(request, List.of());
        return ResponseEntity.ok(ApiResponse.success(ApiCode.SUCCESS, "Contract created with PENDING_SIGN status", contract));
    }

    @PostMapping(value = "/init", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ContractDTO>> initContractMultipart(
        @Valid @RequestPart("contract") CreateContractRequestDTO request,
        @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
        @RequestPart(value = "attachment", required = false) List<MultipartFile> attachment
    ) {
        List<MultipartFile> safeAttachments = mergeAttachmentParts(attachments, attachment);
        log.info("API init contract multipart for email={}, attachments={}", request.getEmail(), safeAttachments.size());
        ContractDTO contract = contractService.createContract(request, safeAttachments);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.SUCCESS, "Contract created with PENDING_SIGN status", contract));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<OtpIssueResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("API login for username={}", request.getUsername());
        OtpIssueResponseDTO result = contractService.loginAndIssueOtp(request);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.SUCCESS, "OTP generated and sent to email", result));
    }

    @PostMapping(value = "/verify-otp-sign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ContractDTO>> verifyOtpAndSign(
            @Valid @RequestPart("otp") OTPDTO otpDTO,
            @RequestPart("signature") MultipartFile signatureImage
    ) {
        log.info("API verify OTP + sign for username={} contractCode={}", otpDTO.getUsername(), otpDTO.getContractCode());
        ContractDTO result = contractService.verifyOtpAndComplete(otpDTO, signatureImage);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.SUCCESS, "Contract signed and completed successfully", result));
    }

    @PostMapping("/stamp/{contractCode}")
    public ResponseEntity<ApiResponse<ContractDTO>> stampContract(@PathVariable String contractCode) {
        log.info("API stamp contract contractCode={}", contractCode);
        ContractDTO result = contractService.stampContract(contractCode);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.SUCCESS, "Contract stamped successfully", result));
    }

    @GetMapping(value = "/{contractCode}/staff-documents/{staffFileId}/view", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> viewStaffDocument(
            @PathVariable String contractCode,
            @PathVariable Long staffFileId
    ) {
        log.info("API stream staff document for contractCode={} staffFileId={}", contractCode, staffFileId);
        StaffDocumentDTO document = contractService.getStaffDocumentFile(contractCode, staffFileId);

        if (!isPdfDocument(document)) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "Selected document is not a PDF file");
        }

        try {
            Resource resource = buildResource(document.getFilePath());
            if (!resource.exists()) {
                throw new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Staff document file not found");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getFileName() + "\"")
                    .body(resource);
        } catch (MalformedURLException exception) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot read staff document file");
        }
    }

    @GetMapping("/{contractCode}/process-files")
    public ResponseEntity<ApiResponse<List<ContractProcessFileDTO>>> getContractProcessFiles(@PathVariable String contractCode) {
        log.info("API get contract process files for contractCode={}", contractCode);
        List<ContractProcessFileDTO> files = contractService.getContractProcessFiles(contractCode);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.SUCCESS, "Get contract process files successfully", files));
    }

    private Resource buildResource(String filePath) throws MalformedURLException {
        if (filePath.startsWith("http://") || filePath.startsWith("https://") || filePath.startsWith("file:/")) {
            return new UrlResource(filePath);
        }

        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        return new UrlResource(path.toUri());
    }

    private boolean isPdfDocument(StaffDocumentDTO document) {
        if (document.getFileType() != null && "pdf".equalsIgnoreCase(document.getFileType())) {
            return true;
        }

        String fileName = document.getFileName() == null ? "" : document.getFileName().toLowerCase();
        String filePath = document.getFilePath() == null ? "" : document.getFilePath().toLowerCase();
        return fileName.endsWith(".pdf") || filePath.endsWith(".pdf");
    }

    private List<MultipartFile> mergeAttachmentParts(List<MultipartFile> attachments, List<MultipartFile> attachment) {
        if ((attachments == null || attachments.isEmpty()) && (attachment == null || attachment.isEmpty())) {
            return List.of();
        }

        List<MultipartFile> merged = new java.util.ArrayList<>();
        if (attachments != null && !attachments.isEmpty()) {
            merged.addAll(attachments);
        }
        if (attachment != null && !attachment.isEmpty()) {
            merged.addAll(attachment);
        }
        return merged;
    }
}

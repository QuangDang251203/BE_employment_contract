package com.be_employment_contract.service.contract;

import com.be_employment_contract.constant.ApiCode;
import com.be_employment_contract.constant.AppConstants;
import com.be_employment_contract.constant.ContractStatus;
import com.be_employment_contract.dto.ContractDetailDTO;
import com.be_employment_contract.dto.ContractDTO;
import com.be_employment_contract.dto.ContractFileDTO;
import com.be_employment_contract.dto.ContractListItemDTO;
import com.be_employment_contract.dto.CreateContractRequestDTO;
import com.be_employment_contract.dto.CreateStaffDocumentRequestDTO;
import com.be_employment_contract.dto.LoginRequestDTO;
import com.be_employment_contract.dto.OTPDTO;
import com.be_employment_contract.dto.OtpIssueResponseDTO;
import com.be_employment_contract.dto.ProvisionedAccountDTO;
import com.be_employment_contract.dto.StaffDocumentDTO;
import com.be_employment_contract.entity.Account;
import com.be_employment_contract.entity.Contract;
import com.be_employment_contract.entity.ContractFile;
import com.be_employment_contract.entity.StaffFile;
import com.be_employment_contract.exception.BusinessException;
import com.be_employment_contract.mapper.ContractMapper;
import com.be_employment_contract.mapper.StaffMapper;
import com.be_employment_contract.repository.AccountRepository;
import com.be_employment_contract.repository.BranchRepository;
import com.be_employment_contract.repository.ContractFileRepository;
import com.be_employment_contract.repository.ContractRepository;
import com.be_employment_contract.repository.StaffFileRepository;
import com.be_employment_contract.service.staff.StaffService;
import com.be_employment_contract.utils.MailTemplateUtils;
import com.be_employment_contract.utils.OtpUtils;
import com.be_employment_contract.utils.PasswordUtils;
import com.be_employment_contract.utils.FileStorageUtils;
import com.be_employment_contract.utils.ContractDocumentUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractServiceImpl.class);

    private final ContractRepository contractRepository;
    private final BranchRepository branchRepository;
    private final ContractFileRepository contractFileRepository;
    private final StaffService staffService;
    private final AccountRepository accountRepository;
    private final StaffFileRepository staffFileRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender mailSender;


    @Override
    @Transactional
    public ContractDTO createContract(CreateContractRequestDTO request, List<MultipartFile> attachments) {

        validateContractDates(request);
        log.info("Create contract flow started with data : {}", request);

        List<CreateStaffDocumentRequestDTO> mergedDocuments = mergeDocuments(request.getStaffDocuments(), attachments);
        if (mergedDocuments.isEmpty()) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                "At least one staff document is required");
        }

        ProvisionedAccountDTO provisioned = staffService.createStaffWithAccount(request);

        List<StaffFile> staffFiles = StaffMapper.toStaffFiles(mergedDocuments, provisioned.getStaff());
        if (!staffFiles.isEmpty()) {
            staffFileRepository.saveAll(staffFiles);
        }

        String contractCode = generateUniqueContractCode();
        Contract contract = ContractMapper.toEntity(request, provisioned.getStaff(), provisioned.getStaff().getBranch(), contractCode);
        Contract savedContract = contractRepository.save(contract);

        ContractDocumentUtils.GeneratedContractFile generatedContractFile = createAndSaveGeneratedContractFile(request, savedContract);

        queueCredentialMailAfterCommit(
                request.getEmail(),
                provisioned.getStaff().getFullName(),
                provisioned.getStaff().getAccount().getUsername(),
                provisioned.getRawPassword(),
                contractCode
        );

        log.info("Created contract code={} with status={} for staffId={}",
                savedContract.getContractCode(),
                savedContract.getStatus(),
                provisioned.getStaff().getId());
        log.info("Saved {} staff documents for staffId={}",
                staffFiles.size(),
                provisioned.getStaff().getId());
        log.info("Generated contract PDF name={} path={}",
                generatedContractFile.fileName(), generatedContractFile.filePath());

        return ContractMapper.toDto(savedContract);
    }

    private List<CreateStaffDocumentRequestDTO> mergeDocuments(
        List<CreateStaffDocumentRequestDTO> payloadDocuments,
        List<MultipartFile> attachments
    ) {
        List<CreateStaffDocumentRequestDTO> merged = new ArrayList<>();
        if (payloadDocuments != null && !payloadDocuments.isEmpty()) {
            try {
                merged.addAll(FileStorageUtils.normalizePayloadDocumentsToLocal(
                        payloadDocuments,
                        AppConstants.STAFF_DOCUMENT_STORAGE_DIR
                ));
            } catch (IOException exception) {
                log.error("Failed to normalize payload document paths to local storage", exception);
                throw new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot store payload staff documents");
            }
        }

        List<MultipartFile> safeAttachments = attachments == null ? Collections.emptyList() : attachments;
        if (!safeAttachments.isEmpty()) {
            try {
                merged.addAll(FileStorageUtils.storeStaffAttachments(safeAttachments, AppConstants.STAFF_DOCUMENT_STORAGE_DIR));
            } catch (IOException exception) {
                log.error("Failed to store staff attachments", exception);
                throw new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot store staff attachments");
            }
        }

        return merged;
    }

    @Override
    @Transactional(readOnly = true)
    public OtpIssueResponseDTO loginAndIssueOtp(LoginRequestDTO request) {
        log.info("Login and issue OTP for username={} contractCode={}", request.getUsername(), request.getContractCode());

        Account account = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ApiCode.AUTH_FAILED, HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!PasswordUtils.matches(request.getPassword(), account.getPasswordHash())) {
            throw new BusinessException(ApiCode.AUTH_FAILED, HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        // Login is for viewing too, so allow all statuses as long as contract belongs to this account.
        Contract contract = contractRepository
                .findByContractCodeAndStaffAccountUsername(request.getContractCode(), request.getUsername())
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Contract not found for this account"));

        String otpCode = OtpUtils.generateNumericOtp(AppConstants.OTP_LENGTH);
        String redisKey = AppConstants.REDIS_OTP_PREFIX + ":" + request.getUsername() + ":" + request.getContractCode();
        redisTemplate.opsForValue().set(redisKey, otpCode, Duration.ofMinutes(AppConstants.OTP_TTL_MINUTES));

        try {
            sendMail(
                    account.getEmail(),
                    AppConstants.EMAIL_SUBJECT_OTP,
                    MailTemplateUtils.otpMailBody(contract.getStaff().getFullName(), otpCode, AppConstants.OTP_TTL_MINUTES)
            );
        } catch (RuntimeException exception) {
            redisTemplate.delete(redisKey);
            throw exception;
        }

        OtpIssueResponseDTO responseDTO = new OtpIssueResponseDTO();
        responseDTO.setContractCode(request.getContractCode());
        responseDTO.setOtpExpiresInSeconds(Duration.ofMinutes(AppConstants.OTP_TTL_MINUTES).toSeconds());

        log.info("Issued OTP for username={} contractCode={} contractStatus={}",
                request.getUsername(), request.getContractCode(), contract.getStatus());
        return responseDTO;
    }

    @Override
    @Transactional
    public ContractDTO verifyOtpAndComplete(OTPDTO otpDTO) {
        String redisKey = AppConstants.REDIS_OTP_PREFIX + ":" + otpDTO.getUsername() + ":" + otpDTO.getContractCode();
        String redisOtp = redisTemplate.opsForValue().get(redisKey);

        if (redisOtp == null || !redisOtp.equals(otpDTO.getOtpCode())) {
            throw new BusinessException(ApiCode.OTP_INVALID, HttpStatus.BAD_REQUEST, "OTP is invalid or expired");
        }

        Contract contract = contractRepository
                .findByContractCodeAndStaffAccountUsernameAndStatus(otpDTO.getContractCode(), otpDTO.getUsername(), ContractStatus.PENDING_SIGN)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Pending contract not found"));

        contract.setStatus(ContractStatus.COMPLETED);
        Contract updated = contractRepository.save(contract);
        redisTemplate.delete(redisKey);

        log.info("Contract code={} marked COMPLETED by username={}", updated.getContractCode(), otpDTO.getUsername());
        return ContractMapper.toDto(updated);
    }

    @Override
    @Transactional
    public ContractDTO verifyOtpAndCompleteWithSignature(OTPDTO otpDTO, MultipartFile signatureImage) {
        String redisKey = AppConstants.REDIS_OTP_PREFIX + ":" + otpDTO.getUsername() + ":" + otpDTO.getContractCode();
        String redisOtp = redisTemplate.opsForValue().get(redisKey);

        if (redisOtp == null || !redisOtp.equals(otpDTO.getOtpCode())) {
            throw new BusinessException(ApiCode.OTP_INVALID, HttpStatus.BAD_REQUEST, "OTP is invalid or expired");
        }

        Contract contract = contractRepository
                .findByContractCodeAndStaffAccountUsernameAndStatus(otpDTO.getContractCode(), otpDTO.getUsername(), ContractStatus.PENDING_SIGN)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Pending contract not found"));

        ContractFile latestGeneratedPdf = contractFileRepository
                .findByContractContractCodeOrderBySignedAtDesc(contract.getContractCode())
                .stream()
                .filter(file -> AppConstants.CONTRACT_FILE_TYPE_GENERATED_PDF.equals(file.getFileType()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND,
                        "Generated contract PDF not found. Please recreate contract"));

        ContractDocumentUtils.GeneratedContractFile signedFile;
        try {
            signedFile = ContractDocumentUtils.createSignedPdf(
                    Paths.get(latestGeneratedPdf.getFilePath()),
                    signatureImage,
                    contract.getContractCode()
            );
        } catch (IOException exception) {
            log.error("Failed to create signed contract PDF for contractCode={}", contract.getContractCode(), exception);
            throw new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot create signed contract file");
        }

        ContractFile signedContractFile = new ContractFile();
        signedContractFile.setContract(contract);
        signedContractFile.setFileName(signedFile.fileName());
        signedContractFile.setFilePath(signedFile.filePath());
        signedContractFile.setFileType(AppConstants.CONTRACT_FILE_TYPE_SIGNED_PDF);
        signedContractFile.setSignedAt(LocalDateTime.now());
        contractFileRepository.save(signedContractFile);

        contract.setStatus(ContractStatus.SIGNED);
        Contract updated = contractRepository.save(contract);
        redisTemplate.delete(redisKey);

        log.info("Contract code={} marked SIGNED with signed file={}", updated.getContractCode(), signedFile.fileName());
        return ContractMapper.toDto(updated);
    }

    @Override
    @Transactional
    public ContractDTO stampContract(String contractCode, MultipartFile stampImage) {
        log.info("Stamp contract flow started for contractCode={}", contractCode);

        Contract contract = contractRepository.findById(contractCode)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Contract not found"));

        List<ContractFile> contractFiles = contractFileRepository.findByContractContractCodeOrderBySignedAtDesc(contractCode);
        ContractFile latestSigned = findLatestByType(contractFiles, AppConstants.CONTRACT_FILE_TYPE_SIGNED_PDF)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND,
                        "Signed contract file not found. Employee must sign first"));

        ContractDocumentUtils.GeneratedContractFile stampedFile;
        try {
            stampedFile = ContractDocumentUtils.createStampedPdf(
                    Paths.get(latestSigned.getFilePath()),
                    stampImage,
                    contractCode
            );
        } catch (IOException exception) {
            log.error("Failed to create stamped contract PDF for contractCode={}", contractCode, exception);
            throw new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot create stamped contract file");
        }

        ContractFile stampedContractFile = new ContractFile();
        stampedContractFile.setContract(contract);
        stampedContractFile.setFileName(stampedFile.fileName());
        stampedContractFile.setFilePath(stampedFile.filePath());
        stampedContractFile.setFileType(AppConstants.CONTRACT_FILE_TYPE_STAMPED_PDF);
        stampedContractFile.setSignedAt(LocalDateTime.now());
        contractFileRepository.save(stampedContractFile);

        contract.setStatus(ContractStatus.STAMPED);
        Contract updated = contractRepository.save(contract);
        log.info("Contract code={} marked STAMPED with file={}", updated.getContractCode(), stampedFile.fileName());
        return ContractMapper.toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractFileDTO getPreferredContractFileForStaff(String contractCode, String username) {
        log.info("Get preferred contract file for staff username={} contractCode={}", username, contractCode);

        contractRepository.findByContractCodeAndStaffAccountUsername(contractCode, username)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND,
                        "Contract not found for this account"));

        List<ContractFile> contractFiles = contractFileRepository.findByContractContractCodeOrderBySignedAtDesc(contractCode);
        ContractFile preferred = findPreferredFile(contractFiles)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND,
                        "No contract file found"));

        return new ContractFileDTO(
                preferred.getId(),
                preferred.getFileName(),
                preferred.getFilePath(),
                preferred.getFileType(),
                preferred.getSignedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ContractFileDTO getPreferredContractFile(String contractCode) {
        log.info("Get preferred contract file by contractCode={}", contractCode);

        if (!contractRepository.existsById(contractCode)) {
            throw new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Contract not found");
        }

        List<ContractFile> contractFiles = contractFileRepository.findByContractContractCodeOrderBySignedAtDesc(contractCode);
        ContractFile preferred = findPreferredFile(contractFiles)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND,
                        "No contract file found"));

        return new ContractFileDTO(
                preferred.getId(),
                preferred.getFileName(),
                preferred.getFilePath(),
                preferred.getFileType(),
                preferred.getSignedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractListItemDTO> getAllContracts() {
        log.info("Get all contracts flow started");
        List<ContractListItemDTO> contracts = contractRepository.findAllWithStaffAndBranch()
                .stream()
                .map(ContractMapper::toListItemDto)
                .toList();
        log.info("Get all contracts flow completed with {} records", contracts.size());
        return contracts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractDTO> getAllContract() {
        log.info("Get all contract full data flow started");
        List<ContractDTO> contracts = contractRepository.findAllWithStaffAndBranch()
                .stream()
                .map(ContractMapper::toDto)
                .toList();
        log.info("Get all contract full data flow completed with {} records", contracts.size());
        return contracts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractDTO> getContractsByBranchId(Long branchId) {
        log.info("Get contracts by branchId={} flow started", branchId);

        if (branchId == null) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "branchId is required");
        }

        if (!branchRepository.existsById(branchId)) {
            throw new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Branch not found");
        }

        List<ContractDTO> contracts = contractRepository.findAllByBranchIdWithStaffAndBranch(branchId)
                .stream()
                .map(ContractMapper::toDto)
                .toList();

        log.info("Get contracts by branchId={} flow completed with {} records", branchId, contracts.size());
        return contracts;
    }

    @Override
    @Transactional(readOnly = true)
    public ContractDetailDTO getContractDetail(String contractCode) {
        log.info("Get contract detail flow started for contractCode={}", contractCode);
        Contract contract = contractRepository.findDetailByContractCode(contractCode)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Contract not found"));

        List<StaffFile> staffFiles = staffFileRepository.findByStaffIdOrderByIdAsc(contract.getStaff().getId());
        List<ContractFile> contractFiles = contractFileRepository.findByContractContractCodeOrderBySignedAtDesc(contractCode);
        ContractDetailDTO detailDTO = ContractMapper.toDetailDto(contract, staffFiles, contractFiles);
        log.info("Get contract detail flow completed for contractCode={} with {} documents",
                contractCode, staffFiles.size());
        return detailDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public StaffDocumentDTO getStaffDocumentFile(String contractCode, Long staffFileId) {
        log.info("Get staff document file for contractCode={} staffFileId={}", contractCode, staffFileId);

        Contract contract = contractRepository.findById(contractCode)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Contract not found"));

        StaffFile staffFile = staffFileRepository.findByIdAndStaffId(staffFileId, contract.getStaff().getId())
                .orElse(null);
        if (staffFile == null) {
            staffFileRepository.findById(staffFileId).ifPresent(file ->
                    log.warn("Staff document id={} exists but belongs to staffId={}, while contractCode={} belongs to staffId={}",
                            staffFileId,
                            file.getStaff() == null ? null : file.getStaff().getId(),
                            contractCode,
                            contract.getStaff().getId())
            );
            throw new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND,
                    "Staff document not found for this contract");
        }

        return new StaffDocumentDTO(
                staffFile.getId(),
                staffFile.getFileName(),
                staffFile.getFilePath(),
                staffFile.getFileType()
        );
    }

    private void validateContractDates(CreateContractRequestDTO request) {
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "endDate must be after startDate");
        }
    }

    private String generateUniqueContractCode() {
        String candidate = "CT" + System.currentTimeMillis();
        while (contractRepository.existsById(candidate)) {
            candidate = "CT" + System.currentTimeMillis();
        }
        if (candidate.length() > 20) {
            candidate = candidate.substring(0, 20);
        }
        return candidate;
    }

    private ContractDocumentUtils.GeneratedContractFile createAndSaveGeneratedContractFile(
            CreateContractRequestDTO request,
            Contract contract
    ) {
        ContractDocumentUtils.GeneratedContractFile generatedFile;
        try {
            generatedFile = ContractDocumentUtils.generatePdfFromTemplate(request, contract);
        } catch (IOException exception) {
            log.error("Failed to generate contract PDF for contractCode={}", contract.getContractCode(), exception);
            throw new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot generate contract PDF from template");
        }

        ContractFile contractFile = new ContractFile();
        contractFile.setContract(contract);
        contractFile.setFileName(generatedFile.fileName());
        contractFile.setFilePath(generatedFile.filePath());
        contractFile.setFileType(AppConstants.CONTRACT_FILE_TYPE_GENERATED_PDF);
        contractFile.setSignedAt(LocalDateTime.now());
        contractFileRepository.save(contractFile);

        return generatedFile;
    }

    private Optional<ContractFile> findPreferredFile(List<ContractFile> contractFiles) {
        Optional<ContractFile> stamped = findLatestByType(contractFiles, AppConstants.CONTRACT_FILE_TYPE_STAMPED_PDF);
        if (stamped.isPresent()) {
            return stamped;
        }

        Optional<ContractFile> signed = findLatestByType(contractFiles, AppConstants.CONTRACT_FILE_TYPE_SIGNED_PDF);
        if (signed.isPresent()) {
            return signed;
        }

        return findLatestByType(contractFiles, AppConstants.CONTRACT_FILE_TYPE_GENERATED_PDF);
    }

    private Optional<ContractFile> findLatestByType(List<ContractFile> contractFiles, String fileType) {
        if (contractFiles == null || contractFiles.isEmpty()) {
            return Optional.empty();
        }

        return contractFiles.stream()
                .filter(file -> fileType.equals(file.getFileType()))
                .findFirst();
    }

    private void queueCredentialMailAfterCommit(String toEmail, String fullName, String username, String rawPassword, String ContractCode) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendMail(
                    toEmail,
                    AppConstants.EMAIL_SUBJECT_CREDENTIAL,
                    MailTemplateUtils.credentialMailBody(fullName, username, rawPassword, ContractCode)
            );
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    sendMail(
                            toEmail,
                            AppConstants.EMAIL_SUBJECT_CREDENTIAL,
                            MailTemplateUtils.credentialMailBody(fullName, username, rawPassword, ContractCode)
                    );
                    log.info("Credential email sent to {}", toEmail);
                } catch (RuntimeException exception) {
                    log.error("Failed to send credential email to {} after commit", toEmail, exception);
                }
            }
        });
    }

    private void sendMail(String toEmail, String subject, String content) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, false);
            mailSender.send(mimeMessage);
        } catch (MessagingException exception) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Cannot prepare email content");
        } catch (Exception exception) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Cannot send email");
        }
    }
}

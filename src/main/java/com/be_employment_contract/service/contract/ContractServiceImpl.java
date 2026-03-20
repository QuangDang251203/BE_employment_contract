package com.be_employment_contract.service.contract;

import com.be_employment_contract.constant.ApiCode;
import com.be_employment_contract.constant.AppConstants;
import com.be_employment_contract.constant.ContractStatus;
import com.be_employment_contract.dto.ContractDetailDTO;
import com.be_employment_contract.dto.ContractDTO;
import com.be_employment_contract.dto.ContractListItemDTO;
import com.be_employment_contract.dto.CreateContractRequestDTO;
import com.be_employment_contract.dto.LoginRequestDTO;
import com.be_employment_contract.dto.OTPDTO;
import com.be_employment_contract.dto.OtpIssueResponseDTO;
import com.be_employment_contract.dto.ProvisionedAccountDTO;
import com.be_employment_contract.entity.Account;
import com.be_employment_contract.entity.Contract;
import com.be_employment_contract.entity.StaffFile;
import com.be_employment_contract.exception.BusinessException;
import com.be_employment_contract.mapper.ContractMapper;
import com.be_employment_contract.repository.AccountRepository;
import com.be_employment_contract.repository.ContractRepository;
import com.be_employment_contract.repository.StaffFileRepository;
import com.be_employment_contract.service.staff.StaffService;
import com.be_employment_contract.utils.MailTemplateUtils;
import com.be_employment_contract.utils.OtpUtils;
import com.be_employment_contract.utils.PasswordUtils;
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

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractServiceImpl.class);

    private final ContractRepository contractRepository;
    private final StaffService staffService;
    private final AccountRepository accountRepository;
    private final StaffFileRepository staffFileRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender mailSender;


    @Override
    @Transactional
    public ContractDTO createContract(CreateContractRequestDTO request) {
        validateContractDates(request);
        log.info("Create contract flow started for email={}", request.getEmail());

        ProvisionedAccountDTO provisioned = staffService.createStaffWithAccount(request);
        String contractCode = generateUniqueContractCode();
        Contract contract = ContractMapper.toEntity(request, provisioned.getStaff(), provisioned.getStaff().getBranch(), contractCode);
        Contract savedContract = contractRepository.save(contract);

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

        return ContractMapper.toDto(savedContract);
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

        Contract contract = contractRepository
                .findByContractCodeAndStaffAccountUsernameAndStatus(request.getContractCode(), request.getUsername(), ContractStatus.PENDING_SIGN)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Pending contract not found"));

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

        log.info("Issued OTP for username={} contractCode={}", request.getUsername(), request.getContractCode());
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
    public ContractDetailDTO getContractDetail(String contractCode) {
        log.info("Get contract detail flow started for contractCode={}", contractCode);
        Contract contract = contractRepository.findDetailByContractCode(contractCode)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Contract not found"));

        List<StaffFile> staffFiles = staffFileRepository.findByStaffIdOrderByIdAsc(contract.getStaff().getId());
        ContractDetailDTO detailDTO = ContractMapper.toDetailDto(contract, staffFiles);
        log.info("Get contract detail flow completed for contractCode={} with {} documents",
                contractCode, staffFiles.size());
        return detailDTO;
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

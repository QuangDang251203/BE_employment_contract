package com.be_employment_contract.service.staff;

import com.be_employment_contract.constant.ApiCode;
import com.be_employment_contract.constant.AppConstants;
import com.be_employment_contract.dto.CreateContractRequestDTO;
import com.be_employment_contract.dto.ProvisionedAccountDTO;
import com.be_employment_contract.entity.Account;
import com.be_employment_contract.entity.Branch;
import com.be_employment_contract.entity.Staff;
import com.be_employment_contract.exception.BusinessException;
import com.be_employment_contract.mapper.StaffMapper;
import com.be_employment_contract.repository.AccountRepository;
import com.be_employment_contract.repository.BranchRepository;
import com.be_employment_contract.repository.StaffRepository;
import com.be_employment_contract.utils.CredentialUtils;
import com.be_employment_contract.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

	private static final Logger log = LoggerFactory.getLogger(StaffServiceImpl.class);

	private final StaffRepository staffRepository;
	private final AccountRepository accountRepository;
	private final BranchRepository branchRepository;

	@Override
	public ProvisionedAccountDTO createStaffWithAccount(CreateContractRequestDTO request) {
		if (accountRepository.existsByEmail(request.getEmail())) {
			throw new BusinessException(ApiCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, "Email already exists");
		}

		Branch branch = branchRepository.findById(request.getBranchId())
			.orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Branch not found"));

		String usernameSeed = CredentialUtils.sanitizeUsernameSeed(request.getEmail());
		String username = ensureUniqueUsername(usernameSeed);
		String rawPassword = CredentialUtils.generatePassword(AppConstants.INITIAL_PASSWORD_LENGTH);

		Staff staff = StaffMapper.toEntity(request);
		staff.setBranch(branch);
		staff.setId(generateNextStaffId());
		Staff saved = staffRepository.save(staff);

		Account account = new Account();
		account.setUsername(username);
		account.setPasswordHash(PasswordUtils.hash(rawPassword));
		account.setEmail(request.getEmail());
		account.setStatus(0);
		account.setStaff(saved);
		Account savedAccount = accountRepository.save(account);
		saved.setAccount(savedAccount);

		log.info("Created staff id={} with username={}", saved.getId(), username);

		ProvisionedAccountDTO provisioned = new ProvisionedAccountDTO();
		provisioned.setStaff(saved);
		provisioned.setRawPassword(rawPassword);
		return provisioned;
	}

	private Long generateNextStaffId() {
		return staffRepository.findTopByOrderByIdDesc()
			.map(staff -> staff.getId() + 1)
			.orElse(1L);
	}

	private String ensureUniqueUsername(String seed) {
		String base = seed == null || seed.isBlank() ? "user" : seed;
		String candidate = base;
		int counter = 1;
		while (accountRepository.existsByUsername(candidate)) {
			candidate = base + counter;
			counter++;
		}
		return candidate;
	}
}

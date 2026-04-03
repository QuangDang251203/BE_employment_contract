package com.be_employment_contract.mapper;

import com.be_employment_contract.constant.ContractStatus;
import com.be_employment_contract.dto.ContractDetailDTO;
import com.be_employment_contract.dto.ContractDTO;
import com.be_employment_contract.dto.ContractFileDTO;
import com.be_employment_contract.dto.ContractListItemDTO;
import com.be_employment_contract.dto.CreateContractRequestDTO;
import com.be_employment_contract.dto.StaffDocumentDTO;
import com.be_employment_contract.entity.Branch;
import com.be_employment_contract.entity.Contract;
import com.be_employment_contract.entity.ContractFile;
import com.be_employment_contract.entity.Staff;
import com.be_employment_contract.entity.StaffFile;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

public final class ContractMapper {

    private ContractMapper() {
    }

    public static Contract toEntity(CreateContractRequestDTO request, Staff staff, Branch branch, String contractCode) {
        Contract contract = new Contract();
        contract.setContractCode(contractCode);
        contract.setDecisionNumber(request.getDecisionNumber());
        contract.setDecisionDate(request.getDecisionDate());
        contract.setEmail(request.getEmail());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setStatus(ContractStatus.PENDING_SIGN);
        contract.setStaff(staff);
        contract.setBranch(branch);
        contract.setJobPosition(request.getJobPosition());
        contract.setLevel(request.getLevel());
        contract.setSalaryRank(request.getSalaryRank());
        contract.setPercentageOfSalary(request.getPercentageOfSalary());
        contract.setProbationarySalary(request.getProbationarySalary());
        return contract;
    }

    public static ContractDTO toDto(Contract contract) {
        ContractDTO dto = new ContractDTO();
        dto.setContractCode(contract.getContractCode());
        dto.setDecisionNumber(contract.getDecisionNumber());
        dto.setDecisionDate(contract.getDecisionDate());
        dto.setEmail(contract.getEmail());
        dto.setStartDate(contract.getStartDate());
        dto.setEndDate(contract.getEndDate());
        dto.setStatus(contract.getStatus());
        if (contract.getBranch() != null) {
            dto.setBranchId(contract.getBranch().getId());
            dto.setBranchName(contract.getBranch().getBranchName());
        }
        dto.setJobPosition(contract.getJobPosition());
        dto.setLevel(contract.getLevel());
        dto.setSalaryRank(contract.getSalaryRank());
        dto.setPercentageOfSalary(contract.getPercentageOfSalary());
        dto.setProbationarySalary(contract.getProbationarySalary());
        dto.setCreateAt(contract.getCreateAt());
        dto.setStaff(StaffMapper.toDto(contract.getStaff()));
        return dto;
    }

    public static ContractListItemDTO toListItemDto(Contract contract) {
        ContractListItemDTO dto = new ContractListItemDTO();
        dto.setContractCode(contract.getContractCode());
        dto.setStaffFullName(contract.getStaff().getFullName());
        dto.setCitizenIdNumber(contract.getStaff().getId());
        dto.setDateOfBirth(contract.getStaff().getDateOfBirth());
        dto.setPermanentAddress(contract.getStaff().getAddress());
        dto.setStartDate(contract.getStartDate());
        dto.setEndDate(contract.getEndDate());
        dto.setCreatedAt(contract.getCreateAt());
        dto.setBranchName(contract.getBranch().getBranchName());
        dto.setJobPosition(contract.getJobPosition());
        dto.setContractStatus(contract.getStatus());
        return dto;
    }

    public static ContractDetailDTO toDetailDto(Contract contract, List<StaffFile> staffFiles, List<ContractFile> contractFiles) {
        ContractDetailDTO dto = new ContractDetailDTO();
        dto.setDecisionNumber(contract.getDecisionNumber());
        dto.setDecisionDate(contract.getDecisionDate());
        dto.setStaffFullName(contract.getStaff().getFullName());
        dto.setDateOfBirth(contract.getStaff().getDateOfBirth());
        dto.setSoCCCD(contract.getStaff().getSoCCCD());
        dto.setDateIssued(contract.getStaff().getDateIssued());
        dto.setIssuingLocation(contract.getStaff().getIssuingLocation());
        dto.setEmail(contract.getEmail());
        dto.setAddress(contract.getStaff().getAddress());
        dto.setLevelOfTraining(contract.getStaff().getLevelOfTraining());
        dto.setStaffDocuments(toStaffDocumentDtoList(staffFiles));
        dto.setStartDate(contract.getStartDate());
        dto.setProbationDays(ChronoUnit.DAYS.between(contract.getStartDate(), contract.getEndDate()));
        dto.setEndDate(contract.getEndDate());
        dto.setCreatedAt(contract.getCreateAt());
        dto.setBranchName(contract.getBranch().getBranchName());
        dto.setJobPosition(contract.getJobPosition());
        dto.setSalaryRank(contract.getSalaryRank());
        dto.setLevel(contract.getLevel());
        dto.setPercentageOfSalary(contract.getPercentageOfSalary());
        dto.setProbationarySalary(contract.getProbationarySalary());
        dto.setStatus(contract.getStatus());
        dto.setContractFiles(toContractFileDtoList(contractFiles));
        return dto;
    }

    private static List<StaffDocumentDTO> toStaffDocumentDtoList(List<StaffFile> staffFiles) {
        if (staffFiles == null || staffFiles.isEmpty()) {
            return Collections.emptyList();
        }
        return staffFiles.stream()
                .map(file -> new StaffDocumentDTO(
                        file.getId(),
                        file.getFileName(),
                        file.getFilePath(),
                        file.getFileType()
                ))
                .toList();
    }

    private static List<ContractFileDTO> toContractFileDtoList(List<ContractFile> contractFiles) {
        if (contractFiles == null || contractFiles.isEmpty()) {
            return Collections.emptyList();
        }
        return contractFiles.stream()
                .map(file -> new ContractFileDTO(
                        file.getId(),
                        file.getFileName(),
                        file.getFilePath(),
                        file.getFileType(),
                        file.getSignedAt()
                ))
                .toList();
    }
}

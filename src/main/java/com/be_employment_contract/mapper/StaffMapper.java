package com.be_employment_contract.mapper;

import com.be_employment_contract.dto.CreateContractRequestDTO;
import com.be_employment_contract.dto.StaffDTO;
import com.be_employment_contract.entity.Staff;

public final class StaffMapper {

    private StaffMapper() {
    }

    public static Staff toEntity(CreateContractRequestDTO request) {
        Staff staff = new Staff();
        staff.setFullName(request.getFullName());
        staff.setDateOfBirth(request.getDateOfBirth());
        staff.setAddress(request.getAddress());
        staff.setDateIssued(request.getDateIssued());
        staff.setIssuingLocation(request.getIssuingLocation());
        staff.setLevelOfTraining(request.getLevelOfTraining());
        return staff;
    }

    public static StaffDTO toDto(Staff staff) {
        StaffDTO dto = new StaffDTO();
        dto.setId(staff.getId());
        dto.setFullName(staff.getFullName());
        dto.setDateOfBirth(staff.getDateOfBirth());
        dto.setAddress(staff.getAddress());
        dto.setDateIssued(staff.getDateIssued());
        dto.setIssuingLocation(staff.getIssuingLocation());
        dto.setLevelOfTraining(staff.getLevelOfTraining());
        if (staff.getBranch() != null) {
            dto.setBranchId(staff.getBranch().getId());
        }
        if (staff.getAccount() != null) {
            dto.setUsername(staff.getAccount().getUsername());
            dto.setEmail(staff.getAccount().getEmail());
        }
        return dto;
    }
}


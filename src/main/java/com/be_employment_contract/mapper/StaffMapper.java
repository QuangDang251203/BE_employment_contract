package com.be_employment_contract.mapper;

import com.be_employment_contract.dto.CreateContractRequestDTO;
import com.be_employment_contract.dto.CreateStaffDocumentRequestDTO;
import com.be_employment_contract.dto.StaffDTO;
import com.be_employment_contract.entity.Staff;
import com.be_employment_contract.entity.StaffFile;

import java.util.Collections;
import java.util.List;

public final class StaffMapper {

    private StaffMapper() {
    }

    public static Staff toEntity(CreateContractRequestDTO request) {
        Staff staff = new Staff();
        staff.setFullName(request.getFullName());
        staff.setSoCCCD(request.getSoCCCD());
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
        dto.setSoCCCD(staff.getSoCCCD());
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

    public static List<StaffFile> toStaffFiles(List<CreateStaffDocumentRequestDTO> documents, Staff staff) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }

        return documents.stream()
                .map(document -> {
                    StaffFile staffFile = new StaffFile();
                    staffFile.setFileName(safeValue(document.getFileName()));
                    staffFile.setFilePath(safeValue(document.getFilePath()));
                    staffFile.setFileType(safeValue(document.getFileType()));
                    staffFile.setStaff(staff);
                    return staffFile;
                })
                .toList();
    }

    private static String safeValue(String value) {
        return value == null ? "" : value.trim();
    }
}


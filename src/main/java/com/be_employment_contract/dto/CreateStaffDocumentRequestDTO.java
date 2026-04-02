package com.be_employment_contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateStaffDocumentRequestDTO {

    @NotBlank(message = "fileName is required")
    @Size(max = 150, message = "fileName max length is 150")
    private String fileName;

    @NotBlank(message = "filePath is required")
    @Size(max = 255, message = "filePath max length is 255")
    private String filePath;

    @NotBlank(message = "fileType is required")
    private String fileType;
}


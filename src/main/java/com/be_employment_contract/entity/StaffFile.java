package com.be_employment_contract.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "staff_file")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffFile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "file_name", nullable = false, length = 150)
	private String fileName;

	@Column(name = "file_path", nullable = false, length = 255)
	private String filePath;

	private String fileType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "staff_id", nullable = false)
	private Staff staff;
}

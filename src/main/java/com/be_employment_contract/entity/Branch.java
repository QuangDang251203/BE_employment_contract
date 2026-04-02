package com.be_employment_contract.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "branch")
@NoArgsConstructor
@Data
@AllArgsConstructor
public class Branch {

	@Id
	private Long id;

	@Column(name = "branch_name", nullable = false, length = 50)
	private String branchName;

}

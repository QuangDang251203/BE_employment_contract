package com.be_employment_contract.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "staff")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Staff {

	@Id
	private Long id;

	@Column(name = "fullname", nullable = false, length = 120)
	private String fullName;

	@Column(name = "date_of_birth", nullable = false)
	private java.time.LocalDate dateOfBirth;

	@Column(nullable = false, length = 255)
	private String address;

	@Column(name = "date_issued", nullable = false)
	private java.time.LocalDate dateIssued;

	@Column(name = "issuing_location", nullable = false, length = 80)
	private String issuingLocation;

	@Column(name = "level_of_training", nullable = false, length = 150)
	private String levelOfTraining;

	@ManyToOne
	@JoinColumn(name = "branch_id", nullable = false)
	private Branch branch;

	@OneToOne(mappedBy = "staff", cascade = CascadeType.ALL)
	private Account account;

}

package com.be_employment_contract.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "account")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(nullable = false, unique = true, length = 150)
	private String email;

	@Column(nullable = false)
	private Integer status;

	@Column(name = "created_at", insertable = false, updatable = false)
	private java.time.LocalDateTime createdAt;

	@Column(name = "verified_at")
	private java.time.LocalDateTime verifiedAt;

	@OneToOne
	@jakarta.persistence.JoinColumn(name = "staff_id", nullable = false, unique = true)
	private Staff staff;
}

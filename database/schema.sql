-- Schema aligned with current application entities
CREATE DATABASE IF NOT EXISTS employment_contract;
USE employment_contract;

CREATE TABLE IF NOT EXISTS branch (
    id BIGINT PRIMARY KEY,
    branch_code VARCHAR(6) NOT NULL,
    branch_name VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS staff (
    id BIGINT PRIMARY KEY,
    fullname VARCHAR(120) NOT NULL,
    date_of_birth DATE NOT NULL,
    address VARCHAR(255) NOT NULL,
    date_issued DATE NOT NULL,
    issuing_location VARCHAR(80) NOT NULL,
    level_of_training VARCHAR(150) NOT NULL,
    branch_id BIGINT NOT NULL,
    CONSTRAINT fk_staff_branch FOREIGN KEY (branch_id) REFERENCES branch(id)
);

CREATE TABLE IF NOT EXISTS account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    staff_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP NULL,
    CONSTRAINT fk_account_staff FOREIGN KEY (staff_id) REFERENCES staff(id)
);

CREATE TABLE IF NOT EXISTS contract (
    contract_code VARCHAR(20) PRIMARY KEY,
    decision_number VARCHAR(5) NOT NULL,
    decision_date DATE NOT NULL,
    staff_id BIGINT NOT NULL,
    email VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL,
    status INT NOT NULL,
    branch_id BIGINT NOT NULL,
    level VARCHAR(20) NOT NULL,
    salary_rank VARCHAR(20) NOT NULL,
    percentage_of_salary DECIMAL(5,2) NOT NULL,
    probationary_salary DECIMAL(15,2) NOT NULL,
    CONSTRAINT fk_contract_staff FOREIGN KEY (staff_id) REFERENCES staff(id),
    CONSTRAINT fk_contract_branch FOREIGN KEY (branch_id) REFERENCES branch(id)
);


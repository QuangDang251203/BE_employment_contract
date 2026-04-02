package com.be_employment_contract.service.branch;

import com.be_employment_contract.entity.Branch;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface BranchService {
    List<Branch> getAllBranches();
}

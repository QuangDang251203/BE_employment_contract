package com.be_employment_contract.service.branch;

import com.be_employment_contract.entity.Branch;
import com.be_employment_contract.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {
    private final BranchRepository branchRepository;
    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }
}



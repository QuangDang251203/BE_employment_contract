package com.be_employment_contract.controller;

import com.be_employment_contract.entity.Branch;
import com.be_employment_contract.service.branch.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@CrossOrigin(origins = "http://localhost:3000",
        allowCredentials = "true")
@RequiredArgsConstructor
public class BranchController {
    private final BranchService branchService;
    @GetMapping("/getAllBranches")
    public List<Branch> getAllBranches() {
        return branchService.getAllBranches();
    }
}

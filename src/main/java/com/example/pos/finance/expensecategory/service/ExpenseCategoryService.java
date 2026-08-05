package com.example.pos.finance.expensecategory.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.finance.expensecategory.dto.ExpenseCategoryRequestDto;
import com.example.pos.finance.expensecategory.model.ExpenseCategory;
import com.example.pos.finance.expensecategory.repository.ExpenseCategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository repo;
    public ExpenseCategoryService(ExpenseCategoryRepository repo) { this.repo = repo; }

    public ExpenseCategory create(ExpenseCategoryRequestDto dto) {
        if (repo.existsByCategoryName(dto.getCategoryName()))
            throw new ConflictException("Category '" + dto.getCategoryName() + "' already exists");
        ExpenseCategory ec = new ExpenseCategory();
        ec.setCategoryName(dto.getCategoryName());
        ec.setCategoryDescription(dto.getCategoryDescription());
        return repo.save(ec);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseCategory> getAll(Pageable pageable) { return repo.findAll(pageable); }

    @Transactional(readOnly = true)
    public ExpenseCategory getById(UUID id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("ExpenseCategory", id));
    }

    public ExpenseCategory update(UUID id, ExpenseCategoryRequestDto dto) {
        ExpenseCategory ec = getById(id);
        if (repo.existsByCategoryNameAndIdNot(dto.getCategoryName(), id))
            throw new ConflictException("Category '" + dto.getCategoryName() + "' already exists");
        ec.setCategoryName(dto.getCategoryName());
        ec.setCategoryDescription(dto.getCategoryDescription());
        return repo.save(ec);
    }

    public void delete(UUID id) { repo.delete(getById(id)); }
}

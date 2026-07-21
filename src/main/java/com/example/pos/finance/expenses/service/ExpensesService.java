package com.example.pos.finance.expenses.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.finance.cashdrawers.model.CashDrawers;
import com.example.pos.finance.cashdrawers.repository.CashDrawersRepository;
import com.example.pos.finance.expensecategory.model.ExpenseCategory;
import com.example.pos.finance.expensecategory.repository.ExpenseCategoryRepository;
import com.example.pos.finance.expenses.dto.ExpensesRequestDto;
import com.example.pos.finance.expenses.model.Expenses;
import com.example.pos.finance.expenses.repository.ExpensesRepository;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ExpensesService {

    private final ExpensesRepository repo;
    private final ExpenseCategoryRepository catRepo;
    private final CashDrawersRepository drawerRepo;
    private final UserRepository userRepo;

    public ExpensesService(ExpensesRepository repo, ExpenseCategoryRepository catRepo,
                           CashDrawersRepository drawerRepo, UserRepository userRepo) {
        this.repo = repo;
        this.catRepo = catRepo;
        this.drawerRepo = drawerRepo;
        this.userRepo = userRepo;
    }

    public Expenses create(ExpensesRequestDto dto) {
        ExpenseCategory category = catRepo.findById(dto.getExpenseCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("ExpenseCategory", dto.getExpenseCategoryId()));
        User user = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.getUserId()));

        Expenses expense = new Expenses();
        expense.setExpenseCategory(category);
        expense.setUser(user);
        expense.setAmount(dto.getAmount());
        expense.setDescription(dto.getDescription());
        expense.setExpenseDate(LocalDateTime.now());

        if (dto.getCashDrawersId() != null) {
            CashDrawers drawer = drawerRepo.findById(dto.getCashDrawersId())
                    .orElseThrow(() -> new ResourceNotFoundException("CashDrawer", dto.getCashDrawersId()));
            expense.setCashDrawers(drawer);
        }

        return repo.save(expense);
    }

    @Transactional(readOnly = true)
    public List<Expenses> getAll() { return repo.findAll(); }

    @Transactional(readOnly = true)
    public Expenses getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }

    public void delete(Long id) { repo.delete(getById(id)); }
}

package com.example.pos.masterdata.categories.service;

import com.example.pos.common.exception.ConflictException;
import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.masterdata.categories.dto.CategoryRequestDto;
import com.example.pos.masterdata.categories.model.MedicineCategories;
import com.example.pos.masterdata.categories.repository.MedicineCategoriesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final MedicineCategoriesRepository repository;

    public CategoryService(MedicineCategoriesRepository repository) {
        this.repository = repository;
    }

    public MedicineCategories create(CategoryRequestDto dto) {
        if (repository.existsByCategoryName(dto.getCategoryName())) {
            throw new ConflictException("Category '" + dto.getCategoryName() + "' already exists");
        }
        MedicineCategories category = new MedicineCategories();
        category.setCategoryName(dto.getCategoryName());
        category.setCategoryDescription(dto.getCategoryDescription());
        return repository.save(category);
    }

    @Transactional(readOnly = true)
    public List<MedicineCategories> getAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public MedicineCategories getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicineCategory", id));
    }

    public MedicineCategories update(Long id, CategoryRequestDto dto) {
        MedicineCategories category = getById(id);
        if (repository.existsByCategoryNameAndIdNot(dto.getCategoryName(), id)) {
            throw new ConflictException("Category '" + dto.getCategoryName() + "' already exists");
        }
        category.setCategoryName(dto.getCategoryName());
        category.setCategoryDescription(dto.getCategoryDescription());
        return repository.save(category);
    }

    public void delete(Long id) {
        repository.delete(getById(id));
    }
}

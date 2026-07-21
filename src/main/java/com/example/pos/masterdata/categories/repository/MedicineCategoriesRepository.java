package com.example.pos.masterdata.categories.repository;

import com.example.pos.masterdata.categories.model.MedicineCategories;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicineCategoriesRepository extends JpaRepository<MedicineCategories, Long> {

    Optional<MedicineCategories> findByCategoryName(String categoryName);

    boolean existsByCategoryName(String categoryName);

    boolean existsByCategoryNameAndIdNot(String categoryName, Long id);
}

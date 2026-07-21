package com.example.pos.sale.saleitems.repository;

import com.example.pos.sale.saleitems.model.SaleItems;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleItemsRepository extends JpaRepository<SaleItems, Long> {
}

package com.example.pos.sale.salereturns.service;

import com.example.pos.common.exception.ResourceNotFoundException;
import com.example.pos.inventory.batches.model.MedicineBatches;
import com.example.pos.inventory.batches.repository.MedicineBatchesRepository;
import com.example.pos.inventory.stock.model.Stock;
import com.example.pos.inventory.stock.repository.StockRepository;
import com.example.pos.sale.saleitems.model.SaleItems;
import com.example.pos.sale.saleitems.repository.SaleItemsRepository;
import com.example.pos.sale.salereturnitems.model.SaleReturnItems;
import com.example.pos.sale.salereturns.dto.SaleReturnRequestDto;
import com.example.pos.sale.salereturns.dto.SaleReturnResponseDto;
import com.example.pos.sale.salereturns.model.SaleReturns;
import com.example.pos.sale.salereturns.repository.SaleReturnsRepository;
import com.example.pos.sale.sales.model.Sales;
import com.example.pos.sale.sales.repository.SalesRepository;
import com.example.pos.user.users.model.User;
import com.example.pos.user.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class SaleReturnsService {

    private final SaleReturnsRepository returnsRepository;
    private final SalesRepository salesRepository;
    private final UserRepository userRepository;
    private final MedicineBatchesRepository batchesRepository;
    private final StockRepository stockRepository;
    private final SaleItemsRepository saleItemsRepository;

    public SaleReturnsService(SaleReturnsRepository returnsRepository, SalesRepository salesRepository,
                              UserRepository userRepository, MedicineBatchesRepository batchesRepository,
                              StockRepository stockRepository, SaleItemsRepository saleItemsRepository) {
        this.returnsRepository = returnsRepository;
        this.salesRepository = salesRepository;
        this.userRepository = userRepository;
        this.batchesRepository = batchesRepository;
        this.stockRepository = stockRepository;
        this.saleItemsRepository = saleItemsRepository;
    }

    public SaleReturns createReturn(SaleReturnRequestDto dto) {
        Sales sale = salesRepository.findById(dto.getSaleId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale", dto.getSaleId()));
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.getUserId()));

        SaleReturns returnObj = new SaleReturns();
        returnObj.setSales(sale);
        returnObj.setUser(user);
        returnObj.setReason(dto.getReason());
        returnObj.setReturnDate(LocalDateTime.now());
        returnObj.setStatus("COMPLETED");

        returnsRepository.save(returnObj);

        List<SaleReturnItems> items = new ArrayList<>();
        for (SaleReturnRequestDto.ReturnItemDto item : dto.getItems()) {
            MedicineBatches batch = batchesRepository.findById(item.getMedicineBatchesId())
                    .orElseThrow(() -> new ResourceNotFoundException("MedicineBatch", item.getMedicineBatchesId()));

            SaleReturnItems ri = new SaleReturnItems();
            ri.setSaleReturns(returnObj);
            ri.setMedicineBatches(batch);
            ri.setQuantity(item.getQuantity());
            if (item.getSaleItemId() != null) {
                SaleItems si = saleItemsRepository.getReferenceById(item.getSaleItemId());
                ri.setSaleItems(si);
            }
            items.add(ri);

            Stock stock = stockRepository.findByBranchIdAndMedicineBatchesId(
                            sale.getBranch().getId(), item.getMedicineBatchesId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stock for batch " + item.getMedicineBatchesId()));
            stock.setQuantityAvailable(
                    (stock.getQuantityAvailable() != null ? stock.getQuantityAvailable() : 0) + item.getQuantity());
            stockRepository.save(stock);
        }

        returnObj.setSaleReturnItems(new java.util.HashSet<>(items));
        return returnObj;
    }

    @Transactional(readOnly = true)
    public Page<SaleReturns> getReturnsBySale(Long saleId, Pageable pageable) {
        return returnsRepository.findBySalesId(saleId, pageable);
    }

    @Transactional(readOnly = true)
    public SaleReturns getReturnById(Long id) {
        return returnsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SaleReturn", id));
    }

    public SaleReturnResponseDto toResponseDto(SaleReturns sr) {
        SaleReturnResponseDto dto = SaleReturnResponseDto.from(sr);
        if (sr.getSaleReturnItems() != null) {
            List<SaleReturnResponseDto.ReturnItemResponse> items = sr.getSaleReturnItems().stream()
                    .map(ri -> SaleReturnResponseDto.ReturnItemResponse.builder()
                            .id(ri.getId())
                            .saleItemId(ri.getSaleItems() != null ? ri.getSaleItems().getId() : null)
                            .medicineBatchesId(ri.getMedicineBatches() != null ? ri.getMedicineBatches().getId() : null)
                            .batchNumber(ri.getMedicineBatches() != null ? ri.getMedicineBatches().getBatchNumber() : null)
                            .medicineName(ri.getMedicineBatches() != null && ri.getMedicineBatches().getMedicine() != null
                                    ? ri.getMedicineBatches().getMedicine().getBrandName() : null)
                            .quantity(ri.getQuantity())
                            .build())
                    .toList();
            dto.setItems(items);
        }
        return dto;
    }
}

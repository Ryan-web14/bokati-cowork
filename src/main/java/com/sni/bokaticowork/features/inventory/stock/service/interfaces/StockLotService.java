package com.sni.bokaticowork.features.inventory.stock.service.interfaces;

import com.sni.bokaticowork.features.inventory.stock.dto.response.StockLotResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface StockLotService {
    Page<StockLotResponse> search(String itemCode,
                                  String locationCode,
                                  String lotNumber,
                                  LocalDate expiringBefore,
                                  Boolean active,
                                  Boolean remainingOnly,
                                  Pageable pageable);
}

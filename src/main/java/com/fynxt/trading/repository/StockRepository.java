package com.fynxt.trading.repository;

import com.fynxt.trading.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, String> {
}

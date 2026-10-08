package com.fynxt.trading.service;

import com.fynxt.trading.domain.Stock;
import com.fynxt.trading.domain.Trader;
import com.fynxt.trading.exception.StockSectorMismatchException;
import com.fynxt.trading.repository.StockRepository;
import com.fynxt.trading.repository.TraderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Registers traders and stocks on first use. Each insert runs in its own
 * short transaction, and must be called outside any other transaction: two
 * concurrent first requests may race to insert the same row, and the loser
 * simply catches the unique-key violation and reads the winner's row.
 */
@Component
public class ReferenceDataRegistrar {

    private static final Logger log = LoggerFactory.getLogger(ReferenceDataRegistrar.class);

    private final TraderRepository traderRepository;
    private final StockRepository stockRepository;
    private final TransactionTemplate tx;

    public ReferenceDataRegistrar(TraderRepository traderRepository,
                                  StockRepository stockRepository,
                                  PlatformTransactionManager transactionManager) {
        this.traderRepository = traderRepository;
        this.stockRepository = stockRepository;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public void ensureTrader(String traderId) {
        if (traderRepository.existsById(traderId)) {
            return;
        }
        try {
            tx.executeWithoutResult(status -> traderRepository.saveAndFlush(new Trader(traderId)));
            log.info("Registered new trader {}", traderId);
        } catch (DataIntegrityViolationException e) {
            log.debug("Trader {} was registered concurrently", traderId);
        }
    }

    /**
     * @return the stock, registering it under {@code sector} if new
     * @throws StockSectorMismatchException if the stock exists under another sector
     */
    public Stock ensureStock(String symbol, String sector) {
        Stock stock = stockRepository.findById(symbol).orElseGet(() -> insertStock(symbol, sector));
        if (!stock.getSector().equals(sector)) {
            throw new StockSectorMismatchException(symbol, stock.getSector(), sector);
        }
        return stock;
    }

    private Stock insertStock(String symbol, String sector) {
        try {
            Stock created = tx.execute(status -> stockRepository.saveAndFlush(new Stock(symbol, sector)));
            log.info("Registered new stock {} in sector {}", symbol, sector);
            return created;
        } catch (DataIntegrityViolationException e) {
            log.debug("Stock {} was registered concurrently", symbol);
            return stockRepository.findById(symbol).orElseThrow(() -> e);
        }
    }
}

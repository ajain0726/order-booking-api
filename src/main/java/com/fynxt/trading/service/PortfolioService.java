package com.fynxt.trading.service;

import com.fynxt.trading.domain.Holding;
import com.fynxt.trading.domain.Stock;
import com.fynxt.trading.overlap.OverlapReport;
import com.fynxt.trading.overlap.SectorOverlapAnalyzer;
import com.fynxt.trading.repository.HoldingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Portfolio reads, direct additions, and sector overlap analysis.
 */
@Service
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    private final HoldingRepository holdingRepository;
    private final ReferenceDataRegistrar registrar;
    private final TraderLock traderLock;
    private final SectorOverlapAnalyzer overlapAnalyzer;
    private final TransactionTemplate tx;
    private final TransactionTemplate readOnlyTx;

    public PortfolioService(HoldingRepository holdingRepository,
                            ReferenceDataRegistrar registrar,
                            TraderLock traderLock,
                            SectorOverlapAnalyzer overlapAnalyzer,
                            PlatformTransactionManager transactionManager) {
        this.holdingRepository = holdingRepository;
        this.registrar = registrar;
        this.traderLock = traderLock;
        this.overlapAnalyzer = overlapAnalyzer;
        this.tx = Transactions.readCommitted(transactionManager);
        this.readOnlyTx = Transactions.readOnly(transactionManager);
    }

    public Portfolio getPortfolio(String rawTraderId) {
        String traderId = Identifiers.traderId(rawTraderId);
        List<Holding> holdings = readOnlyTx.execute(status -> holdingRepository.findOpenPositions(traderId));
        return Portfolio.of(traderId, holdings);
    }

    public Portfolio addHolding(AddHoldingCommand command) {
        String traderId = Identifiers.traderId(command.traderId());
        registrar.ensureTrader(traderId);
        Stock stock = registrar.ensureStock(Identifiers.ticker(command.stock()), Identifiers.sector(command.sector()));

        tx.executeWithoutResult(status -> {
            traderLock.acquire(traderId);
            Holding holding = holdingRepository.findByTraderAndStock(traderId, stock.getSymbol())
                    .orElseGet(() -> new Holding(traderId, stock));
            holding.increase(command.quantity());
            holdingRepository.save(holding);
        });

        log.info("Added {} {} to portfolio of trader {}", command.quantity(), stock.getSymbol(), traderId);
        return getPortfolio(traderId);
    }

    public OverlapReport analyzeOverlap(String rawTraderId) {
        Portfolio portfolio = getPortfolio(rawTraderId);
        return overlapAnalyzer.analyze(portfolio.stocks());
    }

    /**
     * Snapshot of a trader's open positions. Maps are sorted by key so the
     * JSON output is deterministic.
     */
    public record Portfolio(String traderId, Map<String, Long> positions, Map<String, Long> sectorBreakdown) {

        static Portfolio of(String traderId, List<Holding> holdings) {
            Map<String, Long> positions = holdings.stream().collect(Collectors.toMap(
                    h -> h.getStock().getSymbol(), Holding::getQuantity, Long::sum, TreeMap::new));
            Map<String, Long> sectors = holdings.stream().collect(Collectors.groupingBy(
                    h -> h.getStock().getSector(), TreeMap::new, Collectors.summingLong(Holding::getQuantity)));
            return new Portfolio(traderId, positions, sectors);
        }

        public Set<String> stocks() {
            return positions.keySet();
        }
    }
}

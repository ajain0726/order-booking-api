package com.fynxt.trading.exception;

public class StockSectorMismatchException extends TradingException {

    public StockSectorMismatchException(String stock, String existingSector, String requestedSector) {
        super(ErrorCode.STOCK_SECTOR_MISMATCH,
                "Stock " + stock + " is registered under sector " + existingSector
                        + " but the request says " + requestedSector);
    }
}

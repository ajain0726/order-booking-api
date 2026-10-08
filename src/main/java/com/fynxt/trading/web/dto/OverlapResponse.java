package com.fynxt.trading.web.dto;

import com.fynxt.trading.overlap.OverlapReport;
import com.fynxt.trading.overlap.RiskFlag;

import java.util.List;

/**
 * Wire format of the overlap analysis; percentages are rendered as "60.00%".
 * {@code dominantBasket} is null when the portfolio overlaps no basket.
 */
public record OverlapResponse(List<Entry> overlaps, String dominantBasket, RiskFlag riskFlag) {

    public record Entry(String basket, String overlap) {
    }

    public static OverlapResponse from(OverlapReport report) {
        List<Entry> entries = report.overlaps().stream()
                .map(o -> new Entry(o.basket(), o.percentage().toPlainString() + "%"))
                .toList();
        return new OverlapResponse(entries, report.dominantBasket().orElse(null), report.riskFlag());
    }
}

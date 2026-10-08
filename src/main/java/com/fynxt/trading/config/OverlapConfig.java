package com.fynxt.trading.config;

import com.fynxt.trading.overlap.BenchmarkBaskets;
import com.fynxt.trading.overlap.SectorOverlapAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free overlap analyzer into the Spring context.
 * The analyzer itself has no Spring or persistence dependency.
 */
@Configuration
public class OverlapConfig {

    @Bean
    public SectorOverlapAnalyzer sectorOverlapAnalyzer() {
        return new SectorOverlapAnalyzer(BenchmarkBaskets.DEFAULT);
    }
}

package com.fynxt.trading.web;

import com.fynxt.trading.service.PortfolioService;
import com.fynxt.trading.web.dto.AddHoldingRequest;
import com.fynxt.trading.web.dto.OverlapResponse;
import com.fynxt.trading.web.dto.PortfolioResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/traders/{traderId}/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public PortfolioResponse getPortfolio(@PathVariable @Size(max = 32) String traderId) {
        return PortfolioResponse.from(portfolioService.getPortfolio(traderId));
    }

    @PostMapping("/holdings")
    public PortfolioResponse addHolding(@PathVariable @Size(max = 32) String traderId,
                                        @Valid @RequestBody AddHoldingRequest request) {
        return PortfolioResponse.from(portfolioService.addHolding(request.toCommand(traderId)));
    }

    @GetMapping("/overlap")
    public OverlapResponse getOverlap(@PathVariable @Size(max = 32) String traderId) {
        return OverlapResponse.from(portfolioService.analyzeOverlap(traderId));
    }
}

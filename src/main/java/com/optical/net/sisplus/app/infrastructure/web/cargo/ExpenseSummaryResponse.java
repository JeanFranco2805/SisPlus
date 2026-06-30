package com.optical.net.sisplus.app.infrastructure.web.cargo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@Builder
public class ExpenseSummaryResponse {
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Double total;
    private Long count;
    private Map<String, Double> expensesByCategory;
}

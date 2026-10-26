package com.siva.assistant.expense;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DayTotal(LocalDate date, BigDecimal total) {
}

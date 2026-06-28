package com.expensetracker.controller;

import com.expensetracker.model.Transaction;
import com.expensetracker.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/transactions")
    public List<Transaction> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return (from != null && to != null) ? service.findByDateRange(from, to) : service.findAll();
    }

    @GetMapping("/transactions/{id}")
    public Transaction get(@PathVariable String id) {
        return service.findById(id);
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction create(@Valid @RequestBody Transaction transaction) {
        transaction.setId(null);
        return service.save(transaction);
    }

    @PutMapping("/transactions/{id}")
    public Transaction update(@PathVariable String id, @Valid @RequestBody Transaction transaction) {
        transaction.setId(id);
        return service.save(transaction);
    }

    @DeleteMapping("/transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    @GetMapping("/summary")
    public Map<String, BigDecimal> summary(@RequestParam(defaultValue = "monthly") String view) {
        return "weekly".equals(view) ? service.getCurrentWeekSummary() : service.getCurrentMonthSummary();
    }

    @GetMapping("/charts")
    public ChartData charts(@RequestParam(defaultValue = "monthly") String view) {
        boolean weekly = "weekly".equals(view);
        Map<String, BigDecimal[]> data = weekly ? service.getCurrentWeekDailyData() : service.getLast6MonthsData();
        String title = weekly ? "This Week — Daily Breakdown" : "Last 6 Months";

        List<String> labels = new ArrayList<>(data.keySet());
        List<BigDecimal> income = data.values().stream().map(v -> v[0]).toList();
        List<BigDecimal> expense = data.values().stream().map(v -> v[1]).toList();

        return new ChartData(labels, income, expense, title);
    }

    @GetMapping("/categories")
    public CategoryData categories() {
        LocalDate[] range = service.currentMonthRange();
        Map<String, BigDecimal> data = service.getCategoryBreakdown(range[0], range[1]);
        return new CategoryData(new ArrayList<>(data.keySet()), new ArrayList<>(data.values()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }

    public record ChartData(List<String> labels, List<BigDecimal> income, List<BigDecimal> expense, String title) {}

    public record CategoryData(List<String> labels, List<BigDecimal> amounts) {}
}

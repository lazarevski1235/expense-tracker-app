package com.expensetracker.service;

import com.expensetracker.model.Transaction;
import com.expensetracker.model.Transaction.TransactionType;
import com.expensetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> findAll() {
        return transactionRepository.findAll().stream()
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .toList();
    }

    public Transaction findById(String id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
    }

    public List<Transaction> findByDateRange(LocalDate from, LocalDate to) {
        return transactionRepository.findByDateBetweenOrderByDateDesc(from, to);
    }

    public Transaction save(Transaction t) {
        return transactionRepository.save(t);
    }

    public void delete(String id) {
        transactionRepository.deleteById(id);
    }

    public Map<String, BigDecimal> getCurrentMonthSummary() {
        LocalDate[] r = currentMonthRange();
        return buildSummary(
                sumByTypeInRange(TransactionType.INCOME, r[0], r[1]),
                sumByTypeInRange(TransactionType.EXPENSE, r[0], r[1])
        );
    }

    public Map<String, BigDecimal> getCurrentWeekSummary() {
        LocalDate[] r = currentWeekRange();
        return buildSummary(
                sumByTypeInRange(TransactionType.INCOME, r[0], r[1]),
                sumByTypeInRange(TransactionType.EXPENSE, r[0], r[1])
        );
    }

    public Map<String, BigDecimal[]> getLast6MonthsData() {
        Map<String, BigDecimal[]> data = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate m = today.minusMonths(i);
            LocalDate from = m.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate to = m.with(TemporalAdjusters.lastDayOfMonth());
            String label = m.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + m.getYear();
            data.put(label, new BigDecimal[]{
                    sumByTypeInRange(TransactionType.INCOME, from, to),
                    sumByTypeInRange(TransactionType.EXPENSE, from, to)
            });
        }
        return data;
    }

    public Map<String, BigDecimal[]> getCurrentWeekDailyData() {
        LocalDate[] range = currentWeekRange();
        LocalDate weekStart = range[0];

        Map<LocalDate, BigDecimal[]> dayMap = new LinkedHashMap<>();
        for (int d = 0; d < 7; d++) {
            dayMap.put(weekStart.plusDays(d), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        List<Transaction> weekTx = transactionRepository.findByDateBetween(range[0], range[1]);
        for (Transaction t : weekTx) {
            if (!dayMap.containsKey(t.getDate())) continue;
            BigDecimal[] vals = dayMap.get(t.getDate());
            if (t.getType() == TransactionType.INCOME) {
                vals[0] = vals[0].add(t.getAmount());
            } else {
                vals[1] = vals[1].add(t.getAmount());
            }
        }

        Map<String, BigDecimal[]> result = new LinkedHashMap<>();
        dayMap.forEach((date, vals) -> {
            String label = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + " " + String.format("%02d", date.getDayOfMonth());
            result.put(label, vals);
        });
        return result;
    }

    public Map<String, BigDecimal> getCategoryBreakdown(LocalDate from, LocalDate to) {
        return transactionRepository.findByTypeAndDateBetween(TransactionType.EXPENSE, from, to)
                .stream()
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new
                ));
    }

    private BigDecimal sumByTypeInRange(TransactionType type, LocalDate from, LocalDate to) {
        return transactionRepository.findByTypeAndDateBetween(type, from, to)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, BigDecimal> buildSummary(BigDecimal income, BigDecimal expense) {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        m.put("totalIncome", income);
        m.put("totalExpense", expense);
        m.put("balance", income.subtract(expense));
        return m;
    }

    public LocalDate[] currentMonthRange() {
        LocalDate today = LocalDate.now();
        return new LocalDate[]{
                today.with(TemporalAdjusters.firstDayOfMonth()),
                today.with(TemporalAdjusters.lastDayOfMonth())
        };
    }

    public LocalDate[] currentWeekRange() {
        LocalDate today = LocalDate.now();
        return new LocalDate[]{
                today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        };
    }
}

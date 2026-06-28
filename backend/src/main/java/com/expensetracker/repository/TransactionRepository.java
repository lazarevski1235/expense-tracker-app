package com.expensetracker.repository;

import com.expensetracker.model.Transaction;
import com.expensetracker.model.Transaction.TransactionType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    List<Transaction> findByDateBetweenOrderByDateDesc(LocalDate from, LocalDate to);
    List<Transaction> findByTypeAndDateBetween(TransactionType type, LocalDate from, LocalDate to);
    List<Transaction> findByDateBetween(LocalDate from, LocalDate to);
}

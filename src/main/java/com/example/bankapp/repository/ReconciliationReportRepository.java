package com.example.bankapp.repository;

import com.example.bankapp.model.ReconciliationReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReconciliationReportRepository extends JpaRepository<ReconciliationReport, Long> {

    List<ReconciliationReport> findAllByOrderByReportDateDesc();

    Optional<ReconciliationReport> findTopByOrderByReportDateDesc();
}

package com.example.bankapp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class ReconciliationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime reportDate;

    private int totalAccounts;

    private int flaggedAccounts;

    private int discrepancyAccounts;

    private BigDecimal totalBalance;

    private String status;

    @Lob
    private String details;

    public ReconciliationReport() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDateTime reportDate) {
        this.reportDate = reportDate;
    }

    public int getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(int totalAccounts) {
        this.totalAccounts = totalAccounts;
    }

    public int getFlaggedAccounts() {
        return flaggedAccounts;
    }

    public void setFlaggedAccounts(int flaggedAccounts) {
        this.flaggedAccounts = flaggedAccounts;
    }

    public int getDiscrepancyAccounts() {
        return discrepancyAccounts;
    }

    public void setDiscrepancyAccounts(int discrepancyAccounts) {
        this.discrepancyAccounts = discrepancyAccounts;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}

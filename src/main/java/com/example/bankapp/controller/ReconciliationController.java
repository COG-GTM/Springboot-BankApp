package com.example.bankapp.controller;

import com.example.bankapp.model.ReconciliationReport;
import com.example.bankapp.repository.ReconciliationReportRepository;
import com.example.bankapp.service.ReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reconciliation")
public class ReconciliationController {

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private ReconciliationReportRepository reportRepository;

    @GetMapping("/reports")
    public String listReports(Model model) {
        model.addAttribute("reports", reportRepository.findAllByOrderByReportDateDesc());
        return "reconciliation-reports";
    }

    @GetMapping("/reports/{id}")
    public String viewReport(@PathVariable Long id, Model model) {
        ReconciliationReport report = reportRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Report not found"));
        model.addAttribute("report", report);
        return "reconciliation-detail";
    }

    @PostMapping("/run")
    public String runManually() {
        reconciliationService.runReconciliation();
        return "redirect:/reconciliation/reports";
    }
}

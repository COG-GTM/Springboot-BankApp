package com.cascade.disclosure.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cascade.disclosure.DisclosureService;
import com.cascade.disclosure.core.CoreOutputs;

@Controller
public class DisclosureController {
    private final DisclosureService service;

    public DisclosureController(DisclosureService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("products", service.productApys());
        model.addAttribute("loans", service.loanAprs());
        model.addAttribute("parity", service.parity());
        model.addAttribute("legacyEarned", service.apyEarned(CoreOutputs.LEGACY).values());
        return "index";
    }

    @GetMapping("/api/disclosures/products")
    @ResponseBody
    public List<DisclosureService.ProductApy> products() {
        return service.productApys();
    }

    @GetMapping("/api/disclosures/loans")
    @ResponseBody
    public List<DisclosureService.LoanApr> loans() {
        return service.loanAprs();
    }

    @GetMapping("/api/disclosures/accounts/{accountId}/apy-earned")
    @ResponseBody
    public ResponseEntity<DisclosureService.ApyEarned> apyEarned(@PathVariable long accountId,
                                                                 @RequestParam(defaultValue = CoreOutputs.LEGACY) String source) {
        return service.apyEarned(source, accountId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/disclosures/parity")
    @ResponseBody
    public DisclosureService.Parity parity() {
        return service.parity();
    }

    @GetMapping("/api/health")
    @ResponseBody
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "cascade-disclosure-service");
    }
}

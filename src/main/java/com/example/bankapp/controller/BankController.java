package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class BankController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.find_account_by_username(username);
        model.addAttribute("account", account);
        return "dashboard";
    }

    @GetMapping("/register")
    public String show_registration_form() {
        return "register";
    }

    @PostMapping("/register")
    public String register_account(@RequestParam String username, @RequestParam String password, Model model) {
        try {
            accountService.register_account(username, password);
            return "redirect:/login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam BigDecimal amount) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.find_account_by_username(username);
        accountService.deposit(account, amount);
        return "redirect:/dashboard";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam BigDecimal amount, Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.find_account_by_username(username);

        try {
            accountService.withdraw(account, amount);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", account);
            return "dashboard";
        }

        return "redirect:/dashboard";
    }

    @GetMapping("/transactions")
    public String transaction_history(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.find_account_by_username(username);
        model.addAttribute("transactions", accountService.get_transaction_history(account));
        return "transactions";
    }

    @PostMapping("/transfer")
    public String transfer_amount(@RequestParam String toUsername, @RequestParam BigDecimal amount, Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account fromAccount = accountService.find_account_by_username(username);

        try {
            accountService.transfer_amount(fromAccount, toUsername, amount);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("account", fromAccount);
            return "dashboard";
        }

        return "redirect:/dashboard";
    }

}

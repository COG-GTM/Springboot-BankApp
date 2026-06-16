package com.example.bankapp.controller;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.service.AccountService;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Date;
import java.util.List;

@Path("/")
public class BankController {

    @Inject
    AccountService accountService;

    @Inject
    SecurityIdentity securityIdentity;

    @CheckedTemplate
    static class Templates {
        public static native TemplateInstance login(boolean error);
        public static native TemplateInstance register(String error);
        public static native TemplateInstance dashboard(Account account, String error);
        public static native TemplateInstance transactions(List<Transaction> transactions);
    }

    @GET
    @Path("login")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance login(@QueryParam("error") String error) {
        return Templates.login(error != null);
    }

    @GET
    @Path("register")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance showRegistrationForm() {
        return Templates.register(null);
    }

    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response registerAccount(@FormParam("username") String username,
                                    @FormParam("password") String password) {
        try {
            accountService.registerAccount(username, password);
            return Response.seeOther(URI.create("/login")).build();
        } catch (RuntimeException e) {
            return Response.ok(Templates.register(e.getMessage())).build();
        }
    }

    @GET
    @Path("dashboard")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance dashboard() {
        Account account = accountService.findAccountByUsername(currentUsername());
        return Templates.dashboard(account, null);
    }

    @POST
    @Path("deposit")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response deposit(@FormParam("amount") BigDecimal amount) {
        Account account = accountService.findAccountByUsername(currentUsername());
        accountService.deposit(account, amount);
        return Response.seeOther(URI.create("/dashboard")).build();
    }

    @POST
    @Path("withdraw")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response withdraw(@FormParam("amount") BigDecimal amount) {
        Account account = accountService.findAccountByUsername(currentUsername());
        try {
            accountService.withdraw(account, amount);
        } catch (RuntimeException e) {
            return Response.ok(Templates.dashboard(account, e.getMessage())).build();
        }
        return Response.seeOther(URI.create("/dashboard")).build();
    }

    @GET
    @Path("transactions")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance transactionHistory() {
        Account account = accountService.findAccountByUsername(currentUsername());
        return Templates.transactions(accountService.getTransactionHistory(account));
    }

    @POST
    @Path("transfer")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response transferAmount(@FormParam("toUsername") String toUsername,
                                   @FormParam("amount") BigDecimal amount) {
        Account fromAccount = accountService.findAccountByUsername(currentUsername());
        try {
            accountService.transferAmount(fromAccount, toUsername, amount);
        } catch (RuntimeException e) {
            return Response.ok(Templates.dashboard(fromAccount, e.getMessage())).build();
        }
        return Response.seeOther(URI.create("/dashboard")).build();
    }

    @GET
    @Path("logout")
    public Response logout() {
        NewCookie expired = new NewCookie.Builder("quarkus-credential")
                .value("")
                .path("/")
                .maxAge(0)
                .expiry(new Date(0))
                .build();
        return Response.seeOther(URI.create("/login")).cookie(expired).build();
    }

    private String currentUsername() {
        return securityIdentity.getPrincipal().getName();
    }
}

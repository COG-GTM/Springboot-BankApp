package com.example.bankapp.security;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;
import org.wildfly.security.password.interfaces.BCryptPassword;
import org.wildfly.security.password.util.ModularCrypt;

/**
 * Authenticates form-login requests against the MongoDB {@code accounts} collection.
 */
@ApplicationScoped
public class AccountIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    @Inject
    AccountRepository accountRepository;

    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request,
                                              AuthenticationRequestContext context) {
        return context.runBlocking(() -> {
            String username = request.getUsername();
            String password = new String(request.getPassword().getPassword());

            Account account = accountRepository.findByUsername(username).orElse(null);
            if (account == null || !verifyPassword(password, account.getPassword())) {
                throw new AuthenticationFailedException("Invalid username or password");
            }

            return QuarkusSecurityIdentity.builder()
                    .setPrincipal(new QuarkusPrincipal(username))
                    .addRole("user")
                    .build();
        });
    }

    private boolean verifyPassword(String providedPassword, String storedHash) {
        if (storedHash == null) {
            return false;
        }
        try {
            PasswordFactory passwordFactory = PasswordFactory.getInstance(
                    BCryptPassword.ALGORITHM_BCRYPT, new WildFlyElytronPasswordProvider());
            Password storedPassword = passwordFactory.translate(ModularCrypt.decode(storedHash));
            return passwordFactory.verify(storedPassword, providedPassword.toCharArray());
        } catch (Exception e) {
            return false;
        }
    }
}

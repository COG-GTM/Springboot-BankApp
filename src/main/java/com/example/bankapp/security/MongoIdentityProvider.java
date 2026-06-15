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
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;
import org.wildfly.security.password.interfaces.BCryptPassword;
import org.wildfly.security.password.util.ModularCrypt;

/**
 * Authenticates form-login users against the MongoDB {@code accounts} collection,
 * verifying the bcrypt (Modular Crypt Format) password hash produced by BcryptUtil.
 */
@ApplicationScoped
public class MongoIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

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
            char[] password = request.getPassword().getPassword();

            Account account = accountRepository.findByUsername(username).orElse(null);
            if (account == null || !verify(account.getPassword(), password)) {
                throw new AuthenticationFailedException();
            }

            return QuarkusSecurityIdentity.builder()
                    .setPrincipal(new QuarkusPrincipal(username))
                    .addRole("USER")
                    .build();
        });
    }

    private boolean verify(String storedHash, char[] password) {
        if (storedHash == null) {
            return false;
        }
        try {
            PasswordFactory factory = PasswordFactory.getInstance(
                    BCryptPassword.ALGORITHM_BCRYPT, new WildFlyElytronPasswordProvider());
            BCryptPassword restored = (BCryptPassword) factory.translate(ModularCrypt.decode(storedHash));
            return factory.verify(restored, password);
        } catch (Exception e) {
            return false;
        }
    }
}

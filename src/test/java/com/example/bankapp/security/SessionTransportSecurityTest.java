package com.example.bankapp.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SessionTransportSecurityTest {

    @LocalServerPort
    int port;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private HttpResponse<Void> getDashboard(String forwardedProto) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/dashboard"));
        if (forwardedProto != null) {
            request.header("X-Forwarded-Proto", forwardedProto);
        }
        return client.send(request.GET().build(), HttpResponse.BodyHandlers.discarding());
    }

    @Test
    void sessionCookieIsMarkedSecure() throws Exception {
        List<String> cookies = getDashboard(null).headers().allValues("Set-Cookie");

        assertThat(cookies).anySatisfy(cookie -> assertThat(cookie).startsWith("JSESSIONID="));
        assertThat(cookies).filteredOn(cookie -> cookie.startsWith("JSESSIONID="))
                .allSatisfy(cookie -> assertThat(cookie).containsIgnoringCase("; Secure"));
    }

    @Test
    void forwardedProtoFromTrustedProxyIsHonoured() throws Exception {
        HttpResponse<Void> response = getDashboard("https");

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location")).hasValueSatisfying(
                location -> assertThat(location).startsWith("https://localhost/login"));
    }
}

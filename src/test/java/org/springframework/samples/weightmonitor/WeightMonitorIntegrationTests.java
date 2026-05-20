package org.springframework.samples.weightmonitor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

@SpringBootTest(
        classes = WeightMonitorApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9rIQ==",
                "jwt.expiration=86400000",
                "spring.sql.init.mode=never",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
class WeightMonitorIntegrationTests {

    @LocalServerPort
    private int port;

    private RestClient http;
    private String token;
    private String providerId;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void registerAndLogin() {
        // suppress 4xx/5xx exceptions so every test can assert on the status code directly
        http = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, resp) -> {})
                .build();

        providerId = "doc-" + UUID.randomUUID().toString().substring(0, 8);

        http.post()
                .uri("/provider/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("professionalId", providerId,
                        "password", "Test@1234",
                        "firstName", "Test",
                        "lastName", "Provider"))
                .retrieve()
                .toBodilessEntity();

        ResponseEntity<Map> login = http.post()
                .uri("/provider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("professionalId", providerId, "password", "Test@1234"))
                .retrieve()
                .toEntity(Map.class);

        token = (String) login.getBody().get("token");
    }

    // ── Provider ──────────────────────────────────────────────────────

    @Test
    void register_newProvider_returns201() {
        ResponseEntity<Map> response = http.post()
                .uri("/provider/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("professionalId", "doc-" + UUID.randomUUID().toString().substring(0, 8),
                        "password", "Test@1234",
                        "firstName", "Jane",
                        "lastName", "Smith"))
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("id");
    }

    @Test
    void register_duplicateId_returns409() {
        // providerId is already registered in @BeforeEach — just try again
        ResponseEntity<Map> response = http.post()
                .uri("/provider/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("professionalId", providerId,
                        "password", "x",
                        "firstName", "A",
                        "lastName", "B"))
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void login_validCredentials_returnsToken() {
        ResponseEntity<Map> response = http.post()
                .uri("/provider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("professionalId", providerId, "password", "Test@1234"))
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) response.getBody().get("token")).isNotBlank();
    }

    @Test
    void login_wrongPassword_returns401() {
        ResponseEntity<Map> response = http.post()
                .uri("/provider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("professionalId", providerId, "password", "wrong"))
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_unknownUser_returns401() {
        ResponseEntity<Map> response = http.post()
                .uri("/provider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("professionalId", "nobody", "password", "x"))
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Child ─────────────────────────────────────────────────────────

    @Test
    void createChild_withValidToken_returns201() {
        ResponseEntity<Map> response = http.post()
                .uri("/child/create")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body("""
                        {"firstName":"Alice","lastName":"Test",
                         "birthDate":"2015-06-01","gender":"FEMALE"}
                        """)
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("id");
    }

    @Test
    void createChild_duplicate_returns409() {
        String body = """
                {"firstName":"Bob","lastName":"Dup",
                 "birthDate":"2014-03-15","gender":"MALE"}
                """;

        http.post()
                .uri("/child/create")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        ResponseEntity<Map> second = http.post()
                .uri("/child/create")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(body)
                .retrieve()
                .toEntity(Map.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createChild_withoutToken_returns403() {
        ResponseEntity<Void> response = http.post()
                .uri("/child/create")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"firstName":"Charlie","lastName":"Noauth",
                         "birthDate":"2016-01-01","gender":"MALE"}
                        """)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void searchChild_withValidToken_returns200() {
        ResponseEntity<Map> response = http.get()
                .uri("/child/search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("totalElements");
    }
}
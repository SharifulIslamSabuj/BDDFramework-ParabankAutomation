package com.parabank.parasoft.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Lightweight HTTP client for ParaBank setup operations.
 *
 * <p>Uses Java 17's built-in {@link HttpClient} — no additional dependencies.
 * Currently supports user registration via HTTP form POST, which is
 * significantly faster than browser-based registration.
 *
 * <p>Used by {@code Hooks.ensureDefaultTestUserExists()} as the primary
 * registration mechanism; the existing browser-based approach is the fallback.
 */
public class ParaBankApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ParaBankApiClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final String baseUrl;
    private final HttpClient httpClient;

    public ParaBankApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Registers a new user via ParaBank's registration form endpoint (HTTP POST).
     *
     * <p>Considers both of the following as success so the caller can proceed:
     * <ul>
     *   <li>The server redirects away from {@code register.htm} — account created.</li>
     *   <li>The response body contains "This username already exists" — user already present.</li>
     * </ul>
     *
     * @return {@code true} if the user is available for login; {@code false} on error
     */
    public boolean registerUser(String firstName, String lastName,
                                String address, String city, String state,
                                String zipCode, String phone, String ssn,
                                String username, String password) {
        logger.info("[API] Registering user '{}' via HTTP POST", username);
        try {
            String formBody = buildFormBody(firstName, lastName, address, city, state,
                    zipCode, phone, ssn, username, password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "register.htm"))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String finalUrl = response.uri().toString();
            String body = response.body();

            // Success: server redirected away from the registration page after creating the account
            if (!finalUrl.contains("register.htm")) {
                logger.info("[API] User '{}' registered — server redirected to: {}", username, finalUrl);
                return true;
            }
            // Also success: user already exists on this demo server
            if (body.contains("This username already exists")) {
                logger.info("[API] User '{}' already exists on server — no action needed", username);
                return true;
            }

            logger.warn("[API] Registration for '{}' did not redirect. HTTP {}, final URL: {}",
                    username, response.statusCode(), finalUrl);
            return false;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("[API] Registration interrupted for '{}'", username);
            return false;
        } catch (Exception e) {
            logger.warn("[API] Registration via API unavailable for '{}': {}", username, e.getMessage());
            return false;
        }
    }

    private String buildFormBody(String firstName, String lastName, String address,
                                  String city, String state, String zipCode,
                                  String phone, String ssn, String username, String password) {
        return String.join("&",
                "customer.firstName="      + encode(firstName),
                "customer.lastName="       + encode(lastName),
                "customer.address.street=" + encode(address),
                "customer.address.city="   + encode(city),
                "customer.address.state="  + encode(state),
                "customer.address.zipCode="+ encode(zipCode),
                "customer.phoneNumber="    + encode(phone),
                "customer.ssn="            + encode(ssn),
                "customer.username="       + encode(username),
                "customer.password="       + encode(password),
                "repeatedPassword="        + encode(password)
        );
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

package dev.quantumlab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/** Starts the real service on a free port and calls it over HTTP. */
class ServiceOverHttpTest {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static ConfigurableApplicationContext context;
    private static String base;

    @BeforeAll
    static void startTheService() {
        context = SpringApplication.run(QuantumLabServiceApplication.class, "--server.port=0");
        String port = context.getEnvironment().getProperty("local.server.port");
        assertNotNull(port, "the server did not report its port");
        base = "http://127.0.0.1:" + port;
    }

    @AfterAll
    static void stopTheService() {
        if (context != null) {
            context.close();
        }
    }

    private static HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + path)).GET().build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String contentType(HttpResponse<String> response) {
        return response.headers().firstValue("Content-Type").orElse("");
    }

    @Test
    void statusIsServedAsJson() throws Exception {
        HttpResponse<String> response = get("/api/status");

        assertEquals(200, response.statusCode());
        assertTrue(contentType(response).startsWith("application/json"));
        assertEquals("{\"service\":\"QuantumLab service\",\"status\":\"ok\"}", response.body());
    }

    @Test
    void rejectedInputBecomesA400WithAJsonMessage() throws Exception {
        HttpResponse<String> response = get("/api/decay?rate=NaN&time=2");

        assertEquals(400, response.statusCode());
        assertTrue(contentType(response).startsWith("application/json"));
        assertEquals(
            "{\"error\":\"rate must be a plain decimal number, got 'NaN'\"}", response.body());
    }

    @Test
    void measurementReproducesThePinnedSequenceOverHttp() throws Exception {
        HttpResponse<String> response =
            get("/api/measurement?p=0.5&basis=Z&seed=2026&trials=20");

        assertEquals(200, response.statusCode());
        Matcher outcome = Pattern.compile("\"outcome\"\\s*:\\s*(\\d+)").matcher(response.body());
        StringBuilder sequence = new StringBuilder();
        while (outcome.find()) {
            sequence.append(outcome.group(1));
        }
        // The same twenty outcomes that ProjectiveMeasurementTest pins for seed 2026 on |+> in Z.
        assertEquals("10101111000100100011", sequence.toString());
    }

    @Test
    void nonAsciiTextSurvivesTheTrip() throws Exception {
        HttpResponse<String> response = get("/api/measurement?p=0.5&basis=Z&seed=1&trials=10");

        assertTrue(response.body().contains("\u27e9"), "the ket bracket in the labels was lost");
    }
}

package com.rajesh.urlshortener.api;

import com.rajesh.urlshortener.error.ShortUrlExceptionHandler;
import com.rajesh.urlshortener.service.CreatedShortUrl;
import com.rajesh.urlshortener.service.ShortUrlCreationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.sql.SQLException;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ShortUrlControllerTest {

    private ShortUrlCreationService service;
    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ShortUrlCreationService.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(new ShortUrlController(service))
                .setControllerAdvice(new ShortUrlExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.destroy();
    }

    @Test
    void createsUrlAndReturns201ResponseWithRequestBasedLocation() throws Exception {
        String originalUrl = "https://example.com/path";
        when(service.create(originalUrl, null))
                .thenReturn(new CreatedShortUrl("Ab12Cd34", originalUrl, null));

        mockMvc.perform(post("/api/v1/urls")
                        .with(request -> {
                            request.setScheme("https");
                            request.setServerName("short.example");
                            request.setServerPort(443);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://example.com/path"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "https://short.example/Ab12Cd34"))
                .andExpect(jsonPath("$.code").value("Ab12Cd34"))
                .andExpect(jsonPath("$.originalUrl").value(originalUrl))
                .andExpect(jsonPath("$.expiresAt").doesNotExist())
                .andExpect(jsonPath("$.shortUrl").value("https://short.example/Ab12Cd34"));

        verify(service).create(originalUrl, null);
    }

    @Test
    void preservesOptionalExpirationTimestamp() throws Exception {
        String originalUrl = "https://example.com/path";
        Instant expiresAt = Instant.parse("2026-12-31T23:59:59Z");
        when(service.create(originalUrl, expiresAt))
                .thenReturn(new CreatedShortUrl("Ab12Cd34", originalUrl, expiresAt));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://example.com/path","expiresAt":"2026-12-31T23:59:59Z"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").value("2026-12-31T23:59:59Z"));

        verify(service).create(originalUrl, expiresAt);
    }

    @Test
    void rejectsBlankUrlWithProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsMissingUrlWithProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void mapsMalformedUrlToBadRequest() throws Exception {
        when(service.create(eq("https://exa mple.com"), eq(null)))
                .thenThrow(new IllegalArgumentException("malformed URL details"));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://exa mple.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The URL creation request is invalid"));
    }

    @Test
    void mapsUnsupportedSchemeToBadRequest() throws Exception {
        when(service.create(eq("ftp://example.com"), eq(null)))
                .thenThrow(new IllegalArgumentException("unsupported scheme"));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"ftp://example.com"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void doesNotExposePersistenceErrorDetails() throws Exception {
        when(service.create(eq("https://example.com"), eq(null)))
                .thenThrow(new DataIntegrityViolationException("SQL secret", new SQLException("database secret")));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://example.com"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("The short URL could not be created"));
    }
}

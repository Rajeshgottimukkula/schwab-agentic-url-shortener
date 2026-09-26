package com.rajesh.urlshortener.api;

import com.rajesh.urlshortener.error.ShortUrlExceptionHandler;
import com.rajesh.urlshortener.service.CreatedShortUrl;
import com.rajesh.urlshortener.service.ShortUrlCreationService;
import com.rajesh.urlshortener.service.LookedUpShortUrl;
import com.rajesh.urlshortener.service.ShortUrlLookupService;
import com.rajesh.urlshortener.service.ShortUrlRedirectService;
import com.rajesh.urlshortener.service.ShortCodeGenerationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import java.sql.SQLException;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(OutputCaptureExtension.class)
class ShortUrlControllerTest {

    private ShortUrlCreationService service;
    private ShortUrlRedirectService redirectService;
    private ShortUrlLookupService lookupService;
    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ShortUrlCreationService.class);
        redirectService = mock(ShortUrlRedirectService.class);
        lookupService = mock(ShortUrlLookupService.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(
                        new ShortUrlController(service, lookupService), new RedirectController(redirectService)
                )
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
    void persistenceFailureIsLoggedAndReturnsSanitizedProblemDetail(CapturedOutput output) throws Exception {
        when(service.create(eq("https://example.com"), eq(null)))
                .thenThrow(new DataIntegrityViolationException("SQL secret", new SQLException("database secret")));

        String response = mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://example.com"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("The request could not be completed"))
                .andReturn().getResponse().getContentAsString();

        assertFalse(response.contains("SQL secret"));
        assertFalse(response.contains("database secret"));
        org.junit.jupiter.api.Assertions.assertTrue(output.toString().contains("Persistence failure"));
        org.junit.jupiter.api.Assertions.assertTrue(output.toString().contains("DataIntegrityViolationException"));
        org.junit.jupiter.api.Assertions.assertFalse(output.toString().contains("SQL secret"));
        org.junit.jupiter.api.Assertions.assertFalse(output.toString().contains("database secret"));
    }

    @Test
    void unexpectedFailureIsLoggedAndReturnsSanitizedProblemDetail(CapturedOutput output) throws Exception {
        when(lookupService.lookup("Ab12Cd34"))
                .thenThrow(new IllegalStateException("internal diagnostic detail"));

        String response = mockMvc.perform(get("/api/v1/urls/Ab12Cd34"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("The request could not be completed"))
                .andReturn().getResponse().getContentAsString();

        assertFalse(response.contains("internal diagnostic detail"));
        org.junit.jupiter.api.Assertions.assertTrue(output.toString().contains("Unexpected failure"));
        org.junit.jupiter.api.Assertions.assertTrue(output.toString().contains("IllegalStateException"));
        org.junit.jupiter.api.Assertions.assertFalse(output.toString().contains("internal diagnostic detail"));
    }

    @Test
    void codeGenerationFailureRemainsSanitized503() throws Exception {
        when(service.create(eq("https://example.com"), eq(null)))
                .thenThrow(new ShortCodeGenerationException(new IllegalStateException("private database detail")));

        String response = mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://example.com"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("A unique short code could not be allocated"))
                .andReturn().getResponse().getContentAsString();

        assertFalse(response.contains("private database detail"));
    }

    @Test
    void redirectsWith302AndStoredDestination() throws Exception {
        when(redirectService.resolveAndRecordClick("Ab12Cd34"))
                .thenReturn("https://example.com/path?source=test#section");

        mockMvc.perform(get("/Ab12Cd34"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/path?source=test#section"));

        verify(redirectService).resolveAndRecordClick("Ab12Cd34");
    }

    @Test
    void mapsUnknownOrExpiredRedirectToProblemDetail404() throws Exception {
        when(redirectService.resolveAndRecordClick("missing"))
                .thenThrow(new com.rajesh.urlshortener.service.ShortUrlNotFoundException("missing"));

        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("The short URL does not exist or has expired"));
    }

    @Test
    void looksUpMappingAndBuildsShortUrlFromCurrentRequest() throws Exception {
        Instant expiresAt = Instant.parse("2026-12-31T23:59:59Z");
        when(lookupService.lookup("Ab12Cd34"))
                .thenReturn(new LookedUpShortUrl("Ab12Cd34", "https://example.com/path", expiresAt, 7));

        mockMvc.perform(get("/api/v1/urls/Ab12Cd34")
                        .with(request -> {
                            request.setScheme("https");
                            request.setServerName("short.example");
                            request.setServerPort(443);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("Ab12Cd34"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/path"))
                .andExpect(jsonPath("$.expiresAt").value("2026-12-31T23:59:59Z"))
                .andExpect(jsonPath("$.clickCount").value(7))
                .andExpect(jsonPath("$.shortUrl").value("https://short.example/Ab12Cd34"));

        verify(lookupService).lookup("Ab12Cd34");
    }

    @Test
    void unknownLookupReturnsSanitizedProblemDetail404() throws Exception {
        when(lookupService.lookup("missing"))
                .thenThrow(new com.rajesh.urlshortener.service.ShortUrlNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/urls/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("The short URL does not exist or has expired"));
    }
}

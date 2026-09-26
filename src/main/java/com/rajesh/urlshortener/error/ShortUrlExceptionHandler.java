package com.rajesh.urlshortener.error;

import com.rajesh.urlshortener.service.ShortCodeGenerationException;
import com.rajesh.urlshortener.service.ShortUrlNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class ShortUrlExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShortUrlExceptionHandler.class);

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ProblemDetail> handleInvalidRequest(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "The URL creation request is invalid", request);
    }

    @ExceptionHandler(ShortCodeGenerationException.class)
    public ResponseEntity<ProblemDetail> handleCodeGenerationFailure(
            ShortCodeGenerationException exception,
            HttpServletRequest request
    ) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Short URL unavailable",
                "A unique short code could not be allocated", request);
    }

    @ExceptionHandler(ShortUrlNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            ShortUrlNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(HttpStatus.NOT_FOUND, "Short URL not found",
                "The short URL does not exist or has expired", request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> handlePersistenceFailure(
            DataAccessException exception,
            HttpServletRequest request
    ) {
        LOGGER.error("Persistence failure for request {} ({})",
                request.getRequestURI(), exception.getClass().getSimpleName());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Request failed",
                "The request could not be completed", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedFailure(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unexpected failure for request {} ({})",
                request.getRequestURI(), exception.getClass().getSimpleName());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Request failed",
                "The request could not be completed", request);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(status).body(problem);
    }
}

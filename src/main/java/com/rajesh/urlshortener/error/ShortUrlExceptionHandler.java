package com.rajesh.urlshortener.error;

import com.rajesh.urlshortener.service.ShortCodeGenerationException;
import jakarta.servlet.http.HttpServletRequest;
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

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> handlePersistenceFailure(
            DataAccessException exception,
            HttpServletRequest request
    ) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Request failed",
                "The short URL could not be created", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedFailure(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Request failed",
                "The short URL could not be created", request);
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

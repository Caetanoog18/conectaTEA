package com.github.caetanoog18.conectatea.medicaldocument.api;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = MedicalDocumentController.class)
public class MedicalDocumentExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleStatus(ResponseStatusException exception) {
        String detail = exception.getReason() == null
                ? "Não foi possível processar o laudo."
                : exception.getReason();

        var problem = ProblemDetail.forStatusAndDetail(
                exception.getStatusCode(),
                detail
        );

        return ResponseEntity.status(exception.getStatusCode())
                .cacheControl(CacheControl.noStore())
                .body(problem);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleSize(MaxUploadSizeExceededException exception) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "O limite é de 10 MiB por arquivo."
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .cacheControl(CacheControl.noStore())
                .body(problem);
    }
}
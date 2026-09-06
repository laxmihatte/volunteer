package org.sfa.volunteer.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class IdentityDocumentException extends RuntimeException {
    private final SaayamStatusCode statusCode;
    private final HttpStatus httpStatus;
    private final String reason;
}
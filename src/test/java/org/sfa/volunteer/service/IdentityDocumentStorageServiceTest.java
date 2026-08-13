package org.sfa.volunteer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ExtendWith(MockitoExtension.class)
class IdentityDocumentStorageServiceTest {

    @Mock
    private VolunteerService volunteerService;
    @Mock
    private S3Client s3ClientUs;
    @Mock
    private S3Client s3ClientEu;

    private IdentityDocumentStorageService service;

    private String validBase64;
    private LocalDate futureDate;

    @BeforeEach
    void setUp() {
        service = new IdentityDocumentStorageService(volunteerService, s3ClientUs, s3ClientEu);

        ReflectionTestUtils.setField(service, "maxBytes", 2097152L);
        ReflectionTestUtils.setField(service, "allowedMimeCsv", "image/jpeg,image/png,application/pdf");
        ReflectionTestUtils.setField(service, "keyPattern", "users/%s/identity/slot%d");
        ReflectionTestUtils.setField(service, "euBucket", "test-bucket-eu");
        ReflectionTestUtils.setField(service, "usBucket", "test-bucket-us");

        byte[] pngBytes = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,  // PNG signature
             0x00, 0x00, 0x00, 0x00                                    // padding to reach 12 bytes
        };
        validBase64 = Base64.getEncoder().encodeToString(pngBytes);
        futureDate = LocalDate.now().plusDays(90);
    }

    @Test
    void rejectsNullExpiry() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.uploadBase64("SID-001", 1, "passport.png", validBase64, null, "us-east-1"));
        assertEquals(BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void rejectsPastExpiry() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.uploadBase64("SID-001", 1, "passport.png", validBase64,
                        LocalDate.now().minusDays(1), "us-east-1"));
        assertEquals(BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void rejectsUnsupportedFileType() {
        byte[] gifBytes = new byte[]{
            'G', 'I', 'F', '8', '9', 'a',
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00   // pad past the 12-byte minimum
        };
        String badBase64 = Base64.getEncoder().encodeToString(gifBytes);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.uploadBase64("SID-001", 1, "sneaky.png", badBase64, futureDate, "us-east-1"));
        assertEquals(BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void rejectsBlankDocumentName() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.uploadBase64("SID-001", 1, "   ", validBase64, futureDate, "us-east-1"));
        assertEquals(BAD_REQUEST, ex.getStatusCode());
    }
}
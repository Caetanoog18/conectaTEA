package com.github.caetanoog18.conectatea.medicaldocument;

import com.github.caetanoog18.conectatea.medicaldocument.application.PdfUploadValidator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfUploadValidatorTest {
    private final PdfUploadValidator validator = new PdfUploadValidator();

    @Test
    void shouldParseActualPdfBytesEvenWithoutTrustedMimeMetadata() throws Exception {
        byte[] pdf = MedicalDocumentIntegrationTest.validPdf();

        var file = new MockMultipartFile(
                "file",
                "file.pdf",
                "application/octet-stream",
                pdf
        );

        assertThat(validator.validate(file)).isEqualTo(pdf);
    }

    @Test
    void shouldRejectNullFile() {
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldRejectOnlyThePdfSignatureWithoutAValidDocument() {
        var file = new MockMultipartFile("file", "%PDF-1.4\n".getBytes());

        assertThatThrownBy(() -> validator.validate(file)).isInstanceOf(ResponseStatusException.class);
    }
}
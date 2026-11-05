package com.github.caetanoog18.conectatea.report;

import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.observation.api.dto.ObservationResponse;
import com.github.caetanoog18.conectatea.report.api.dto.StudentReportResponse;
import com.github.caetanoog18.conectatea.report.infrastructure.StudentReportPdfRenderer;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentReportPdfRendererTest {
    private final StudentReportPdfRenderer renderer = new StudentReportPdfRenderer();

    @Test
    void shouldRenderAccentsAndPreserveLiteralText() throws Exception {
        var report = sampleReport(
                1,
                "João participou da atividade de educação. " + "<b>Texto literal</b>");

        byte[] bytes = renderer.render(report);

        try (PdfReader reader = new PdfReader(bytes)) {
            assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(1);

            String text = extractAll(reader);

            assertThat(text)
                    .contains("João")
                    .contains("educação")
                    .contains("<b>Texto literal</b>")
                    .contains(report.reportId().toString());
        }

        Files.createDirectories(Path.of("target"));
        Files.write(Path.of("target/student-report-preview.pdf"), bytes);
    }

    @Test
    void shouldPaginateWithoutLosingLastObservation() throws Exception {
        var report = sampleReport(
                20,
                "Atividade pedagógica registrada para teste. "
                        .repeat(40));

        byte[] bytes = renderer.render(report);

        try (PdfReader reader = new PdfReader(bytes)) {
            assertThat(reader.getNumberOfPages()).isGreaterThan(1);

            String text = extractAll(reader);

            assertThat(text)
                    .contains("Observação 1")
                    .contains("Observação 20")
                    .contains("Página");
        }

        Files.createDirectories(Path.of("target"));
        Files.write(Path.of("target/student-report-long-preview.pdf"), bytes);
    }

    @Test
    void shouldRenderEmptyReport() throws Exception {
        byte[] bytes = renderer.render(sampleReport(0, ""));

        try (PdfReader reader = new PdfReader(bytes)) {
            assertThat(extractAll(reader)).contains("Nenhuma observação encontrada");
        }
    }

    @Test
    void unsupportedCharacterShouldNotBeSilentlyRemoved() {
        var report = sampleReport(1, "Teste \uD83D\uDE00");

        assertThatThrownBy(() -> renderer.render(report))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(
                                exception.getStatusCode().value())
                                .isEqualTo(422));
    }

    private static String extractAll(PdfReader reader) throws Exception {
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        StringBuilder text = new StringBuilder();

        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(extractor.getTextFromPage(page)).append('\n');
        }

        return text.toString();
    }

    private static StudentReportResponse sampleReport(int count, String content) {
        UUID studentId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();

        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");

        List<ObservationResponse> observations =
                IntStream.rangeClosed(1, count)
                        .mapToObj(number -> new ObservationResponse(
                                UUID.randomUUID(),
                                studentId,
                                professionalId,
                                "Maria da Silva",
                                ConsentPurpose.EDUCATIONAL_SUPPORT,
                                "Observação " + number,
                                content,
                                from.plusSeconds(number),
                                from.plusSeconds(number + 60)
                        ))
                        .toList();

        return new StudentReportResponse(
                UUID.randomUUID(),
                to,
                new StudentReportResponse.StudentSummary(
                        studentId,
                        "João Pedro - estudante fictício",
                        null,
                        "PDF-TEST-001",
                        2026,
                        "5º ano",
                        "Turma B"
                ),
                new StudentReportResponse.RequesterSummary(professionalId, "Maria da Silva"),
                ConsentPurpose.EDUCATIONAL_SUPPORT,
                from,
                to,
                observations.size(),
                observations
        );
    }
}
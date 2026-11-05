package com.github.caetanoog18.conectatea.report.infrastructure;

import com.github.caetanoog18.conectatea.consent.domain.ConsentPurpose;
import com.github.caetanoog18.conectatea.report.api.dto.StudentReportResponse;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class StudentReportPdfRenderer {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    public byte[] render(StudentReportResponse report) {
        Document document = new Document(
                PageSize.A4,
                48,
                48,
                48,
                60
        );

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try {
                BaseFont regular = BaseFont.createFont(
                        "liberation/LiberationSans-Regular.ttf",
                        BaseFont.IDENTITY_H,
                        BaseFont.EMBEDDED
                );

                BaseFont bold = BaseFont.createFont(
                        "liberation/LiberationSans-Bold.ttf",
                        BaseFont.IDENTITY_H,
                        BaseFont.EMBEDDED
                );

                Font titleFont = new Font(bold, 18);
                Font headingFont = new Font(bold, 12);
                Font bodyFont = new Font(regular, 11);
                Font smallFont = new Font(regular, 9);
                Font footerFont = new Font(regular, 8);

                PdfWriter writer = PdfWriter.getInstance(document, output);

                writer.setPageEvent(new ReportFooter(report.reportId(), footerFont));

                document.addTitle("ConectaTEA - Relatório de acompanhamento");
                document.addCreator("ConectaTEA API");
                document.open();

                add(document, "ConectaTEA", titleFont);
                add(document, "Relatório de acompanhamento", headingFont);
                add(document, "USO RESTRITO", smallFont);

                add(
                        document,
                        "Identificador: " + report.reportId(),
                        smallFont
                );

                add(
                        document,
                        "Gerado em: " + format(report.generatedAt()),
                        smallFont
                );

                add(document, "Estudante", headingFont);

                var student = report.student();

                add(document, "Nome: " + student.fullName(), bodyFont);

                if (student.preferredName() != null && !student.preferredName().isBlank()) {
                    add(
                            document,
                            "Nome de preferência: " + student.preferredName(),
                            bodyFont
                    );
                }

                add(
                        document,
                        "Matrícula: " + student.enrollmentNumber(),
                        bodyFont
                );

                add(
                        document,
                        "Ano letivo: " + student.schoolYear()
                                + " | Ano/série: " + student.gradeLevel()
                                + " | Turma: " + student.className(),
                        bodyFont
                );

                add(
                        document,
                        "Solicitado por: " + report.generatedBy().fullName(),
                        bodyFont
                );

                add(
                        document,
                        "Finalidade: " + purposeLabel(report.purpose()),
                        bodyFont
                );

                add(
                        document,
                        "Início incluído: " + format(report.from()),
                        bodyFont
                );

                add(
                        document,
                        "Fim excluído: " + format(report.to()),
                        bodyFont
                );

                add(
                        document,
                        "Total de observações: " + report.totalObservations(),
                        bodyFont
                );

                add(document, "Observações", headingFont);

                if (report.observations().isEmpty()) {
                    add(
                            document,
                            "Nenhuma observação encontrada no período autorizado.",
                            bodyFont
                    );
                }

                int number = 1;

                for (var observation : report.observations()) {
                    add(
                            document,
                            number++ + ". " + observation.title(),
                            headingFont
                    );

                    add(
                            document,
                            "Autor: " + observation.authorName(),
                            smallFont
                    );

                    add(
                            document,
                            "Ocorrência: " + format(observation.occurredAt()) + " | Registro: "
                                    + format(observation.createdAt()),
                            smallFont
                    );

                    add(
                            document,
                            "Identificador da observação: " + observation.id(),
                            smallFont
                    );

                    add(document, observation.content(), bodyFont);
                }

                add(
                        document,
                        "Compilação dos registros cadastrados. " + "Este documento não é um laudo clínico.",
                        smallFont
                );
            } finally {
                document.close();
            }

            return output.toByteArray();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "PDF generation failed",
                    exception
            );
        }
    }

    private static void add(Document document, String value, Font font) {
        String text = Normalizer.normalize(value, Normalizer.Form.NFC)
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\t", "    ");

        boolean unsupported = text.codePoints()
                .anyMatch(codePoint -> codePoint != '\n' && !font.getBaseFont().charExists(codePoint));

        if (unsupported) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(422),
                    "Report contains characters unsupported by the PDF font");
        }

        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setLeading(0, 1.35f);
        paragraph.setSpacingAfter(8);
        paragraph.setAlignment(Element.ALIGN_LEFT);

        document.add(paragraph);
    }

    private static String format(Instant value) {
        return DATE_TIME.format(value);
    }

    private static String purposeLabel(ConsentPurpose purpose) {
        return switch (purpose) {
            case EDUCATIONAL_SUPPORT -> "Apoio educacional";
            case MULTIPROFESSIONAL_MONITORING -> "Acompanhamento multiprofissional";
            default -> throw new IllegalArgumentException("Unsupported report purpose");
        };
    }

    private static final class ReportFooter extends PdfPageEventHelper {

        private final UUID reportId;
        private final Font font;

        private ReportFooter(UUID reportId, Font font) {
            this.reportId = reportId;
            this.font = font;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_LEFT,
                    new Phrase("ConectaTEA | " + reportId, font),
                    document.left(),
                    28,
                    0
            );

            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(), font),
                    document.right(),
                    28,
                    0
            );
        }
    }
}
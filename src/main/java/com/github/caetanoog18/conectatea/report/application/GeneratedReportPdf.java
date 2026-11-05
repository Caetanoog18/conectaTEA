package com.github.caetanoog18.conectatea.report.application;

import java.util.UUID;

public record GeneratedReportPdf(
        UUID reportId,
        byte[] content
) {
}
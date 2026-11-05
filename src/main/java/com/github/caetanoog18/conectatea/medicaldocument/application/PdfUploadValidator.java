package com.github.caetanoog18.conectatea.medicaldocument.application;

import org.openpdf.text.pdf.PdfReader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PdfUploadValidator {
    public static final int MAX_BYTES = 10 * 1024 * 1024;

    public byte[] validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalid("Selecione um arquivo PDF.");
        }

        if (file.getSize() > MAX_BYTES) {
            throw tooLarge();
        }

        byte[] bytes;

        try (var input = file.getInputStream()) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        } catch (IOException exception) {
            throw invalid("Não foi possível ler o arquivo enviado.");
        }

        if (bytes.length > MAX_BYTES) {
            throw tooLarge();
        }

        if (
                bytes.length < 5 ||
                        !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")
        ) {
            throw invalid("O arquivo deve ser um PDF válido.");
        }

        PdfReader reader = null;

        try {
            reader = new PdfReader(bytes);

            if (reader.isEncrypted() || reader.getNumberOfPages() < 1) {
                throw invalid("Envie um PDF com páginas e sem proteção por senha.");
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid("O PDF está inválido, corrompido ou protegido por senha.");
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return bytes;
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException tooLarge() {
        return new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "O limite é de 10 MiB por arquivo.");
    }
}
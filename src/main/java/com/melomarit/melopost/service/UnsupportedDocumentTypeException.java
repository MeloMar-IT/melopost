package com.melomarit.melopost.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Thrown when an uploaded document has a file type that is not on the allow-list (HTTP 400). */
public class UnsupportedDocumentTypeException extends ResponseStatusException {
    public static final String MESSAGE =
            "This file type is not allowed. Allowed types: PDF, DOCX, XLSX, PNG, JPEG, TXT, CSV.";

    public UnsupportedDocumentTypeException() {
        super(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}

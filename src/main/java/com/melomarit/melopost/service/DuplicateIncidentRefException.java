package com.melomarit.melopost.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateIncidentRefException extends RuntimeException {
    public DuplicateIncidentRefException(String incidentRef) {
        super("A postmortem with this Incident Ref already exists: " + incidentRef);
    }
}

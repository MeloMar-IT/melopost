package com.melomarit.melopost.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The current user may not perform the requested action on a postmortem. Maps to HTTP 403. */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class PostmortemAccessDeniedException extends AccessDeniedException {
    public PostmortemAccessDeniedException(String message) {
        super(message);
    }
}

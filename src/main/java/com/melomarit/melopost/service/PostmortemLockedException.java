package com.melomarit.melopost.service;

import java.util.UUID;

/** A non-admin tried to change or delete a Published postmortem. */
public class PostmortemLockedException extends PostmortemAccessDeniedException {
    private final UUID postmortemId;

    public PostmortemLockedException(UUID postmortemId) {
        super("Postmortem is Published and locked");
        this.postmortemId = postmortemId;
    }

    public UUID getPostmortemId() {
        return postmortemId;
    }
}

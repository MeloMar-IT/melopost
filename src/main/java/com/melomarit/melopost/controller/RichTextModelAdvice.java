package com.melomarit.melopost.controller;

import com.melomarit.melopost.service.RichTextSanitizer;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the rich text sanitizer to templates as {@code richText}. Thymeleaf 3.1
 * forbids {@code T(...)} inside {@code th:utext}, so templates call
 * {@code ${richText.sanitize(value)}} instead.
 */
@ControllerAdvice
public class RichTextModelAdvice {

    /** Template-facing wrapper around the static sanitizer. */
    public static final class RichText {
        public String sanitize(String html) {
            return RichTextSanitizer.sanitize(html);
        }
    }

    private static final RichText RICH_TEXT = new RichText();

    @ModelAttribute("richText")
    public RichText richText() {
        return RICH_TEXT;
    }
}

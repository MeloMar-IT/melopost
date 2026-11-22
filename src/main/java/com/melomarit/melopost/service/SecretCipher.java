package com.melomarit.melopost.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM for stored data-source tokens. Format: {@code enc:v1:} + Base64(12-byte IV, ciphertext and tag).
 * Neither the key nor any value appears in log lines or exception messages.
 */
@Component
public class SecretCipher {
    private static final Logger log = LoggerFactory.getLogger(SecretCipher.class);

    public static final String PREFIX = "enc:v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BYTES = 32;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public SecretCipher(@Value("${melopost.datasource.key:}") String base64Key, Environment environment) {
        this(base64Key, environment.acceptsProfiles(Profiles.of("prod | acc")));
    }

    SecretCipher(String base64Key, boolean keyRequired) {
        if (base64Key == null || base64Key.isBlank()) {
            if (keyRequired) {
                throw new IllegalStateException("MELOPOST_DATASOURCE_KEY must be set in the prod and acc profiles");
            }
            log.warn("[DATASOURCE_KEY] MELOPOST_DATASOURCE_KEY is not set: data-source tokens are stored unencrypted.");
            this.key = null;
            return;
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("MELOPOST_DATASOURCE_KEY must be a Base64-encoded 32-byte key");
        }
        if (raw.length != KEY_BYTES) {
            throw new IllegalStateException("MELOPOST_DATASOURCE_KEY must be a Base64-encoded 32-byte key");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public boolean isEnabled() {
        return key != null;
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty() || key == null) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[IV_BYTES + ct.length];
            System.arraycopy(iv, 0, out, 0, IV_BYTES);
            System.arraycopy(ct, 0, out, IV_BYTES, ct.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encrypting a data-source token failed");
        }
    }

    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            return stored;
        }
        if (key == null) {
            throw new IllegalStateException("A stored token is encrypted but MELOPOST_DATASOURCE_KEY is not set");
        }
        try {
            byte[] in = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            if (in.length < IV_BYTES + TAG_BITS / 8) {
                throw new IllegalStateException("Stored token is malformed");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, in, 0, IV_BYTES));
            return new String(cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new IllegalStateException("Stored token could not be decrypted with the current key");
        }
    }
}

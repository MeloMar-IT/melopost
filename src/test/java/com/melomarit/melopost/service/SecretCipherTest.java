package com.melomarit.melopost.service;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class SecretCipherTest {

    static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    static final String OTHER_KEY = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    private final SecretCipher cipher = new SecretCipher(KEY, false);

    @Test
    void roundTrip_returnsPlaintextAndUsesPrefix() {
        String enc = cipher.encrypt("secret-123");
        assertTrue(enc.startsWith("enc:v1:"));
        assertFalse(enc.contains("secret-123"));
        assertEquals("secret-123", cipher.decrypt(enc));
    }

    @Test
    void encrypt_usesFreshIv() {
        assertNotEquals(cipher.encrypt("same"), cipher.encrypt("same"));
    }

    @Test
    void decrypt_returnsValueWithoutPrefixUnchanged() {
        assertEquals("plain", cipher.decrypt("plain"));
    }

    @Test
    void nullAndEmptyPassThrough() {
        assertNull(cipher.encrypt(null));
        assertEquals("", cipher.encrypt(""));
        assertNull(cipher.decrypt(null));
    }

    @Test
    void decrypt_rejectsTamperedCiphertext() {
        String enc = cipher.encrypt("secret-123");
        byte[] raw = Base64.getDecoder().decode(enc.substring(SecretCipher.PREFIX.length()));
        raw[raw.length - 1] ^= 1;
        String tampered = SecretCipher.PREFIX + Base64.getEncoder().encodeToString(raw);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> cipher.decrypt(tampered));
        assertFalse(e.getMessage().contains("secret-123"));
        assertFalse(e.getMessage().contains(tampered));
    }

    @Test
    void decrypt_rejectsMalformedBase64AndShortInput() {
        assertThrows(IllegalStateException.class, () -> cipher.decrypt("enc:v1:%%%"));
        assertThrows(IllegalStateException.class, () -> cipher.decrypt("enc:v1:garbage"));
    }

    @Test
    void decrypt_rejectsCiphertextFromAnotherKey() {
        String enc = new SecretCipher(OTHER_KEY, false).encrypt("secret-123");
        assertThrows(IllegalStateException.class, () -> cipher.decrypt(enc));
    }

    @Test
    void blankKey_failsWhenRequired() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> new SecretCipher("", true));
        assertTrue(e.getMessage().contains("MELOPOST_DATASOURCE_KEY must be set"));
    }

    @Test
    void blankKey_disablesEncryptionWhenNotRequired() {
        SecretCipher disabled = new SecretCipher(" ", false);
        assertFalse(disabled.isEnabled());
        assertEquals("tok", disabled.encrypt("tok"));
        assertThrows(IllegalStateException.class, () -> disabled.decrypt("enc:v1:abcd"));
    }

    @Test
    void invalidKey_failsWithoutLeakingIt() {
        String notBase64 = "not base64 !!";
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        for (String bad : new String[] {notBase64, shortKey}) {
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> new SecretCipher(bad, false));
            assertTrue(e.getMessage().contains("Base64-encoded 32-byte key"));
            assertFalse(e.getMessage().contains(bad));
        }
    }
}

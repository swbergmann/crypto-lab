package com.example.cryptolab.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

/**
 * LAB VULNERABILITY 2: anyone who can read this repository or container image
 * obtains the key needed to decrypt customer data.
 */
@Service
public class HardCodedKeyService {

    private static final byte[] CUSTOMER_DATA_KEY =
        "0123456789ABCDEF0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKey key = new SecretKeySpec(CUSTOMER_DATA_KEY, "AES");

    public EncryptionResult encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] payload = ByteBuffer.allocate(iv.length + ciphertext.length)
                .put(iv)
                .put(ciphertext)
                .array();

            return new EncryptionResult(
                TRANSFORMATION,
                Base64.getEncoder().encodeToString(payload),
                "Hard-coded constant in HardCodedKeyService.java"
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Encryption failed", exception);
        }
    }

    public String keySource() {
        return "Hard-coded constant in source code";
    }

    public record EncryptionResult(String algorithm, String ciphertext, String keySource) {}
}


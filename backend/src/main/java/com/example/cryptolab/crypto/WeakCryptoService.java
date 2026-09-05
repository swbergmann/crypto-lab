package com.example.cryptolab.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.springframework.stereotype.Service;

/** Secure solution for lab 1: authenticated encryption with a fresh IV. */
@Service
public class WeakCryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    public WeakCryptoService(LabKeyProvider keyProvider) {
        this.key = keyProvider.currentKey();
    }

    public EncryptionPair encryptTwice(String plaintext) {
        String first = encrypt(plaintext);
        String second = encrypt(plaintext);
        return new EncryptionPair(TRANSFORMATION, first, second, first.equals(second));
    }

    private String encrypt(String plaintext) {
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
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Encryption failed", exception);
        }
    }

    public String algorithm() {
        return TRANSFORMATION;
    }

    public record EncryptionPair(
        String algorithm,
        String firstCiphertext,
        String secondCiphertext,
        boolean identical
    ) {}
}


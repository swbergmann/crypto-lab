package com.example.cryptolab.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

/**
 * LAB VULNERABILITY 1: AES in ECB mode is deterministic and does not provide
 * authenticated encryption. Replace this class using the first lab exercise.
 */
@Service
public class WeakCryptoService {

    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    private final SecretKey key;

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
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ciphertext);
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


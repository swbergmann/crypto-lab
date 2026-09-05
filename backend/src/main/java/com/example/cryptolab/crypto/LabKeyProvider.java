package com.example.cryptolab.crypto;

import java.security.GeneralSecurityException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

/**
 * Generates an ephemeral key for this local lab process. Production ECS tasks
 * should replace this provider with AWS KMS envelope encryption.
 */
@Component
public class LabKeyProvider {

    private final SecretKey key;

    public LabKeyProvider() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            this.key = generator.generateKey();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to create the lab encryption key", exception);
        }
    }

    public SecretKey currentKey() {
        return key;
    }

    public String description() {
        return "Ephemeral key generated at application startup";
    }
}


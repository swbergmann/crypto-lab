package com.example.cryptolab.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CryptoServiceTest {

    @Test
    void weakLabProducesTwoCiphertextsForComparison() {
        WeakCryptoService service = new WeakCryptoService(new LabKeyProvider());

        WeakCryptoService.EncryptionPair result = service.encryptTwice("same customer value");

        assertThat(result.firstCiphertext()).isNotBlank();
        assertThat(result.secondCiphertext()).isNotBlank();
    }
}

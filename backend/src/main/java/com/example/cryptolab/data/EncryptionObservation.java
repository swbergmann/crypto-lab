package com.example.cryptolab.data;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "LAB_ENCRYPTION_OBSERVATION")
public class EncryptionObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String labName;

    @Column(nullable = false, length = 80)
    private String algorithm;

    @Lob
    @Column(nullable = false)
    private String ciphertext;

    @Column(nullable = false)
    private Instant createdAt;

    protected EncryptionObservation() {}

    public EncryptionObservation(String labName, String algorithm, String ciphertext) {
        this.labName = labName;
        this.algorithm = algorithm;
        this.ciphertext = ciphertext;
    }

    @PrePersist
    void assignCreationTime() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getLabName() {
        return labName;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}


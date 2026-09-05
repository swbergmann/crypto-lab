package com.example.cryptolab.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EncryptionObservationRepository
    extends JpaRepository<EncryptionObservation, Long> {

    List<EncryptionObservation> findTop12ByOrderByCreatedAtDesc();
}


package com.example.cryptolab.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import com.example.cryptolab.crypto.HardCodedKeyService;
import com.example.cryptolab.crypto.LabKeyProvider;
import com.example.cryptolab.crypto.WeakCryptoService;
import com.example.cryptolab.data.EncryptionObservation;
import com.example.cryptolab.data.EncryptionObservationRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/labs")
public class LabController {

    private final WeakCryptoService weakCryptoService;
    private final HardCodedKeyService hardCodedKeyService;
    private final LabKeyProvider labKeyProvider;
    private final EncryptionObservationRepository observationRepository;
    private final DataSource dataSource;

    public LabController(
        WeakCryptoService weakCryptoService,
        HardCodedKeyService hardCodedKeyService,
        LabKeyProvider labKeyProvider,
        EncryptionObservationRepository observationRepository,
        DataSource dataSource
    ) {
        this.weakCryptoService = weakCryptoService;
        this.hardCodedKeyService = hardCodedKeyService;
        this.labKeyProvider = labKeyProvider;
        this.observationRepository = observationRepository;
        this.dataSource = dataSource;
    }

    @PostMapping("/weak/encrypt-twice")
    public ResponseEntity<WeakLabResponse> weakEncrypt(@Valid @RequestBody EncryptRequest request) {
        WeakCryptoService.EncryptionPair result = weakCryptoService.encryptTwice(request.value());
        EncryptionObservation saved = observationRepository.save(
            new EncryptionObservation("weak-algorithm", result.algorithm(), result.firstCiphertext())
        );

        return noStore(new WeakLabResponse(
            result.algorithm(),
            result.firstCiphertext(),
            result.secondCiphertext(),
            result.identical(),
            saved.getId()
        ));
    }

    @PostMapping("/hardcoded/encrypt")
    public ResponseEntity<KeyLabResponse> hardcodedEncrypt(@Valid @RequestBody EncryptRequest request) {
        HardCodedKeyService.EncryptionResult result = hardCodedKeyService.encrypt(request.value());
        EncryptionObservation saved = observationRepository.save(
            new EncryptionObservation("hard-coded-key", result.algorithm(), result.ciphertext())
        );

        return noStore(new KeyLabResponse(
            result.algorithm(),
            result.ciphertext(),
            result.keySource(),
            saved.getId()
        ));
    }

    @GetMapping("/observations")
    public ResponseEntity<List<ObservationResponse>> observations() {
        List<ObservationResponse> observations = observationRepository
            .findTop12ByOrderByCreatedAtDesc()
            .stream()
            .map(item -> new ObservationResponse(
                item.getId(),
                item.getLabName(),
                item.getAlgorithm(),
                item.getCiphertext(),
                item.getCreatedAt().toString()
            ))
            .toList();
        return noStore(observations);
    }

    @GetMapping("/status")
    public ResponseEntity<LabStatusResponse> status(HttpServletRequest request) {
        return noStore(new LabStatusResponse(
            weakCryptoService.algorithm(),
            hardCodedKeyService.keySource(),
            labKeyProvider.description(),
            request.getScheme(),
            request.isSecure(),
            databaseProduct()
        ));
    }

    private String databaseProduct() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName()
                + " "
                + connection.getMetaData().getDatabaseProductVersion();
        } catch (SQLException exception) {
            return "Oracle connection unavailable";
        }
    }

    private static <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(body);
    }

    public record EncryptRequest(
        @NotBlank(message = "Enter a value to encrypt")
        @Size(max = 256, message = "Use 256 characters or fewer")
        String value
    ) {}

    public record WeakLabResponse(
        String algorithm,
        String firstCiphertext,
        String secondCiphertext,
        boolean identical,
        Long observationId
    ) {}

    public record KeyLabResponse(
        String algorithm,
        String ciphertext,
        String keySource,
        Long observationId
    ) {}

    public record ObservationResponse(
        Long id,
        String labName,
        String algorithm,
        String ciphertext,
        String createdAt
    ) {}

    public record LabStatusResponse(
        String weakLabAlgorithm,
        String hardcodedLabKeySource,
        String secureLabKeySource,
        String requestScheme,
        boolean secureTransport,
        String database
    ) {}
}


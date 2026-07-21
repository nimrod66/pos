package com.example.pos.compliance.initialization;

import com.example.pos.compliance.config.ComplianceConfiguration;
import com.example.pos.compliance.initialization.model.DeviceRegistration;
import com.example.pos.compliance.initialization.repository.DeviceRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class EtimsInitializer {

    private static final Logger log = LoggerFactory.getLogger(EtimsInitializer.class);

    private final DeviceRegistrationRepository registrationRepo;
    private final CommunicationKeyManager keyManager;
    private final ComplianceConfiguration config;

    public EtimsInitializer(DeviceRegistrationRepository registrationRepo,
                            CommunicationKeyManager keyManager,
                            ComplianceConfiguration config) {
        this.registrationRepo = registrationRepo;
        this.keyManager = keyManager;
        this.config = config;
    }

    public DeviceRegistration initialize(String deviceSerial, String kraPin, String cmcKey) {
        registrationRepo.findByDeviceSerial(deviceSerial).ifPresent(existing -> {
            throw new IllegalStateException("Device " + deviceSerial + " is already registered");
        });

        String encrypted = keyManager.encrypt(cmcKey);

        DeviceRegistration reg = DeviceRegistration.builder()
                .deviceSerial(deviceSerial)
                .kraPin(kraPin)
                .encryptedCmcKey(encrypted)
                .registrationStatus("INITIALIZED")
                .registeredAt(LocalDateTime.now())
                .environment(config.getMode().name())
                .build();

        reg = registrationRepo.save(reg);
        log.info("Device {} initialized for KRA PIN {}", deviceSerial, kraPin);
        return reg;
    }

    public String getDecryptedKey(String deviceSerial) {
        DeviceRegistration reg = registrationRepo.findByDeviceSerial(deviceSerial)
                .orElseThrow(() -> new IllegalStateException("Device " + deviceSerial + " not registered"));
        return keyManager.decrypt(reg.getEncryptedCmcKey());
    }

    public String getMaskedKey(String deviceSerial) {
        DeviceRegistration reg = registrationRepo.findByDeviceSerial(deviceSerial)
                .orElseThrow(() -> new IllegalStateException("Device " + deviceSerial + " not registered"));
        String raw = keyManager.decrypt(reg.getEncryptedCmcKey());
        return keyManager.maskKey(raw);
    }

    public void renewKey(String deviceSerial, String newCmcKey) {
        DeviceRegistration reg = registrationRepo.findByDeviceSerial(deviceSerial)
                .orElseThrow(() -> new IllegalStateException("Device " + deviceSerial + " not registered"));
        reg.setEncryptedCmcKey(keyManager.encrypt(newCmcKey));
        reg.setLastRenewedAt(LocalDateTime.now());
        registrationRepo.save(reg);
        log.info("Communication key renewed for device {}", deviceSerial);
    }
}

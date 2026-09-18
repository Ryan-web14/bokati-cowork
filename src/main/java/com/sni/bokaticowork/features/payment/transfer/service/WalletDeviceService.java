package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.control.service.WalletRiskFlagService;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.transfer.model.WalletDevice;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Le journal des appareils d'un portefeuille.
 *
 * <p>On note chaque appareil qui opere, on dit si celui-ci est nouveau, et on laisse le titulaire
 * en revoquer. Rien de plus · la decision de ce qu'on fait d'un appareil inconnu appartient a la
 * surveillance, pas au journal.</p>
 */
@Service
@RequiredArgsConstructor
public class WalletDeviceService {

    private final WalletDeviceRepository deviceRepository;
    private final WalletNotifier notifier;
    private final WalletRiskFlagService flagService;

    /** Ce que le journal sait de l'appareil qui opere. */
    public record DeviceVerdict(boolean known, boolean revoked, boolean firstUse, int useCount) {

        static DeviceVerdict anonymous() {
            return new DeviceVerdict(false, false, false, 0);
        }
    }

    /**
     * Note le passage d'un appareil, et dit s'il est nouveau.
     *
     * <p>Un appareil revoque n'est pas reactive par un nouveau passage : c'est precisement le cas du
     * telephone perdu dont quelqu'un se sert encore. Il est note, signale, et reste revoque.</p>
     */
    @Transactional
    public DeviceVerdict touch(WalletAccount wallet, String deviceId, String ipAddress) {
        if (!StringUtils.hasText(deviceId)) {
            return DeviceVerdict.anonymous();
        }
        Instant now = Instant.now();
        Optional<WalletDevice> existing = deviceRepository.findByWallet_IdAndDeviceId(wallet.getId(), deviceId.trim());
        if (existing.isEmpty()) {
            deviceRepository.save(WalletDevice.builder()
                    .wallet(wallet)
                    .deviceId(deviceId.trim())
                    .firstSeenAt(now)
                    .lastSeenAt(now)
                    .lastIpAddress(ipAddress)
                    .useCount(1)
                    .build());
            notifier.securityEvent(wallet, "WALLET_NEW_DEVICE",
                    "Nouvel appareil sur votre portefeuille",
                    Map.of("deviceId", deviceId.trim(), "ipAddress", ipAddress == null ? "" : ipAddress));
            return new DeviceVerdict(false, false, true, 1);
        }

        WalletDevice device = existing.get();
        device.setLastSeenAt(now);
        device.setLastIpAddress(ipAddress);
        device.setUseCount((device.getUseCount() == null ? 0 : device.getUseCount()) + 1);
        deviceRepository.save(device);
        if (device.revoked()) {
            notifier.securityEvent(wallet, "WALLET_REVOKED_DEVICE_USED",
                    "Un appareil révoqué a tenté d'opérer sur votre portefeuille",
                    Map.of("deviceId", deviceId.trim(), "ipAddress", ipAddress == null ? "" : ipAddress));
            flagService.raise(wallet, WalletRiskFlag.Type.REVOKED_DEVICE_USED, WalletRiskFlag.Severity.HIGH,
                    "Appareil " + deviceId.trim() + " revoque le " + device.getRevokedAt() + ", depuis " + ipAddress,
                    deviceId.trim());
        }
        return new DeviceVerdict(true, device.revoked(), false, device.getUseCount());
    }

    @Transactional(readOnly = true)
    public List<WalletDevice> list(WalletAccount wallet) {
        return deviceRepository.findByWallet_IdOrderByLastSeenAtDesc(wallet.getId());
    }

    @Transactional
    public WalletDevice rename(WalletAccount wallet, Long deviceId, String label) {
        WalletDevice device = owned(wallet, deviceId);
        device.setLabel(StringUtils.hasText(label) ? label.trim() : null);
        return deviceRepository.save(device);
    }

    @Transactional
    public WalletDevice trust(WalletAccount wallet, Long deviceId, boolean trusted) {
        WalletDevice device = owned(wallet, deviceId);
        device.setTrusted(trusted);
        return deviceRepository.save(device);
    }

    /** Revoque · l'appareil reste au journal, marque, pour qu'un nouveau passage soit signale. */
    @Transactional
    public WalletDevice revoke(WalletAccount wallet, Long deviceId) {
        WalletDevice device = owned(wallet, deviceId);
        if (!device.revoked()) {
            device.setRevokedAt(Instant.now());
            device.setTrusted(Boolean.FALSE);
            deviceRepository.save(device);
        }
        return device;
    }

    private WalletDevice owned(WalletAccount wallet, Long deviceId) {
        return deviceRepository.findByIdAndWallet_Id(deviceId, wallet.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Appareil introuvable"));
    }
}

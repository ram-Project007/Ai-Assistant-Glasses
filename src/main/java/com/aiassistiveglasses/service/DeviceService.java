package com.aiassistiveglasses.service;

import com.aiassistiveglasses.dto.request.DeviceRequest;
import com.aiassistiveglasses.dto.response.DeviceResponse;
import com.aiassistiveglasses.entity.Device;
import com.aiassistiveglasses.entity.DeviceStatus;
import com.aiassistiveglasses.entity.User;
import com.aiassistiveglasses.exception.DuplicateResourceException;
import com.aiassistiveglasses.exception.ResourceNotFoundException;
import com.aiassistiveglasses.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;

    @Transactional
    public DeviceResponse registerDevice(User owner, DeviceRequest request) {
        if (deviceRepository.existsByDeviceIdentifier(request.getDeviceIdentifier())) {
            throw new DuplicateResourceException("A device with this identifier is already registered");
        }

        Device device = Device.builder()
                .deviceName(request.getDeviceName())
                .deviceIdentifier(request.getDeviceIdentifier())
                .deviceType(request.getDeviceType())
                .status(request.getStatus() != null ? request.getStatus() : DeviceStatus.INACTIVE)
                .user(owner)
                .build();

        Device saved = deviceRepository.save(device);
        return DeviceResponse.fromEntity(saved);
    }

    public List<DeviceResponse> getDevicesForUser(User owner) {
        return deviceRepository.findByUser(owner).stream()
                .map(DeviceResponse::fromEntity)
                .toList();
    }

    public DeviceResponse getDeviceForUser(User owner, Long deviceId) {
        Device device = findOwnedDeviceOrThrow(owner, deviceId);
        return DeviceResponse.fromEntity(device);
    }

    @Transactional
    public DeviceResponse updateDevice(User owner, Long deviceId, DeviceRequest request) {
        Device device = findOwnedDeviceOrThrow(owner, deviceId);

        // If the identifier is changing, make sure it doesn't collide with
        // another device (its own current value is fine).
        if (!device.getDeviceIdentifier().equals(request.getDeviceIdentifier())
                && deviceRepository.existsByDeviceIdentifier(request.getDeviceIdentifier())) {
            throw new DuplicateResourceException("A device with this identifier is already registered");
        }

        device.setDeviceName(request.getDeviceName());
        device.setDeviceIdentifier(request.getDeviceIdentifier());
        device.setDeviceType(request.getDeviceType());
        if (request.getStatus() != null) {
            device.setStatus(request.getStatus());
            if (request.getStatus() == DeviceStatus.CONNECTED) {
                device.setLastConnectedAt(java.time.LocalDateTime.now());
            }
        }

        Device saved = deviceRepository.save(device);
        return DeviceResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteDevice(User owner, Long deviceId) {
        Device device = findOwnedDeviceOrThrow(owner, deviceId);
        deviceRepository.delete(device);
    }

    /**
     * Looks a device up scoped to its owner in a single query, so a device
     * belonging to another user is indistinguishable from a non-existent
     * one (404) - this avoids leaking which device IDs exist to other users.
     */
    private Device findOwnedDeviceOrThrow(User owner, Long deviceId) {
        return deviceRepository.findByIdAndUser(deviceId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
    }
}

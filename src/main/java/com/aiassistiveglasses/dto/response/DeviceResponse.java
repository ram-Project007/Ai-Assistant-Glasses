package com.aiassistiveglasses.dto.response;

import com.aiassistiveglasses.entity.Device;
import com.aiassistiveglasses.entity.DeviceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponse {

    private Long id;
    private String deviceName;
    private String deviceIdentifier;
    private String deviceType;
    private DeviceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastConnectedAt;

    public static DeviceResponse fromEntity(Device device) {
        return DeviceResponse.builder()
                .id(device.getId())
                .deviceName(device.getDeviceName())
                .deviceIdentifier(device.getDeviceIdentifier())
                .deviceType(device.getDeviceType())
                .status(device.getStatus())
                .createdAt(device.getCreatedAt())
                .lastConnectedAt(device.getLastConnectedAt())
                .build();
    }
}

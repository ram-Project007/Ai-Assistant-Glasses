package com.aiassistiveglasses.dto.request;

import com.aiassistiveglasses.entity.DeviceStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRequest {

    @NotBlank(message = "Device name is required")
    private String deviceName;

    @NotBlank(message = "Device identifier is required")
    private String deviceIdentifier;

    @NotBlank(message = "Device type is required")
    private String deviceType;

    /**
     * Ignored when registering a device: new devices always begin INACTIVE.
     * Retained for the existing update endpoint; device communication will
     * own connection-state transitions in a later phase.
     */
    private DeviceStatus status;
}

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
     * Optional. Defaults to INACTIVE on creation if not provided.
     * Used on update to change status explicitly.
     */
    private DeviceStatus status;
}

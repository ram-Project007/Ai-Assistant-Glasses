package com.aiassistiveglasses.controller;

import com.aiassistiveglasses.dto.request.DeviceRequest;
import com.aiassistiveglasses.dto.response.ApiResponse;
import com.aiassistiveglasses.dto.response.DeviceResponse;
import com.aiassistiveglasses.entity.User;
import com.aiassistiveglasses.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Tag(name = "Devices", description = "Assistive-glasses device registration and management")
@SecurityRequirement(name = "bearerAuth")
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    @Operation(summary = "Register a new device for the authenticated user")
    public ResponseEntity<ApiResponse<DeviceResponse>> createDevice(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody DeviceRequest request) {
        DeviceResponse device = deviceService.registerDevice(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Device registered successfully", device));
    }

    @GetMapping
    @Operation(summary = "List all devices belonging to the authenticated user")
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> getMyDevices(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success("Devices fetched successfully", deviceService.getDevicesForUser(currentUser)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single device - only if it belongs to the authenticated user")
    public ResponseEntity<ApiResponse<DeviceResponse>> getDevice(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Device fetched successfully", deviceService.getDeviceForUser(currentUser, id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a device - only if it belongs to the authenticated user")
    public ResponseEntity<ApiResponse<DeviceResponse>> updateDevice(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody DeviceRequest request) {
        DeviceResponse updated = deviceService.updateDevice(currentUser, id, request);
        return ResponseEntity.ok(ApiResponse.success("Device updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete/unregister a device - only if it belongs to the authenticated user")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        deviceService.deleteDevice(currentUser, id);
        return ResponseEntity.ok(ApiResponse.success("Device deleted successfully"));
    }
}

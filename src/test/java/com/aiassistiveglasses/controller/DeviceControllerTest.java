package com.aiassistiveglasses.controller;

import com.aiassistiveglasses.dto.request.DeviceRequest;
import com.aiassistiveglasses.dto.request.LoginRequest;
import com.aiassistiveglasses.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userAToken;
    private String userBToken;

    @BeforeEach
    void setUpTwoUsers() throws Exception {
        userAToken = registerAndLogin("deviceUserA" + System.nanoTime() + "@example.com");
        userBToken = registerAndLogin("deviceUserB" + System.nanoTime() + "@example.com");
    }

    private String registerAndLogin(String email) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("Device Owner", email, "9876543210", "Test@123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Test@123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private Long createDeviceAs(String token, String identifier) throws Exception {
        DeviceRequest request = new DeviceRequest("Smart Glasses", identifier, "GLASSES_V1", null);

        String response = mockMvc.perform(post("/api/devices")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    @Test
    void createDeviceSucceeds() throws Exception {
        DeviceRequest request = new DeviceRequest("Smart Glasses", "DEV-CREATE-" + System.nanoTime(), "GLASSES_V1", null);

        mockMvc.perform(post("/api/devices")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.deviceName").value("Smart Glasses"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void createDeviceAlwaysStartsInactive() throws Exception {
        DeviceRequest request = new DeviceRequest(
                "Smart Glasses", "DEV-STATUS-" + System.nanoTime(), "GLASSES_V1",
                com.aiassistiveglasses.entity.DeviceStatus.CONNECTED);

        mockMvc.perform(post("/api/devices")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void getDevicesReturnsOnlyOwnDevices() throws Exception {
        createDeviceAs(userAToken, "DEV-LIST-A-" + System.nanoTime());

        mockMvc.perform(get("/api/devices")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        // A brand-new user (userB) should see zero devices.
        mockMvc.perform(get("/api/devices")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void updateDeviceSucceedsForOwner() throws Exception {
        Long deviceId = createDeviceAs(userAToken, "DEV-UPDATE-" + System.nanoTime());

        DeviceRequest update = new DeviceRequest("Updated Glasses", "DEV-UPDATE-NEW-" + System.nanoTime(),
                "GLASSES_V2", com.aiassistiveglasses.entity.DeviceStatus.ACTIVE);

        mockMvc.perform(put("/api/devices/" + deviceId)
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceName").value("Updated Glasses"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void deleteDeviceSucceedsForOwner() throws Exception {
        Long deviceId = createDeviceAs(userAToken, "DEV-DELETE-" + System.nanoTime());

        mockMvc.perform(delete("/api/devices/" + deviceId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/devices/" + deviceId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotAccessAnotherUsersDevice() throws Exception {
        Long deviceId = createDeviceAs(userAToken, "DEV-CROSSUSER-" + System.nanoTime());

        // userB must not be able to read, update, or delete userA's device.
        mockMvc.perform(get("/api/devices/" + deviceId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        DeviceRequest update = new DeviceRequest("Hijacked", "DEV-HIJACK-" + System.nanoTime(), "GLASSES_V1", null);
        mockMvc.perform(put("/api/devices/" + deviceId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/devices/" + deviceId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());
    }
}

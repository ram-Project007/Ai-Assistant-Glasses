package com.aiassistiveglasses.repository;

import com.aiassistiveglasses.entity.Device;
import com.aiassistiveglasses.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByUser(User user);

    Optional<Device> findByIdAndUser(Long id, User user);

    boolean existsByDeviceIdentifier(String deviceIdentifier);
}

package com.aiassistiveglasses.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the "jwt.*" properties from application.yml so the secret and
 * expiration are configurable per-environment instead of hard-coded.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {

    /**
     * Base64 or plain secret used to sign tokens. Override via the
     * JWT_SECRET environment variable in production - never commit a
     * real secret to source control.
     */
    private String secret;

    /**
     * Token lifetime in milliseconds.
     */
    private long expiration;
}

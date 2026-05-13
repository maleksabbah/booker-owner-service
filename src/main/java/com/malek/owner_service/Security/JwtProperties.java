package com.malek.owner_service.Security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    /** Shared signing secret. Must match user-service exactly. */
    private String secret;

    /** Access-token expiry in seconds. Used for verification only here. */
    private long accessTokenExpirySeconds = 900;     // 15 min

    /** Refresh-token expiry, unused in owner-service. Kept for property parity. */
    private long refreshTokenExpirySeconds = 1209600; // 14 days
}
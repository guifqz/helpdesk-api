package dev.guifqz.helpdesk.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "helpdesk.jwt")
public record JwtProperties(String secret, Duration expiracao) {
}

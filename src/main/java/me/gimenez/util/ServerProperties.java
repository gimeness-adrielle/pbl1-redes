package me.gimenez.util;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "peer")
public record ServerProperties(
        String id,
        String ip
) {}

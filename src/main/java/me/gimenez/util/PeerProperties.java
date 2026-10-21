package me.gimenez.util;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "peer")
public record PeerProperties(Set<String> servers) {
}

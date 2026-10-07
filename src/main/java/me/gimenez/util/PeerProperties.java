package me.gimenez.util;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "peer")
public record PeerProperties(List<String> servers) {
}

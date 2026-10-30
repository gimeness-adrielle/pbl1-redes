package me.gimenez.util;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "peer")
public record PeerProperties(String selfId, Map<String, String> urls) {

    public List<String> peerUrls(){
        return urls.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(selfId))
                .map(Map.Entry::getValue)
                .toList();
    }

    public String getPeerUrl(String peerId){
        return urls.get(peerId);
    }

}

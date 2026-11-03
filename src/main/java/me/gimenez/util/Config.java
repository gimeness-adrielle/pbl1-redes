package me.gimenez.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableConfigurationProperties({PeerProperties.class})
@RequiredArgsConstructor
public class Config implements WebMvcConfigurer {
    protected static final String LAMPORT_HEADER = "X-Lamport-Timestamp";
    private final ClockRequestInterceptor clockRequestInterceptor;
    private final ClockResponseInterceptor clockResponseInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(clockResponseInterceptor)
                .addPathPatterns("/api/rides/segments", "/api/transactions/**", "/api/reservations");
    }

    @Bean
    public ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public RestClient restClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(3));

        return RestClient.builder()
                .requestInterceptor(clockRequestInterceptor)
                .requestFactory(factory)
                .build();
    }

    @Bean
    public ScheduledExecutorService scheduler(){
        return Executors.newScheduledThreadPool(1);
    }

}
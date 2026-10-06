package me.gimenez.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.gimenez.model.User;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserRepository {

    private final ObjectMapper mapper;
    private final Path path = Paths.get("data", "users.json");

    private final Map<UUID, User> users = new ConcurrentHashMap<>();

    public UserRepository(ObjectMapper mapper) {
        this.mapper = mapper;
        loadData();
    }

    private void loadData() {
        try {
            if (Files.exists(path)) {
                List<User> users = mapper.readValue(path.toFile(), new TypeReference<>() {});

                for (User user : users) {
                    this.users.put(user.id(), user);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(User user) throws IOException {
        users.put(user.id(), user);
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), users.values());
    }

    public List<User> getUsers() {
        return new ArrayList<>(users.values());
    }

    public Optional<User> findUserByEmail(String email) {
        return users.values().stream()
                .filter(user -> user.email().equals(email))
                .findFirst();
    }

    public boolean existsByEmail(String email) {
        return users.values().stream().anyMatch(user -> user.email().equals(email));
    }
}

package me.gimenez.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.model.users.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    private final ObjectMapper mapper;
    private final Path path = Paths.get("data", "users.json");

    public UserRepository() {
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    public void save(User user) throws IOException {
        List<User> users;

        if (Files.exists(path)) {
            users = mapper.readValue(
                    path.toFile(),
                    new TypeReference<List<User>>() {
                    }
            );
        } else {
            users = new ArrayList<>();
        }

        users.add(user);
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), users);
    }

    public User findByUsername(String username) throws IOException {
        List<User> users;

        if (Files.exists(path)) {
            users = mapper.readValue(
                    path.toFile(),
                    new TypeReference<List<User>>() {
                    }
            );
        } else {
            users = new ArrayList<>();
        }

        return users.stream().filter(user -> user.username().equals(username)).findFirst().orElse(null);
    }
}

package me.gimenez.services;

import me.gimenez.model.users.User;
import me.gimenez.repository.UserRepository;
import me.gimenez.requests.auth.LoginRequest;
import me.gimenez.requests.auth.RegisterRequest;

import java.io.IOException;
import java.util.UUID;

public class UserService {

    private final UserRepository repository = new UserRepository();

    public void register(RegisterRequest request){
        User user = new User(UUID.randomUUID(), request.name(), request.username(), request.password(), request.userType());

        try {
            repository.save(user);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public User login(LoginRequest request) {
        User user = null;
        try {
            user = repository.findByUsername(request.username());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (user == null){
            throw new RuntimeException("Usuário não encontrado.");
        }

        if (!user.password().equals(request.password())){
            throw new RuntimeException("Senha incorreta.");
        }

        return user;
    }
}

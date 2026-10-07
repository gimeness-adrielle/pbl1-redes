package me.gimenez.server.services;

import lombok.RequiredArgsConstructor;
import me.gimenez.server.exceptions.EmailAlreadyExistsException;
import me.gimenez.server.exceptions.InvalidUserException;
import me.gimenez.server.exceptions.UserNotCreatedException;
import me.gimenez.domain.models.User;
import me.gimenez.server.repository.UserRepository;
import me.gimenez.domain.dto.requests.LoginRequest;
import me.gimenez.domain.dto.requests.RegisterRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository repository;

    public User login(LoginRequest request) {
        User user = repository.findUserByEmail(request.email())
                .orElseThrow(() -> new InvalidUserException("Usuário ou senha incorretos."));

        if (!user.password().equals(request.password())) {
            throw new InvalidUserException("Usuário ou senha incorretos.");
        }

        return user;
    }

    public void register(RegisterRequest request){
        if (repository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Este email já foi cadastrado.");
        }

        User user = new User(
                UUID.randomUUID(),
                request.email(),
                request.name(),
                request.password(),
                request.userType()
        );

        try {
            repository.save(user);
        } catch (IOException e) {
            throw new UserNotCreatedException("Não foi possível registrar o usuário: " + e);
        }
    }
}

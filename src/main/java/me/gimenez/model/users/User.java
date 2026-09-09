package me.gimenez.model.users;


import java.util.UUID;

public record User(UUID id, String name, String username, String password, UserType userType) {

}

package me.gimenez.dto.auth;

import me.gimenez.model.users.UserType;

public record RegisterRequest (
    String name,
    String username,
    String password,
    UserType userType
){
}

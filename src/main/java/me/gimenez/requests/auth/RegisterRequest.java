package me.gimenez.requests.auth;

import me.gimenez.model.users.UserType;

public record RegisterRequest (
    String name,
    String username,
    String password,
    UserType userType
){
}

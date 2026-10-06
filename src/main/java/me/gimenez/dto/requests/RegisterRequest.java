package me.gimenez.dto.requests;

import me.gimenez.model.UserType;

public record RegisterRequest (
    String email,
    String name,
    String password,
    UserType userType
){
}

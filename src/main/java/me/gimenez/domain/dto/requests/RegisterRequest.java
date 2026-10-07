package me.gimenez.domain.dto.requests;

import me.gimenez.domain.models.UserType;

public record RegisterRequest (
    String email,
    String name,
    String password,
    UserType userType
){
}

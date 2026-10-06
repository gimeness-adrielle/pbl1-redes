package me.gimenez.model;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Setter;

import java.util.UUID;

public record User(@Setter(AccessLevel.NONE)
                   UUID id,

                   @Email
                   String email,

                   @NotBlank(message = "O nome é obrigatório.")
                   String name,

                   @NotBlank(message = "A senha é obrigatória.")
                   @Size(min = 8, max = 16, message = "A senha deve ter entre 8 e 16 caracteres.")
                   String password,

                   @Setter(AccessLevel.NONE)
                   UserType userType) {
}

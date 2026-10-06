package me.gimenez.model;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter @Getter
@AllArgsConstructor
public class Segment {
        @Setter(AccessLevel.NONE)
        private final UUID id;

        @Setter(AccessLevel.NONE)
        private final String serverId;

        @NotBlank(message = "Informar a origem do trecho é obrigatório")
        private final String origin;

        @NotBlank(message = "Informar o destino do trecho é obrigatório.")
        private final String destination;

        @PositiveOrZero(message = "O preço não pode ser negativo.")
        private final double price;

        @PositiveOrZero(message = "Os assentos não podem ser negativos.")
        private int availableSeats;

        @Future(message = "A data não pode ser antiga.")
        private final LocalDateTime departureAt;

        @Future(message = "A data não pode ser antiga.")
        private final LocalDateTime arrivalAt;
}


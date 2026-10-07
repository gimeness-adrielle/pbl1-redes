package me.gimenez.domain.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter @Getter
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

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Segment (
                @JsonProperty("id") UUID id,
                @JsonProperty("serverId") String serverId,
                @JsonProperty("origin") String origin,
                @JsonProperty("destination") String destination,
                @JsonProperty("price") double price,
                @JsonProperty("availableSeats") int availableSeats,
                @JsonProperty("departureAt") LocalDateTime departureAt,
                @JsonProperty("arrivalAt") LocalDateTime arrivalAt
        ){
                this.id =  id;
                this.serverId = serverId;
                this.origin = origin;
                this.destination = destination;
                this.price = price;
                this.availableSeats = availableSeats;
                this.departureAt = departureAt;
                this.arrivalAt = arrivalAt;
        }
}


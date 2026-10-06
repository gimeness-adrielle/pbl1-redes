package me.gimenez.dto.requests;

import java.util.List;
import java.util.UUID;

public record ReservationRequest (
        List<UUID> segmentsIds
){
}

package me.gimenez.dto.responses;

public record Response(
        String status,
        String message,
        Object data
) {

    public static Response ok (String message, Object data){
        return new Response("OK", message, data);
    }

    public static Response error (String message){
        return new Response("ERROR", message, null);
    }

    public static Response notFound (String message){
        return new Response("NOT FOUND", message, null);
    }
}

package me.gimenez.util;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InputValidation {
    private static final Scanner sc = new Scanner(System.in);

    public static Integer readInteger(String message) {
        while (true) {
            System.out.print(message);
            String input = sc.nextLine().trim();

            if (isQuitting(input)) { return null; }

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("❌ Valor inválido. Digite um número inteiro ou 'q' para sair.");
            }
        }
    }

    public static LocalDateTime readDateTime(String message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH-mm");
        while (true) {
            System.out.print(message);
            String input = sc.nextLine().trim();

            if (isQuitting(input)) { return null; }

            try {
                return LocalDateTime.parse(input, formatter);
            } catch (DateTimeParseException e) {
                System.out.println("❌ Formato inválido. Use exatamente o padrão: (dd-MM-yyyy HH-mm)");
            }
        }
    }

    public static LocalDate readLocalDate(String message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        while (true) {
            System.out.print(message);
            String input = sc.nextLine().trim();

            if (isQuitting(input)) { return null; }

            try {
                return LocalDate.parse(input, formatter);
            } catch (DateTimeParseException e) {
                System.out.println("❌ Formato inválido. Use exatamente o padrão: (dd-MM-yyyy)");
            }
        }
    }

    public static String readInput(String message, String errorMessage, Predicate<String> condition){
        while(true){
            System.out.print(message);
            String input = sc.nextLine().trim();

            if (condition.test(input)){
                return input;
            }
            System.out.println(errorMessage);
        }
    }

    public static double convertPrice (String input){
        while (true){
            try {
                String cleanInput = input.trim().replace(",", ".");
                return Double.parseDouble(cleanInput);
            } catch (NumberFormatException e) {
                System.out.println("Erro: Por favor, digite um número válido.");
            }
        }
    }

    public static boolean isQuitting (String input){
        return "q".equalsIgnoreCase(input);
    }

    public static String normalize(String city) {
        if (city == null || city.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(
                        city.trim(),
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);

        return Arrays.stream(normalized.split(" "))
                .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT)
                        + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}

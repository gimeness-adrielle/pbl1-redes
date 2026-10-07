package me.gimenez.client.view;

import me.gimenez.client.PassengerClient;
import me.gimenez.domain.dto.responses.ItineraryResponse;
import me.gimenez.domain.models.Reservation;
import me.gimenez.domain.models.Segment;
import me.gimenez.domain.dto.responses.Response;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import static me.gimenez.util.InputValidation.*;

public class PassengerApp {

    private static final Scanner sc =  new Scanner(System.in);
    private final PassengerClient passengerClient;

    public PassengerApp(PassengerClient passengerClient) {
        this.passengerClient = passengerClient;
    }

    public void start() {
        while(true) {
            System.out.println("\nBem vindo ao painel de passageiro!\n");

            System.out.println("Selecione uma opção:\n");
            System.out.println("1- Buscar itinerários");
            System.out.println("2- Consultar minhas reservas");
            System.out.println("3- Cancelar reservas");
            System.out.println("q- Sair");

            String choice = sc.nextLine();

            switch (choice) {
                case "1": showSearchItinerary(); break;
                case "2": showListReservations(); break;
                case "3": showDeleteReservation(); break;
                case "Q":
                case "q": return;
                default:
                    System.out.println("Opção inválida. Digite uma opção válida ou 'q' para sair.");
            }
        }
    }

    private void showSearchItinerary(){
        System.out.println("Você está procurando itinerários.");
        System.out.println("Digite 'q' para sair a qualquer momento.\n");

        System.out.print("Insira a origem que deseja procurar: ");
        String origin = sc.nextLine();
        if (isQuitting(origin)){ return; }

        System.out.print("Insira o destino: ");
        String destination = sc.nextLine();
        if (isQuitting(destination)){ return; }

        LocalDate date = readLocalDate("Digite a data que deseja viajar (dd/MM/yyyy): ");

        List<ItineraryResponse> itineraries = passengerClient.searchItinerary(origin, destination, date);

        if (itineraries.isEmpty()){
            System.out.println("Não foram encontrados itinerários.");
            return;
        }

        for (int i=0; i<itineraries.size(); i++) {
            ItineraryResponse itineraryResponse = itineraries.get(i);
            printItinerary(itineraryResponse.segments(), itineraryResponse.totalPrice(), i);
        }

        Integer idx = readInteger("Para reservar, digite o número do itinerário desejado, ou digite 'q' para cancelar.\n");
        if (idx == null){ return; }

        ItineraryResponse itineraryResponse = itineraries.get(idx-1);
        if (itineraryResponse == null){
            System.out.println("Número do itinerário não encontrado.");
            return;
        }

        Response response = passengerClient.reserveItinerary(itineraryResponse.segments());

        System.out.println(response.message());
    }

    private List<Reservation> showListReservations(){
        List<Reservation> reservations = passengerClient.listReservations();
        if (reservations.isEmpty()){
            System.out.println("Você não tem reservas.");
            return null;
        }

        System.out.println("\n=== Suas reservas ====\n");

        for (int i=0; i<reservations.size(); i++) {
            Reservation reservation = reservations.get(i);
            printItinerary(reservation.segments(), reservation.totalPrice(), i);
        }

        return reservations;
    }

    private void showDeleteReservation(){
        List<Reservation> reservations = showListReservations();
        if (reservations == null){ return; }

        Integer idx = readInteger("Digite o número do itinerário para reservar ou digite 'q' para cancelar.\n ");
        if (idx == null){ return; }

        Reservation reservation = reservations.get(idx-1);

        if (reservation == null){
            System.out.println("Reserva não encontrada.");
            return;
        }

        Response response = passengerClient.deleteReservation(reservation.id());

        System.out.println(response.message());
    }

    private void printItinerary(List<Segment> segments, double totalPrice, int i){
        System.out.println("\nItinerário " + (i+1) + ":");
        System.out.println("    Trechos: ");

        for (Segment segment : segments) {
            System.out.println("        " + segment.getOrigin() + " → " + segment.getDestination() + " (R$ " + segment.getPrice() + ")");
        }

        System.out.println("Preço total: R$ " + totalPrice);
    }

}

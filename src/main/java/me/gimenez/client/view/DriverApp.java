package me.gimenez.client.view;

import lombok.RequiredArgsConstructor;
import me.gimenez.client.DriverClient;
import me.gimenez.dto.responses.RideResponse;
import me.gimenez.dto.requests.SegmentRequest;
import me.gimenez.dto.responses.SegmentResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static me.gimenez.util.InputValidation.*;

@RequiredArgsConstructor
public class DriverApp {

    private static final Scanner sc =  new Scanner(System.in);
    private final DriverClient driverClient;

    public void start(){
        while(true){
            System.out.println("\nBem-vindo ao painel de motorista.\n");

            System.out.println("Selecione uma opção:\n");
            System.out.println("1- Cadastrar carona");
            System.out.println("2- Listar caronas cadastradas");
            System.out.println("3- Cancelar uma carona");
            System.out.println("q- Sair");

            String choice = sc.nextLine();

            switch (choice) {
                case "1": showPublishRide(); break;
                case "2": showDriverRides(); break;
                case "3": showDeleteRide(); break;
                case "Q":
                case "q": return;
                default:
                    System.out.println("Opção inválida. Digite uma opção válida ou 'q' para sair.");
            }
        }
    }

    public void showPublishRide() {
        System.out.println("Você está cadastrando uma carona");
        System.out.println("Digite 'q' para sair a qualquer momento.\n");

        List<String> routes = new ArrayList<>();
        List<SegmentRequest> segments = new ArrayList<>();

        Integer numOfCities = readInteger("Número de cidades em que você irá passar: ");
        if (numOfCities == null) { return; }

        Integer seats = readInteger("Quantidade de assentos: ");
        if (seats == null) { return; }

        for (int i = 0; i < numOfCities; i++) {
            System.out.println("Informe o nome da cidade " + (i+1) + ":");
            String city = sc.nextLine();
            if (isQuitting(city)){ return; }

            routes.add(city);
        }

        for (int i = 0; i < routes.size() - 1; i++) {
            String origin = routes.get(i);
            String destination = routes.get(i+1);

            System.out.println("Informe os detalhes do trecho:");
            System.out.println("Trecho " + (i+1) + ":\n     " + origin + " → " + destination);

            System.out.print("Preço do trecho: ");
            String priceInput = sc.nextLine();
            if (isQuitting(priceInput)) { return; }

            double price = convertPrice(priceInput);

            LocalDateTime departureAt = readDateTime("Data e horário de saída de " + origin + " (dd-MM-yyyy HH-mm): ");
            if (departureAt == null) { return; }

            LocalDateTime arrivalAt = readDateTime("Data e horário de chegada em " + destination + " (dd-MM-yyyy HH-mm): ");
            if (arrivalAt == null) { return; }

            segments.add(driverClient.createSegmentRequest(origin, destination, price, departureAt, arrivalAt));
        }

        String response = driverClient.publishRide(seats, segments);

        System.out.println(response);
    }

    public List<RideResponse> showDriverRides (){
        List<RideResponse> rides = driverClient.getRidesByDriver();

        if (rides.isEmpty()){
            System.out.println("Você não possui caronas cadastradas.");
            return null;
        }

        System.out.println("Veja abaixo todas as suas caronas cadastradas.");
        for (int i = 0; i<rides.size(); i++) {
            RideResponse ride = rides.get(i);

            System.out.print("\nViagem " + (i+1) + " | ");
            System.out.println(ride.departureAt().toLocalDate() + " saída às " + ride.departureAt().toLocalTime());

            List<SegmentResponse> segments =  ride.segments();

            System.out.println("Trechos da viagem:");

            for (SegmentResponse segment : segments) {
                System.out.println("    " + segment.origin() + " → " + segment.destination());
                System.out.println("    Preço: R$" + segment.price());

                if (segment.availableSeats() == 0){
                    System.out.println("    Status: LOTADO.\n");
                } else {
                    System.out.println("    Status: " + segment.availableSeats() + " assentos disponíveis.\n");
                }
            }
        }

        return rides;
    }

    public void showDeleteRide(){
        List<RideResponse> rides = showDriverRides();
        if (rides == null){ return; }

        Integer idx = readInteger("Escolha o número da viagem para deletar ou digite 'q para cancelar a operação.\n");
        if (idx == null) { return; }

        if (idx-1 < 0 || idx-1 >= rides.size()) {
            System.out.println("ERRO: Escolha o número válido de uma viagem.");
            return;
        }
        RideResponse ride = rides.get(idx-1);
        String response = driverClient.deleteRide(ride.id());

        System.out.println(response);

    }
}

package me.gimenez.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.gimenez.dto.requests.Request;
import me.gimenez.dto.responses.Response;
import me.gimenez.exceptions.CommunicationErrorException;
import me.gimenez.exceptions.ServerUnavailableException;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client implements AutoCloseable{
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;

    private final ObjectMapper mapper;

    private final String serverIp;
    private final int serverPort;

    public Client(ObjectMapper mapper) {
        this.mapper = mapper;
        this.serverIp = System.getenv().getOrDefault("SERVER_IP", "localhost");
        this.serverPort = Integer.parseInt(System.getenv().getOrDefault("SOCKET_PORT", "7071"));
    }

    public void connect(){
        try {
            this.socket = new Socket(serverIp, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (UnknownHostException e){
            throw new ServerUnavailableException("Não foi possível conectar ao servidor. Verifique se o servidor está disponível.");
        } catch (IOException e){
            throw new CommunicationErrorException("Falha ao abrir canais de comunicação com o servidor.");
        }
    }

    @Override
    public void close()  {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao fechar conexões do cliente.", e);
        }
    }

    protected Response sendRequest(String type, Object data) {
        try {
            Request request = new Request(type, data);

            String json = mapper.writeValueAsString(request);

            out.println(json);

            String response = in.readLine();

            if (response == null){
                throw new ServerUnavailableException("O servidor fechou a conexão inesperadamente.");
            }

            return mapper.readValue(response, Response.class);
        } catch (IOException e) {
            throw new CommunicationErrorException("Falha na comunicação de rede com o servidor." + e);
        }
    }
}

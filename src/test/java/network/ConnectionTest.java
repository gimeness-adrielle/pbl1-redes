package network;

import me.gimenez.socket.client.Client;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConnectionTest {

    @Test
    public void testConnection() throws IOException {
        Client client = new Client();
        client.startConnection("localhost", 5000);
        String response = client.sendMessage("hello server!");
        assertEquals("hello client!", response);

    }

    @Test
    public void testMultipleClients() throws InterruptedException {

        Thread client1 =  new Thread(() -> {
            try {
                Client client = new Client();
                client.startConnection("localhost", 5000);
                String response = client.sendMessage("hello server!");
                assertEquals("hello client!", response);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        Thread client2 =  new Thread(() -> {
            try {
                Client client = new Client();
                client.startConnection("localhost", 5000);
                String response = client.sendMessage("hello server!");
                assertEquals("hello client!", response);
            } catch (IOException e){
                throw new RuntimeException(e);
            }
        });

        client1.start();
        client2.start();

        client1.join();
        client2.join();
    }

}

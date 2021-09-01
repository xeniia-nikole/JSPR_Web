package WEB;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executors;

public class Server {

    public static void listen(int port){
        final var threadPool = Executors.newFixedThreadPool(64);
        try (final var serversocket = new ServerSocket(port)) {

            while (true) {
                final var socket = serversocket.accept();
                threadPool.execute(new ThreadServer(socket));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

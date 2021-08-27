import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args){
        final var server = new Server();

        // добавление handler'ов (обработчиков)
        server.addHandler("GET", "/index.html", (request, responseStream) -> {
            // TODO: handlers code
            sendRespond(request, responseStream);

        });
        server.addHandler("POST", "/messages", (request, responseStream) -> {
            // TODO: handlers code
            sendRespond(request, responseStream);
        });

        server.listen(9999);
    }

    public static void sendRespond(Request request, BufferedOutputStream responseStream) {
        final var filePath = Path.of("src", "public", request.getPath());
        final String mimeType;
        try {
            mimeType = Files.probeContentType(filePath);
            final var length = Files.size(filePath);
            responseStream.write(("HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + mimeType + "\r\n" +
                    "Content-Length: " + length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n"
            ).getBytes());
            Files.copy(filePath, responseStream);
            responseStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
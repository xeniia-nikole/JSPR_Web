import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final Map<String, Map<String, Handler>> handlers;
    final Map<String, Handler> handlerMap = new HashMap<>();
    final ExecutorService threadPool = Executors.newFixedThreadPool(64);
    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png",
            "/resources.html", "/styles.css", "/app.js", "/links.html",
            "/forms.html", "/classic.html", "/events.html", "/events.js");

    public Server() {
        System.out.println("Server started");
        this.handlers = new HashMap<>();
    }

    public void listen(int port) {

        try (final var serversocket = new ServerSocket(port)) {

            while (true) {
                threadPool.execute(new ThreadServer(serversocket.accept(), handlers, validPaths));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void addHandler(String method, String path, Handler handler) {
        handlerMap.put(path, handler);
        handlers.put(method, handlerMap);
    }

}

class ThreadServer implements Runnable {
    private static Socket socket;
    protected final List<String> validPaths;
    private final Map<String, Map<String, Handler>> handlers;
    private final Handler notFoundHandler = (request, out) -> {
        try {
            out.write((
                    """
                            HTTP/1.1 404 Not Found\r
                            Content-Length: 0\r
                            Connection: close\r
                            \r
                            """
            ).getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };


    public ThreadServer(Socket client, Map<String, Map<String, Handler>> handlers, List<String> validPaths) {
        ThreadServer.socket = client;
        this.handlers = handlers;
        this.validPaths = validPaths;
    }

    @Override
    public void run() {

        try (final var input = socket.getInputStream();
             final var output = new BufferedOutputStream(socket.getOutputStream())) {
            final var requestLine = Request.getFromInputStream(input);

            Map<String, Handler> handlerMap = handlers.get(requestLine.getMethod());
            if (handlerMap != null){
                Handler handler = handlerMap.get(requestLine.getPath());

                if (handler != null){
                    handler.handle(requestLine, output);

                } else invalidPath(output);

            }else notFoundHandler.handle(requestLine, output);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void invalidPath(BufferedOutputStream out) {
        try {
            out.write(("""
                    HTTP/1.1 404Not Found\r
                    Content-Length: 0\r
                    Connection: close\r
                    \r
                    """
            ).getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
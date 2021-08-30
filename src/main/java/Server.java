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
    public static final String GET = "GET";
    public static final String POST = "POST";

    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png",
            "/resources.html", "/styles.css", "/app.js", "/links.html",
            "/forms.html", "/classic.html", "/events.html", "/events.js");
    final List<String> allowedMethods = List.of(GET, POST);

    public Server() {
        System.out.println("Server started");
        this.handlers = new HashMap<>();
    }

    public void listen(int port) {

        try (final var serverSocket = new ServerSocket(port)) {

            while (true) {
                threadPool.execute(new ThreadServer(serverSocket.accept(),
                        handlers, validPaths, allowedMethods));
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
    private final List<String> allowedMethods;
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


    public ThreadServer(Socket client, Map<String, Map<String, Handler>> handlers,
                        List<String> validPaths, List<String> allowedMethods) {
        ThreadServer.socket = client;
        this.handlers = handlers;
        this.validPaths = validPaths;
        this.allowedMethods = allowedMethods;
    }

    @Override
    public void run() {

        try (final var input = socket.getInputStream();
             final var output = new BufferedOutputStream(socket.getOutputStream())) {
            final var requestLine = Request.getFromInputStream(input, output, allowedMethods);

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
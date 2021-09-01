package WEB;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ThreadServer implements Runnable {
    private static Socket socket;
    public static final String GET = "GET";
    public static final String POST = "POST";

    public ThreadServer(Socket client) {
        ThreadServer.socket = client;
    }

    @Override
    public void run() {
        final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png",
                "/resources.html", "/styles.css", "/app.js", "/links.html",
                "/forms.html", "/classic.html", "/events.html", "/events.js");
        final var allowedMethods = List.of(GET, POST);

        try (final var in = new BufferedInputStream(socket.getInputStream());
             final var out = new BufferedOutputStream(socket.getOutputStream())) {

            final var limit = 4096;

            in.mark(limit);
            final var buffer = new byte[limit];
            final var read = in.read(buffer);

            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                throw new IOException("Invalid request");
            }

            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLine.length != 3) {
                throw new IOException("Invalid request");
            }

            final var method = requestLine[0];
            if (!allowedMethods.contains(method)) {
                throw new IOException("Invalid method");
            }

            final var path = requestLine[1];
            if (!path.startsWith("/")) {
                throw new IOException("Invalid path");
            }

            var queryString = getQueryString(path);
            var queryParams = getParamsMap(queryString);

            final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final var headersStart = requestLineEnd + requestLineDelimiter.length;
            final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                throw new IOException("Invalid headers");
            }

            in.reset();
            in.skip(headersStart);

            final var headersBytes = in.readNBytes(headersEnd - headersStart);
            final var headersList = Arrays.asList(new String(headersBytes).split("\r\n"));
            var headers = new HashMap<String, String>();
            for (String header : headersList) {
                var headerParts = header.split(": ");
                headers.put(headerParts[0], headerParts[1]);
            }

            var bodyString = getBodyString(in, method, headersDelimiter, headersList);
            var bodyParams = getParamsMap(bodyString);

            printRequest(method, path, queryParams, headers, bodyParams);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printRequest(String method,
                              String path,
                              Map<String, List<String>> queryParams,
                              HashMap<String, String> headers,
                              Map<String, List<String>> bodyParams) {
        System.out.println("Request:\n" +
                "method =" + method +
                "path =" + path +
                "query params = ");
        printMapXwww(queryParams);
        System.out.println("headers =");
        printMap(headers);
        System.out.println("body =");
        printMapXwww(bodyParams);
    }


    public static void printMap(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", value: " + entry.getValue());
        }
    }

    public static void printMapXwww (Map < String, List < String >> map){
        for (Map.Entry<String, List<String>> mapEntry : map.entrySet()) {
            System.out.print("Name: " + mapEntry.getKey());
            System.out.print(", value: ");
            for (String value : mapEntry.getValue()) {
                System.out.print(value + " ");
            }
            System.out.println();
        }
    }

        private static String getBodyString (BufferedInputStream in,
                String method,
        byte[] headersDelimiter,
        List<String> headersList)
            throws IOException {
            String body = "";
            if (!method.equals(GET)) {
                in.skip(headersDelimiter.length);
                final var contentLength = extractHeader(headersList, "Content-Length");
                if (contentLength.isPresent()) {
                    final var length = Integer.parseInt(contentLength.get());
                    final var bodyBytes = in.readNBytes(length);
                    body = new String(bodyBytes);
                }
            }
            return body;
        }

        private static String getQueryString (String path){
            var index = path.indexOf('?');
            return path.substring(index + 1);
        }

        public Map<String, List<String>> getPostParamsXwww (String stringName){

            Map<String, List<String>> params = new HashMap<>();

            String[] urlParts = stringName.split("\\?");

            if (urlParts.length > 1) {
                String query = urlParts[1];
                String[] pairs = query.split("\\&");
                for (int i = 0; i < pairs.length; i++) {
                    String[] data = pairs[i].split("=");
                    String name = URLDecoder.decode(data[0], StandardCharsets.UTF_8);
                    String value = "";
                    if (data.length > 1) {
                        value = URLDecoder.decode(data[1], StandardCharsets.UTF_8);
                    }
                    List<String> values = params.get(name);
                    if (values == null) {
                        values = new ArrayList<>();
                        params.put(name, values);
                    }
                    values.add(value);
                }

            }
            return params;
        }

        private static Map<String, List<String>> getParamsMap (String string){
            Map<String, List<String>> params = new HashMap<>();
            var parsed = URLEncodedUtils.parse(string, StandardCharsets.UTF_8);
            for (NameValuePair nameValuePair : parsed) {
                var name = nameValuePair.getName();
                var value = nameValuePair.getValue();
                if (!params.containsKey(name)) {
                    params.put(name, new ArrayList<>());
                }
                params.get(name).add(value);
            }
            return params;
        }


        private static int indexOf ( byte[] array, byte[] target, int start, int max){
            outer:
            for (int i = start; i < max - target.length + 1; i++) {
                for (int j = 0; j < target.length; j++) {
                    if (array[i + j] != target[j]) {
                        continue outer;
                    }
                }
                return i;
            }
            return -1;
        }

        private static Optional<String> extractHeader (List < String > headers, String header){
            return headers.stream()
                    .filter(o -> o.startsWith(header))
                    .map(o -> o.substring(o.indexOf(" ")))
                    .map(String::trim)
                    .findFirst();
        }

}

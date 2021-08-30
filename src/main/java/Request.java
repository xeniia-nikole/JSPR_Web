import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public record Request(String method, String path,
                      Map<String, String> headers,
                      Map<String, String> body, String queryString,
                      Map<String, List<String>> queryParams) {


    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public static Request getFromInputStream(InputStream inputStream, OutputStream outputStream,
                                             List<String> allowedMethods)
            throws IOException {
        final var input = new BufferedInputStream(inputStream);
        final var output = new BufferedOutputStream(outputStream);
// limit
        final var limit = 4096;
// mark limit, get input
        input.mark(limit);
        final var buffer = new byte[limit];
        final var read = input.read(buffer);
// get request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            badRequest(output);
        }
// read request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            badRequest(output);
        }
// get method
        final var method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            badRequest(output);
        }
        System.out.println(method);
// get path
        final var path = requestLine[1];
        if (!path.startsWith("/")) {
            badRequest(output);
        }
        System.out.println(path);
// get query
        final var queryString = getQueryString(path);
        final var queryParams = getQueryParams(queryString);
        System.out.println(queryParams);
// search headers
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            badRequest(output);
        }
// reset to buffer's beginning and skip
        input.reset();
        input.skip(headersStart);
// search ang get headers
        final var headersBytes = input.readNBytes(headersEnd - headersStart);
        final var headersString = new String(headersBytes);
        final var headers = headersMap(headersString);
// search and get body
        byte[] bodyBytes = new byte[0];
        if (!method.equals("GET")) {
            input.skip(headersDelimiter.length);
            final var contentLength = extractHeader(
                    Arrays.asList(headersString.split("\r\n")), "Content-Length");
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                bodyBytes = input.readNBytes(length);
            }
        }
        final var body = getBody(bodyBytes);

        output.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        output.flush();

        return new Request(method, path, headers, body, queryString, queryParams);
    }

    private static Map<String, String> getBody(byte[] bodyBytes) {
        String bodyString = new String(bodyBytes);
        Map<String, String> body = new HashMap<>();
        while (!(bodyString).equals("")) {
            var index = bodyString.indexOf("=");
            var headerName = bodyString.substring(0, index);
            var headerValue = bodyString.substring(index + 1);
            body.put(headerName, headerValue);
        }
        return body;

    }

    private static Map<String, String> headersMap(String headersString) {
        Map<String, String> headers = new HashMap<>();
        while (!(headersString).equals("")) {
            var index = headersString.indexOf(":");
            var headerName = headersString.substring(0, index);
            var headerValue = headersString.substring(index + 2);
            headers.put(headerName, headerValue);
        }
        return headers;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static String getQueryString(String path) {
        var index = path.indexOf('?');
        return path.substring(index + 1);
    }

    private static Map<String, List<String>> getQueryParams(String queryString) {
        Map<String, List<String>> params = new HashMap<>();
        var parsed = URLEncodedUtils.parse(queryString, StandardCharsets.UTF_8);
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

    // from Google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
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

    static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    @Override
    public String toString() {
        return "Request{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", headers=" + headers +
                ", queryString='" + queryString + '\'' +
                ", queryParams=" + queryParams +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Request) obj;
        return Objects.equals(this.method, that.method) &&
                Objects.equals(this.path, that.path) &&
                Objects.equals(this.headers, that.headers) &&
                Objects.equals(this.body, that.body) &&
                Objects.equals(this.queryString, that.queryString) &&
                Objects.equals(this.queryParams, that.queryParams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, path, headers, body, queryString, queryParams);
    }

}

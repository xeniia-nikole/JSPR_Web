import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Request {

    public static final String GET = "GET";
    public static final String POST = "POST";
    private final String method, path;
    private final String queryString;
    private final Map<String, List<String>> queryParams;
    private final Map<String, String> headers;
    private final Map<String, List<String>> postParams;
    private final InputStream input;

    private Request(String method,
                    String path,
                    String queryString,
                    Map<String, List<String>> queryParams,
                    Map<String, String> headers,
                    Map<String, List<String>> postParams,
                    InputStream input) {
        this.method = method;
        this.path = path;
        this.queryString = queryString;
        this.queryParams = queryParams;
        this.headers = headers;
        this.postParams = postParams;
        this.input = input;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getQueryString() { return queryString; }

    public Map<String, List<String>> getQueryParams() { return queryParams; }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public InputStream getInput() {
        return input;
    }

    public static Request getFromInputStream(InputStream inputStream) throws IOException {
        final var allowedMethods = List.of(GET, POST);
        var in = new BufferedInputStream(inputStream);

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
        var postParams = getParamsMap(bodyString);

        return new Request(method, path, queryString, queryParams, headers, postParams, inputStream);
    }

    private static String getBodyString(BufferedInputStream in,
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

    private static String getQueryString(String path) {
        var index = path.indexOf('?');
        return path.substring(index + 1);
    }

    private static Map<String, List<String>> getParamsMap(String string) {
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

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    @Override
    public String toString() {
        return "Request{" +
                "\nmethod='" + method + '\'' +
                ", \npath='" + path + '\'' +
                ", \nqueryString='" + queryString + '\'' +
                ", \nqueryParams=" + queryParams +
                ", \nheaders=" + headers +
                ", \npostParams=" + postParams +
                '}';
    }
}

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Request {
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final String queryString;
    private final Map<String, List<String>> queryParams;


    public Request(String method, String path, Map<String, String> headers,
                   String queryString, Map<String, List<String>> queryParams) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.queryString = queryString;
        this.queryParams = queryParams;
    }


    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public static Request getFromInputStream(InputStream inputStream) throws IOException {
        final var input = new BufferedReader(new InputStreamReader(inputStream));
        final var requestLine = input.readLine();
        final var parts = requestLine.split(" ");

        if (parts.length != 3) {
            System.out.println("Error! Incorrect format");
        }

        final var path = parts[1];
        final var method = parts[0];

        Map<String, String> headers = new HashMap<>();
        String line;
        while (!(line = input.readLine()).equals("")) {
            var index = line.indexOf(":");
            var headerName = line.substring(0, index);
            var headerValue = line.substring(index + 2);
            headers.put(headerName, headerValue);
        }

        var queryString = getQueryString(path);

        var queryParams = getQueryParams(queryString);
        System.out.println(queryParams);


        return new Request(method, path, headers, queryString, queryParams);

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

    @Override
    public String toString() {
        return "Request[" +
                "method=" + method + ", " +
                "path=" + path + ", " +
                "headers=" + headers + ']';
    }


}

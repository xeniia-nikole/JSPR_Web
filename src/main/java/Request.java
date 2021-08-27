import java.io.*;
import java.util.HashMap;
import java.util.Map;

public record Request(String method, String path,
                      Map<String, String> headers) {


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
        return new Request(method, path, headers);

    }


}

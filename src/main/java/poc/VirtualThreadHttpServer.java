package poc;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class VirtualThreadHttpServer {

    public static void main(String[] args) throws Exception {
        int port = argInt(args, 0, 8080);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        // Each request is handled on its own virtual thread.
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.createContext("/hello", ex -> {
            Map<String, String> q = queryParams(ex.getRequestURI());
            String name = q.getOrDefault("name", "world");
            respond(ex, 200, "hello " + name + "\n");
        });

        server.createContext("/sleep", ex -> {
            Map<String, String> q = queryParams(ex.getRequestURI());
            int ms = 200;
            try { ms = Integer.parseInt(q.getOrDefault("ms", "200")); } catch (NumberFormatException ignored) {}

            long start = System.currentTimeMillis();
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            long took = System.currentTimeMillis() - start;
            respond(ex, 200,
                    "slept=" + took + "ms\n" +
                    "thread=" + Thread.currentThread() + "\n" +
                    "time=" + Instant.now() + "\n");
        });

        server.createContext("/", ex -> respond(ex, 200,
                "Endpoints:\n" +
                "  /hello?name=world\n" +
                "  /sleep?ms=200\n"));

        server.start();
        System.out.println("Server listening on http://localhost:" + port);
        System.out.println("Try: curl http://localhost:" + port + "/sleep?ms=200");
    }

    private static void respond(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> queryParams(URI uri) {
        Map<String, String> m = new HashMap<>();
        String q = uri.getRawQuery();
        if (q == null || q.isEmpty()) return m;
        String[] parts = q.split("&");
        for (String p : parts) {
            int i = p.indexOf('=');
            if (i < 0) {
                m.put(urlDecode(p), "");
            } else {
                m.put(urlDecode(p.substring(0, i)), urlDecode(p.substring(i + 1)));
            }
        }
        return m;
    }

    private static String urlDecode(String s) {
        // Minimal decode for typical query params; avoids bringing extra deps.
        return s.replace("+", " ").replace("%20", " ");
    }

    private static int argInt(String[] args, int idx, int def) {
        if (args == null || args.length <= idx) return def;
        try {
            return Integer.parseInt(args[idx]);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}

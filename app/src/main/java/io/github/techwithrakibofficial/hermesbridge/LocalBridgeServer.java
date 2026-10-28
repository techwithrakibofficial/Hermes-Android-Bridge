package io.github.techwithrakibofficial.hermesbridge;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

/** A deliberately narrow HTTP/1.1 loopback protocol. No bodies or arbitrary commands. */
public final class LocalBridgeServer implements AutoCloseable {
    public interface ActionHandler { boolean execute(String action); }
    public static final List<String> SUPPORTED_APPS = Collections.unmodifiableList(Arrays.asList(
        "whatsapp", "facebook", "messenger", "telegram", "chrome", "termux"));
    private final ServerSocket listener;
    private final ActionHandler handler;
    private final String token;
    private final Set<String> usedIds = new HashSet<>();
    private volatile boolean running;
    private volatile Socket client;
    private Thread worker;

    public LocalBridgeServer(int port, ActionHandler handler) throws IOException {
        this.handler = handler;
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        token = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        listener = new ServerSocket();
        try {
            listener.bind(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}), port), 8);
        } catch (IOException e) { listener.close(); throw e; }
    }

    public String getToken() { return token; }
    public int getPort() { return listener.getLocalPort(); }
    public String getBoundAddress() { return listener.getInetAddress().getHostAddress(); }
    public boolean isRunning() { return running; }

    public synchronized void start() {
        if (worker != null || listener.isClosed()) throw new IllegalStateException("Session cannot restart");
        running = true;
        worker = new Thread(this::serve, "hermes-loopback");
        worker.setDaemon(true);
        worker.start();
    }

    private void serve() {
        try {
            while (running) {
                try (Socket socket = listener.accept()) {
                    client = socket;
                    if (!running || !socket.getInetAddress().isLoopbackAddress()) continue;
                    socket.setSoTimeout(2000);
                    try { handle(socket); }
                    catch (BadRequest e) { respond(socket, e.status, e.message); }
                    catch (SocketTimeoutException e) { respond(socket, 408, "request_timeout"); }
                    catch (IOException ignored) { /* Client disconnected; never log credentials. */ }
                    finally { client = null; }
                } catch (IOException e) { if (!running) break; }
            }
        } finally {
            running = false;
            try { listener.close(); } catch (IOException ignored) { }
        }
    }

    private void handle(Socket socket) throws IOException, BadRequest {
        InputStream input = socket.getInputStream();
        long deadline = System.nanoTime() + 2_000_000_000L;
        int[] budget = {0};
        String[] request = line(input, socket, deadline, budget).split(" ", -1);
        if (request.length != 3 || !request[2].equals("HTTP/1.1")) throw new BadRequest(400, "bad_request");
        Map<String,String> headers = new HashMap<>();
        for (int n = 0; ; n++) {
            String value = line(input, socket, deadline, budget);
            if (value.isEmpty()) break;
            if (n >= 32) throw new BadRequest(431, "headers_too_large");
            int colon = value.indexOf(':');
            if (colon <= 0) throw new BadRequest(400, "bad_header");
            String key = value.substring(0, colon).toLowerCase(Locale.ROOT);
            if (!key.matches("[a-z0-9-]+") || headers.put(key, value.substring(colon + 1).trim()) != null)
                throw new BadRequest(400, "duplicate_or_bad_header");
        }
        if (!Objects.equals(headers.get("host"), "127.0.0.1:" + getPort())) throw new BadRequest(400, "invalid_host");
        String supplied = headers.getOrDefault("authorization", "");
        if (!MessageDigest.isEqual(("Bearer " + token).getBytes(StandardCharsets.US_ASCII), supplied.getBytes(StandardCharsets.US_ASCII)))
            throw new BadRequest(401, "unauthorized");
        if (headers.containsKey("origin")) throw new BadRequest(403, "browser_origin_forbidden");
        if (headers.containsKey("transfer-encoding") || !headers.getOrDefault("content-length", "0").equals("0"))
            throw new BadRequest(400, "request_body_not_supported");
        if (request[0].equals("GET") && request[1].equals("/v1/status")) {
            respondJson(socket, 200, "{\"bridge\":\"ready\",\"version\":\"0.3.0\",\"allowed_actions\":[\"home\",\"back\",\"settings\",\"open_app\"],\"supported_apps\":[\"whatsapp\",\"facebook\",\"messenger\",\"telegram\",\"chrome\",\"termux\"]}");
            return;
        }
        if (!request[0].equals("POST")) throw new BadRequest(405, "method_not_allowed");
        final String action;
        if (request[1].equals("/v1/actions/home")) action = "home";
        else if (request[1].equals("/v1/actions/back")) action = "back";
        else if (request[1].equals("/v1/actions/settings")) action = "settings";
        else if (request[1].startsWith("/v1/actions/open-app/")) {
            String app = request[1].substring("/v1/actions/open-app/".length());
            if (!SUPPORTED_APPS.contains(app)) throw new BadRequest(404, "app_not_allowed");
            action = "open_app:" + app;
        }
        else throw new BadRequest(404, "action_not_allowed");
        String id = headers.getOrDefault("x-request-id", "");
        if (!id.matches("[A-Za-z0-9_-]{8,80}")) throw new BadRequest(400, "valid_request_id_required");
        if (usedIds.contains(id)) throw new BadRequest(409, "duplicate_request_id");
        // No eviction: IDs remain reserved for the entire session, including rejected/timeout actions.
        if (usedIds.size() >= 256) throw new BadRequest(429, "session_limit_restart_bridge");
        usedIds.add(id);
        if (!running) throw new BadRequest(503, "bridge_stopped");
        try {
            boolean accepted = handler.execute(action);
            respondJson(socket, accepted ? 200 : 503,
                accepted ? "{\"accepted\":true,\"verification\":\"check_phone_screen\"}" : "{\"accepted\":false,\"error\":\"android_action_rejected\"}");
        } catch (RuntimeException e) { respond(socket, 503, "execution_unavailable_or_timed_out"); }
    }

    private String line(InputStream input, Socket socket, long deadline, int[] budget) throws IOException, BadRequest {
        StringBuilder result = new StringBuilder();
        while (true) {
            long left = deadline - System.nanoTime();
            if (left <= 0) throw new SocketTimeoutException();
            socket.setSoTimeout((int)Math.max(1, Math.min(2000, left / 1_000_000L)));
            int value = input.read();
            if (++budget[0] > 8192 || result.length() > 1024) throw new BadRequest(431, "headers_too_large");
            if (value == -1) throw new EOFException();
            if (value == '\r') {
                if (input.read() != '\n') throw new BadRequest(400, "invalid_line_ending");
                budget[0]++;
                return result.toString();
            }
            if (value < 32 || value > 126) throw new BadRequest(400, "invalid_header_character");
            result.append((char)value);
        }
    }

    private void respond(Socket socket, int status, String error) throws IOException {
        respondJson(socket, status, "{\"error\":\"" + error + "\"}");
    }
    private void respondJson(Socket socket, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + status + (status == 200 ? " OK" : " Error") + "\r\nContent-Type: application/json\r\nContent-Length: " + body.length + "\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n";
        socket.getOutputStream().write(headers.getBytes(StandardCharsets.US_ASCII));
        socket.getOutputStream().write(body);
        socket.getOutputStream().flush();
    }

    @Override public void close() {
        running = false;
        try { listener.close(); } catch (IOException ignored) { }
        Socket active = client;
        if (active != null) try { active.close(); } catch (IOException ignored) { }
    }
    private static class BadRequest extends Exception {
        final int status;
        final String message;
        BadRequest(int status, String message) { this.status = status; this.message = message; }
    }
}

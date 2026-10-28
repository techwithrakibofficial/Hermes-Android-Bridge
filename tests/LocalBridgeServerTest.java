import io.github.techwithrakibofficial.hermesbridge.LocalBridgeServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/** Real loopback socket tests; action handler is fake, not Android execution. */
public class LocalBridgeServerTest {
    private static int checks;
    private static void check(boolean pass, String label) {
        if (!pass) throw new AssertionError(label);
        checks++;
    }
    private static String request(LocalBridgeServer server, String method, String path, String token, String extra) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", server.getPort())) {
            socket.setSoTimeout(4000);
            String raw = method + " " + path + " HTTP/1.1\r\nHost: 127.0.0.1:" + server.getPort() + "\r\nAuthorization: Bearer " + token + "\r\n" + extra + "\r\n";
            socket.getOutputStream().write(raw.getBytes(StandardCharsets.US_ASCII));
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    public static void main(String[] args) throws Exception {
        AtomicInteger actions = new AtomicInteger();
        String oldToken;
        try (LocalBridgeServer server = new LocalBridgeServer(0, action -> { actions.incrementAndGet(); return true; })) {
            server.start();
            String token = server.getToken(); oldToken = token;
            check(server.getBoundAddress().equals("127.0.0.1"), "must bind IPv4 loopback only");
            check(token.matches("[A-Za-z0-9_-]{43}"), "256-bit session token");
            check(request(server,"GET","/v1/status","wrong", "").startsWith("HTTP/1.1 401"), "wrong token denied");
            check(request(server,"GET","/v1/status",token, "").contains("\"bridge\":\"ready\""), "authenticated health");
            check(request(server,"POST","/v1/actions/home",token,"Origin: https://example.com\r\nX-Request-Id: browser01\r\n").startsWith("HTTP/1.1 403"), "browser origin denied");
            check(request(server,"GET","/v1/actions/home",token, "").startsWith("HTTP/1.1 405"), "GET cannot act");
            check(request(server,"POST","/v1/actions/shell",token,"X-Request-Id: unknown01\r\n").startsWith("HTTP/1.1 404"), "unknown action denied");
            check(request(server,"POST","/v1/actions/home",token, "").startsWith("HTTP/1.1 400"), "missing id denied");
            check(request(server,"POST","/v1/actions/home",token,"Content-Length: 5\r\nX-Request-Id: hasbody01\r\n").startsWith("HTTP/1.1 400"), "body denied");
            check(request(server,"POST","/v1/actions/home",token,"Host: evil.test\r\nX-Request-Id: dupehost1\r\n").startsWith("HTTP/1.1 400"), "duplicate host denied");
            check(request(server,"POST","/v1/actions/home",token,"X-Request-Id: action001\r\n").contains("\"accepted\":true"), "home accepted");
            check(request(server,"POST","/v1/actions/back",token,"X-Request-Id: action001\r\n").startsWith("HTTP/1.1 409"), "replay cannot change action");
            check(request(server,"POST","/v1/actions/back",token,"X-Request-Id: action002\r\n").startsWith("HTTP/1.1 200"), "back accepted");
            check(actions.get() == 2, "unauthorized/malformed/replay requests never dispatch");
            check(request(server,"GET","/v1/status",token, "").contains("supported_apps"), "advertise bounded app capability");
            check(request(server,"POST","/v1/actions/settings",token,"X-Request-Id: settings1\r\n").contains("\"accepted\":true"), "settings dispatch allowed");
            for (String app : LocalBridgeServer.SUPPORTED_APPS) {
                check(request(server,"POST","/v1/actions/open-app/"+app,token,"X-Request-Id: openapp-"+app+"\r\n").startsWith("HTTP/1.1 200"), "supported app dispatch: " + app);
            }
            check(request(server,"POST","/v1/actions/open-app/com.evil.app",token,"X-Request-Id: evilapp01\r\n").startsWith("HTTP/1.1 404"), "arbitrary package denied");
            check(request(server,"POST","/v1/actions/open-app/whatsapp%2F..",token,"X-Request-Id: encoded01\r\n").startsWith("HTTP/1.1 404"), "encoded path injection denied");
            check(request(server,"GET","/v1/status",token,"X-Large: " + "a".repeat(1100) + "\r\n").startsWith("HTTP/1.1 431"), "oversized line rejected");
            for (int i=actions.get(); i<256; i++) request(server,"POST","/v1/actions/home",token,"X-Request-Id: capacity"+i+"\r\n");
            check(request(server,"POST","/v1/actions/home",token,"X-Request-Id: overflow1\r\n").startsWith("HTTP/1.1 429"), "session bounded without replay eviction");
        }
        try (LocalBridgeServer fresh = new LocalBridgeServer(0, action -> false)) {
            fresh.start();
            check(!fresh.getToken().equals(oldToken), "new session rotates token");
            check(request(fresh,"GET","/v1/status",oldToken, "").startsWith("HTTP/1.1 401"), "old token invalid");
            check(request(fresh,"POST","/v1/actions/home",fresh.getToken(),"X-Request-Id: rejected1\r\n").startsWith("HTTP/1.1 503"), "Android rejection represented");
            check(request(fresh,"POST","/v1/actions/home",fresh.getToken(),"X-Request-Id: rejected1\r\n").startsWith("HTTP/1.1 409"), "failed id still reserved");
        }
        System.out.println("PASS: " + checks + " loopback protocol checks. Android execution NOT tested.");
    }
}

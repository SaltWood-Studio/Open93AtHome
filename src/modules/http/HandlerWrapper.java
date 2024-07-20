package modules.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;

public class HandlerWrapper implements HttpHandler {
    public void handle(HttpExchange exchange) {
        try {
            Response response = execute(exchange);
            if (response != null) {
                exchange.sendResponseHeaders(response.responseCode, response.bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.bytes);
            }
            exchange.close();
        } catch (Exception e) {
            e.printStackTrace();
            String remoteAddress = exchange.getRequestHeaders().getFirst("X-Real-IP");
            remoteAddress = remoteAddress == null || remoteAddress.isEmpty() ? exchange.getRemoteAddress().toString() : remoteAddress;
            System.out.println(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " " + exchange.getProtocol() + " - "
                    + exchange.getResponseCode() + " [" + remoteAddress + "]");
            exchange.close();
        }
    }
    
    public Response execute(HttpExchange exchange) throws Exception {
        return null;
    }
}

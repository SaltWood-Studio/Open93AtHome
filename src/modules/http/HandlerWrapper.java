package modules.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HandlerWrapper implements HttpHandler {
    public void handle(HttpExchange exchange) {
        try {
            execute(exchange);
            exchange.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String remoteAddress = exchange.getRequestHeaders().getFirst("X-Real-IP");
        remoteAddress = remoteAddress == null || remoteAddress.isEmpty() ? exchange.getRemoteAddress().toString() : remoteAddress;
        System.out.println(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " " + exchange.getProtocol() + " - "
                + exchange.getResponseCode() + " [" + remoteAddress + "]");
        exchange.close();
    }
    
    public void execute(HttpExchange exchange) throws Exception {
        return;
    }
}

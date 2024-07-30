package top.saltwood.everythingAtHome.modules.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import top.saltwood.everythingAtHome.modules.Logger;

public abstract class HandlerWrapper implements HttpHandler {
    public void handle(HttpExchange exchange) {
        try {
            execute(exchange);
            exchange.close();
        } catch (Exception e) {
            Logger.logger.log("Error: " + e);
            e.printStackTrace(Logger.logger);
        }
        String remoteAddress = exchange.getRequestHeaders().getFirst("X-Real-IP");
        remoteAddress = remoteAddress == null || remoteAddress.isEmpty() ? exchange.getRemoteAddress().toString() : remoteAddress;
        Logger.logger.log(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " " + exchange.getProtocol() + " - "
                + exchange.getResponseCode() + " [" + remoteAddress + "]");
        exchange.close();
    }
    
    public abstract void execute(HttpExchange exchange) throws Exception;
}

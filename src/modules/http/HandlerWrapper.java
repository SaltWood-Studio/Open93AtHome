package modules.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
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
            exchange.getResponseBody().close();
            exchange.close();
        } catch (Exception e) {
            e.printStackTrace();
            exchange.close();
        }
    }
    
    public Response execute(HttpExchange exchange) throws Exception {
        return null;
    }
}

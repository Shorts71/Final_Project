package org.example;

import com.sun.net.httpserver.HttpServer;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class Main {

    private static final Ledger ledger = new Ledger();

    private static String extract(String json, String key) {

        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern) + pattern.length();

        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        }

        int end = json.indexOf(",", start);

        if (end == -1) {
            end = json.indexOf("}", start);
        }

        return json.substring(start, end).trim();
    }

    private static Transaction parseTransaction(String json) {

        String from = extract(json, "from");
        String to = extract(json, "to");
        double amount = Double.parseDouble(extract(json, "amount"));

        return new Transaction(from, to, amount);
    }

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.createContext("/transaction", exchange -> {
            System.out.println("Handling request on: " + Thread.currentThread());
            try {

                if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                Transaction transaction = parseTransaction(body);

                boolean success = ledger.transfer(transaction);

                String response;

                if (success) {
                    response = "Transaction successful";
                    System.out.println(response);
                } else {
                    response = "Insufficient funds";
                    System.out.println("Error: " + response);
                }

                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());

            } catch (Exception e) {

                String error = "Server error: " + e.getMessage();
                exchange.sendResponseHeaders(500, error.length());
                exchange.getResponseBody().write(error.getBytes());

            } finally {
                exchange.close();
            }

        });

        server.start();

        System.out.println("Financial Messaging Server running on port " + server.getAddress().getPort() + ".");
    }
}
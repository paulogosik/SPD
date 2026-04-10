package com.spd.calculator.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class CalculatorClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;

    private final String host;
    private final int port;

    public CalculatorClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        System.out.println("[Cliente] Conectando ao servidor " + host + ":" + port + "...");

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("[Cliente] Conectado! Digite operações (ex: 2 + 3) ou 'sair' para encerrar.");

            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String expression = scanner.nextLine();
                if (expression.isBlank()) {
                    continue;
                }

                out.println(expression);
                String response = in.readLine();
                if (response == null) {
                    System.out.println("[Cliente] Servidor encerrou a conexão.");
                    break;
                }

                System.out.println("Resultado: " + response);

                if ("sair".equalsIgnoreCase(expression.trim()) || "exit".equalsIgnoreCase(expression.trim())) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("[Cliente] Erro de conexão: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida, usando padrão: " + DEFAULT_PORT);
            }
        }

        new CalculatorClient(host, port).start();
    }
}

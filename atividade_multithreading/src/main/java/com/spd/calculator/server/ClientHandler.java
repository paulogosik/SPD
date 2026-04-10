package com.spd.calculator.server;

import com.spd.calculator.common.ExpressionEvaluator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final int clientId;

    public ClientHandler(Socket socket, int clientId) {
        this.socket = socket;
        this.clientId = clientId;
    }

    @Override
    public void run() {
        String clientAddress = socket.getRemoteSocketAddress().toString();
        System.out.println("[Servidor] Cliente #" + clientId + " conectado: " + clientAddress);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                if ("sair".equalsIgnoreCase(line.trim()) || "exit".equalsIgnoreCase(line.trim())) {
                    out.println("Conexão encerrada. Até logo!");
                    break;
                }

                String response = processExpression(line);
                System.out.println("[Servidor] Cliente #" + clientId + " -> " + line + " = " + response);
                out.println(response);
            }
        } catch (IOException e) {
            System.err.println("[Servidor] Erro ao comunicar com cliente #" + clientId + ": " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            System.out.println("[Servidor] Cliente #" + clientId + " desconectado.");
        }
    }

    private String processExpression(String expression) {
        try {
            double result = ExpressionEvaluator.evaluate(expression);
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return String.valueOf((long) result);
            }
            return String.valueOf(result);
        } catch (ArithmeticException e) {
            return "ERRO: " + e.getMessage();
        } catch (IllegalArgumentException e) {
            return "ERRO: expressão inválida (" + e.getMessage() + ")";
        } catch (Exception e) {
            return "ERRO: falha ao processar expressão";
        }
    }
}

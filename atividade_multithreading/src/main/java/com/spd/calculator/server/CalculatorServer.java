package com.spd.calculator.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class CalculatorServer {

    private static final int DEFAULT_PORT = 5000;
    private static final int THREAD_POOL_SIZE = 20;

    private final int port;
    private final ExecutorService threadPool;
    private final AtomicInteger clientCounter = new AtomicInteger(0);

    public CalculatorServer(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void start() {
        System.out.println("[Servidor] Iniciando servidor de cálculo na porta " + port + "...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Servidor] Aguardando conexões de clientes...");

            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                int id = clientCounter.incrementAndGet();
                threadPool.submit(new ClientHandler(clientSocket, id));
            }
        } catch (IOException e) {
            System.err.println("[Servidor] Erro no servidor: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida, usando padrão: " + DEFAULT_PORT);
            }
        }

        new CalculatorServer(port).start();
    }
}

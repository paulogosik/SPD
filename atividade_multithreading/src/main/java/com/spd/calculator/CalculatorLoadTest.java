package com.spd.calculator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CalculatorLoadTest {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 6969;
    private static final int TIMEOUT_MS = 10000;

    private static final String PASS = "\u001B[92mOK\u001B[0m";
    private static final String FAIL = "\u001B[91mFAIL\u001B[0m";
    private static final String INFO = "\u001B[94m..\u001B[0m";

    private static final AtomicInteger passed = new AtomicInteger(0);
    private static final AtomicInteger failed = new AtomicInteger(0);
    private static final Object printLock = new Object();

    private static final String[][] CASES = {
            {"1+1",       "2"},
            {"2*3",       "6"},
            {"10-4",      "6"},
            {"8/2",       "4"},
            {"(1+2)*3",   "9"},
            {"2+3*4",     "14"},
            {"100/4",     "25"},
            {"-5+10",     "5"},
            {"7*7",       "49"},
            {"(10-2)/4",  "2"},
    };

    public static void main(String[] args) throws InterruptedException {
        int numConns = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        int reqsPerConn = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        long gapMs = args.length > 2 ? Long.parseLong(args[2]) : 1000L;

        System.out.println("============================================================");
        System.out.printf ("  Teste de carga  |  %d conexoes x %d reqs  |  gap %d ms%n",
                numConns, reqsPerConn, gapMs);
        System.out.println("============================================================");

        if (!waitForServer(3000)) {
            System.out.printf("%s Servidor nao respondeu em %s:%d%n", FAIL, HOST, PORT);
            System.out.printf("%s Inicie com: java -cp out com.spd.calculator.server.CalculatorServer%n", INFO);
            System.exit(1);
        }
        System.out.printf("%s Servidor acessivel em %s:%d%n%n", INFO, HOST, PORT);

        List<Thread> threads = new ArrayList<>();
        long start = System.currentTimeMillis();

        for (int i = 0; i < numConns; i++) {
            final int connId = i + 1;
            Thread t = new Thread(() -> runConnection(connId, reqsPerConn, gapMs), "conn-" + connId);
            t.start();
            threads.add(t);
        }

        for (Thread t : threads) {
            t.join();
        }
        long elapsed = System.currentTimeMillis() - start;

        int total = passed.get() + failed.get();
        System.out.println();
        System.out.println("============================================================");
        System.out.printf ("  Resultado: %d/%d passaram  |  %d falharam%n",
                passed.get(), total, failed.get());
        System.out.printf ("  Tempo total: %.2fs%n", elapsed / 1000.0);
        System.out.println("============================================================");
        System.exit(failed.get() == 0 ? 0 : 1);
    }

    private static void runConnection(int connId, int reqs, long gapMs) {
        try (Socket sock = new Socket()) {
            sock.connect(new InetSocketAddress(HOST, PORT), TIMEOUT_MS);
            sock.setSoTimeout(TIMEOUT_MS);

            try (BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                 PrintWriter out = new PrintWriter(sock.getOutputStream(), true)) {

                log(INFO, String.format("conn %02d conectada", connId));

                for (int r = 0; r < reqs; r++) {
                    int caseIdx = (connId + r) % CASES.length;
                    String expr = CASES[caseIdx][0];
                    String expected = CASES[caseIdx][1];

                    out.println(expr);
                    String got = in.readLine();

                    boolean ok = expected.equals(got);
                    String label = String.format("conn %02d req %d/%d  '%s' -> %s",
                            connId, r + 1, reqs, expr, expected);
                    record(ok, label, ok ? "" : "obtido=" + got);

                    if (r < reqs - 1) {
                        Thread.sleep(gapMs);
                    }
                }

                out.println("sair");
                in.readLine();
                log(INFO, String.format("conn %02d encerrada", connId));
            }
        } catch (IOException e) {
            record(false, "conn " + connId, "erro de IO: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            record(false, "conn " + connId, "interrompida");
        }
    }

    private static void record(boolean ok, String label, String detail) {
        if (ok) {
            passed.incrementAndGet();
        } else {
            failed.incrementAndGet();
        }
        String mark = ok ? PASS : FAIL;
        String suffix = detail.isEmpty() ? "" : "  [" + detail + "]";
        log(mark, label + suffix);
    }

    private static void log(String mark, String msg) {
        synchronized (printLock) {
            System.out.println("  " + mark + " " + msg);
        }
    }

    private static boolean waitForServer(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(HOST, PORT), 500);
                return true;
            } catch (IOException e) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }
}

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {

    static final int PORT = 6000;

    // Estado do leilão
    static String itemNome = null;
    static double lanceAtual = 0;
    static String licitanteAtual = null;
    static boolean leilaoAberto = false;
    static final Object leilaoLock = new Object();

    // Clientes conectados e histórico
    static final List<ClientHandler> clientes = new CopyOnWriteArrayList<>();
    static final List<Map<String, String>> historico = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   Servidor de Leilão | Porta " + PORT + "   ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("Comandos: ADD <item> <preco> | CLOSE | STATUS");
        System.out.println("──────────────────────────────────────────");

        Thread adminThread = new Thread(Server::handleAdmin);
        adminThread.setDaemon(true);
        adminThread.start();

        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket);
            clientes.add(handler);
            Thread t = new Thread(handler);
            t.setDaemon(true);
            t.start();
        }
    }

    // Envia mensagem para todos os clientes conectados
    static void broadcast(String mensagem) {
        for (ClientHandler c : clientes) {
            c.send(mensagem);
        }
    }

    // Loga no console do servidor com timestamp
    static void log(String msg) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        System.out.println("[" + timestamp + "] " + msg);
    }

    // Registra evento estruturado no histórico (tipo + pares chave/valor)
    static void registrar(String tipo, String... pares) {
        String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());
        Map<String, String> evento = new LinkedHashMap<>();
        evento.put("hora", hora);
        evento.put("tipo", tipo);
        for (int i = 0; i + 1 < pares.length; i += 2) {
            evento.put(pares[i], pares[i + 1]);
        }
        synchronized (historico) {
            historico.add(evento);
        }
    }

    // Converte um mapa de evento para JSON inline: {"hora": "...", "tipo": "..."}
    static String eventoParaJson(Map<String, String> ev) {
        StringBuilder sb = new StringBuilder("{");
        boolean primeiro = true;
        for (Map.Entry<String, String> e : ev.entrySet()) {
            if (!primeiro) sb.append(", ");
            sb.append("\"").append(e.getKey()).append("\": \"").append(e.getValue()).append("\"");
            primeiro = false;
        }
        sb.append("}");
        return sb.toString();
    }

    // Salva histórico em JSON incremental — cada leilão é uma entrada no array
    static void salvarHistorico() {
        String arquivo = "historico_leiloes.json";
        String data = new SimpleDateFormat("dd/MM/yyyy").format(new Date());

        // Monta o bloco JSON do leilão atual
        StringBuilder bloco = new StringBuilder();
        bloco.append("  {\n");
        bloco.append("    \"leilao\": \"").append(itemNome).append("\",\n");
        bloco.append("    \"data\": \"").append(data).append("\",\n");
        bloco.append("    \"eventos\": [\n");
        synchronized (historico) {
            for (int i = 0; i < historico.size(); i++) {
                bloco.append("      ").append(eventoParaJson(historico.get(i)));
                if (i < historico.size() - 1) bloco.append(",");
                bloco.append("\n");
            }
        }
        bloco.append("    ]\n");
        bloco.append("  }");

        try {
            File f = new File(arquivo);
            if (!f.exists() || f.length() == 0) {
                // Arquivo novo: cria array com a primeira entrada
                try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                    pw.println("[");
                    pw.print(bloco);
                    pw.println("\n]");
                }
            } else {
                // Arquivo existente: remove o último ']' e acrescenta nova entrada
                String conteudo = new String(Files.readAllBytes(f.toPath()));
                int ultimoColchete = conteudo.lastIndexOf(']');
                String antes = conteudo.substring(0, ultimoColchete).stripTrailing();
                boolean temEntradas = antes.trim().length() > 1; // mais que só '['
                try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                    pw.print(antes);
                    if (temEntradas) pw.print(",");
                    pw.println();
                    pw.print(bloco);
                    pw.println("\n]");
                }
            }
            log("Histórico salvo em " + arquivo);
        } catch (IOException e) {
            log("ERRO | Falha ao salvar histórico: " + e.getMessage());
        }
    }

    // Thread do administrador (stdin)
    static void handleAdmin() {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String linha = scanner.nextLine().trim();

            if (linha.toUpperCase().startsWith("ADD ")) {
                String[] partes = linha.split("\\s+", 3);
                if (partes.length < 3) {
                    System.out.println("Uso: ADD <item> <precoInicial>");
                    continue;
                }
                try {
                    double preco = Double.parseDouble(partes[2]);
                    synchronized (leilaoLock) {
                        if (leilaoAberto) {
                            log("AVISO | Encerre o leilão atual antes de cadastrar um novo.");
                            continue;
                        }
                        itemNome = partes[1];
                        lanceAtual = preco;
                        licitanteAtual = null;
                        leilaoAberto = true;
                        historico.clear();
                    }
                    registrar("ITEM", "item", partes[1], "preco_inicial", String.valueOf(preco));
                    broadcast("ITEM:" + partes[1] + ":" + preco);
                    log("──────────────────────────────────────────");
                    log("LEILÃO ABERTO | Item: " + partes[1] + " | Preço inicial: R$" + preco);
                    log("──────────────────────────────────────────");
                } catch (NumberFormatException e) {
                    log("ERRO | Preço inválido. Use um número (ex: 100.0)");
                }

            } else if (linha.equalsIgnoreCase("CLOSE")) {
                synchronized (leilaoLock) {
                    if (!leilaoAberto) {
                        log("AVISO | Nenhum leilão aberto no momento.");
                        continue;
                    }
                    leilaoAberto = false;
                }
                String vencedor = licitanteAtual != null ? licitanteAtual : "ninguém";
                registrar("ENCERRADO", "vencedor", vencedor, "valor", String.valueOf(lanceAtual));
                broadcast("WINNER:" + vencedor + ":" + lanceAtual);
                log("──────────────────────────────────────────");
                log("LEILÃO ENCERRADO | Vencedor: " + vencedor + " | Valor: R$" + lanceAtual);
                log("──────────────────────────────────────────");
                salvarHistorico();

            } else if (linha.equalsIgnoreCase("STATUS")) {
                synchronized (leilaoLock) {
                    if (!leilaoAberto) {
                        log("STATUS | Nenhum leilão aberto.");
                    } else {
                        log("STATUS | Item: " + itemNome +
                                " | Lance atual: R$" + lanceAtual +
                                " | Licitante: " + (licitanteAtual != null ? licitanteAtual : "nenhum"));
                    }
                }
            } else {
                log("ERRO | Comando desconhecido. Use ADD, CLOSE ou STATUS.");
            }
        }
    }
}

class ClientHandler implements Runnable {

    private final Socket socket;
    private PrintWriter out;
    private String nome = null;

    ClientHandler(Socket socket) {
        this.socket = socket;
    }

    void send(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println("CONNECT:OK");

            String linha;
            while ((linha = in.readLine()) != null) {
                linha = linha.trim();

                if (linha.startsWith("NAME:")) {
                    nome = linha.substring(5).trim();
                    if (nome.isEmpty()) {
                        send("ERROR:Nome não pode ser vazio");
                        nome = null;
                        continue;
                    }
                    send("WELCOME:" + nome);
                    Server.registrar("CONNECT", "cliente", nome);
                    Server.log("CONNECT | Cliente: " + nome);

                    // Informa o estado atual do leilão ao novo participante
                    synchronized (Server.leilaoLock) {
                        if (Server.leilaoAberto) {
                            send("ITEM:" + Server.itemNome + ":" + Server.lanceAtual);
                            if (Server.licitanteAtual != null) {
                                send("NEW_BID:" + Server.licitanteAtual + ":" + Server.lanceAtual);
                            }
                        }
                    }

                } else if (linha.startsWith("BID:")) {
                    if (nome == null) {
                        send("ERROR:Registre seu nome primeiro com NAME:<seu_nome>");
                        continue;
                    }
                    try {
                        double valor = Double.parseDouble(linha.substring(4).trim());
                        synchronized (Server.leilaoLock) {
                            if (!Server.leilaoAberto) {
                                send("ERROR:Nenhum leilão aberto no momento");
                            } else if (valor <= Server.lanceAtual) {
                                send("ERROR:Lance deve ser maior que R$" + Server.lanceAtual);
                                Server.log("LANCE RECUSADO | Cliente: " + nome + " | Valor: R$" + valor + " (mínimo: R$" + Server.lanceAtual + ")");
                            } else {
                                Server.lanceAtual = valor;
                                Server.licitanteAtual = nome;
                                Server.registrar("NEW_BID", "cliente", nome, "valor", String.valueOf(valor));
                                Server.log("NEW_BID | Cliente: " + nome + " | Valor: R$" + valor);
                                Server.broadcast("NEW_BID:" + nome + ":" + valor);
                            }
                        }
                    } catch (NumberFormatException e) {
                        send("ERROR:Valor inválido. Use um número (ex: BID:150.0)");
                    }

                } else {
                    send("ERROR:Comando desconhecido. Use NAME:<nome> ou BID:<valor>");
                }
            }
        } catch (IOException e) {
            // cliente desconectou
        } finally {
            Server.clientes.remove(this);
            if (nome != null) {
                Server.registrar("DISCONNECT", "cliente", nome);
                Server.log("DISCONNECT | Cliente: " + nome);
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}

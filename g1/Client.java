import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    static final String HOST_PADRAO = "127.0.0.1";
    static final int PORTA_PADRAO = 6000;

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : HOST_PADRAO;
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : PORTA_PADRAO;

        Socket socket;
        try {
            socket = new Socket(host, porta);
        } catch (IOException e) {
            System.out.println("Erro ao conectar em " + host + ":" + porta + " -> " + e.getMessage());
            return;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        Scanner scanner = new Scanner(System.in);

        // Thread para receber mensagens do servidor em tempo real
        Thread receiver = new Thread(() -> {
            try {
                String linha;
                while ((linha = in.readLine()) != null) {
                    exibirMensagem(linha);
                }
            } catch (IOException e) {
                System.out.println("\n[Conexão encerrada pelo servidor]");
            }
        });
        receiver.setDaemon(true);
        receiver.start();

        // Registro do participante
        System.out.print("Digite seu nome: ");
        String nome = scanner.nextLine().trim();
        out.println("NAME:" + nome);

        // Interface principal
        System.out.println("\nComandos disponíveis:");
        System.out.println("  BID <valor>   -> Enviar um lance (ex: BID 250.0)");
        System.out.println("  SAIR          -> Encerrar conexão");
        System.out.println();

        while (scanner.hasNextLine()) {
            String entrada = scanner.nextLine().trim();

            if (entrada.equalsIgnoreCase("SAIR")) {
                break;
            } else if (entrada.toUpperCase().startsWith("BID ")) {
                String valor = entrada.substring(4).trim();
                out.println("BID:" + valor);
            } else if (entrada.isEmpty()) {
                // ignora linhas em branco
            } else {
                System.out.println("[Comando inválido] Use BID <valor> ou SAIR");
            }
        }

        socket.close();
        System.out.println("Desconectado.");
    }

    // Exibe mensagens recebidas do servidor com formatação
    static void exibirMensagem(String msg) {
        if (msg.startsWith("CONNECT:OK")) {
            System.out.println("[Conectado ao servidor com sucesso]");

        } else if (msg.startsWith("WELCOME:")) {
            System.out.println("[Registrado como: " + msg.substring(8) + "]");

        } else if (msg.startsWith("ITEM:")) {
            String[] partes = msg.substring(5).split(":");
            System.out.println();
            System.out.println("╔══════════════════════════════════╗");
            System.out.println("║        LEILÃO ABERTO             ║");
            System.out.printf( "║  Item:  %-24s ║%n", partes[0]);
            System.out.printf( "║  Lance inicial: R$%-14s ║%n", partes[1]);
            System.out.println("╚══════════════════════════════════╝");

        } else if (msg.startsWith("NEW_BID:")) {
            String[] partes = msg.substring(8).split(":");
            System.out.printf("  >> Novo lance: %-15s ofereceu R$%s%n", partes[0], partes[1]);

        } else if (msg.startsWith("WINNER:")) {
            String[] partes = msg.substring(7).split(":");
            System.out.println();
            System.out.println("╔══════════════════════════════════╗");
            System.out.println("║       LEILÃO ENCERRADO           ║");
            System.out.printf( "║  Vencedor: %-21s ║%n", partes[0]);
            System.out.printf( "║  Valor:    R$%-19s ║%n", partes[1]);
            System.out.println("╚══════════════════════════════════╝");

        } else if (msg.startsWith("ERROR:")) {
            System.out.println("[ERRO] " + msg.substring(6));

        } else {
            System.out.println(msg);
        }
    }
}

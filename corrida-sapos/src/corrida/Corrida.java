package corrida;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Corrida {

    private static final int LARGURA_BARRA = 40;
    private static final long INTERVALO_EXIBICAO_MS = 1000;
    private static final String ANSI_RESET  = "\u001B[0m";
    private static final String ANSI_GREEN  = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN   = "\u001B[36m";
    private static final String ANSI_BOLD   = "\u001B[1m";

    private final int distanciaTotal;
    private final List<Sapo> sapos;
    private final AtomicReference<Sapo> vencedor;
    private long ultimaExibicao = 0;

    public Corrida(int distanciaTotal) {
        this.distanciaTotal = distanciaTotal;
        this.sapos = new ArrayList<>();
        this.vencedor = new AtomicReference<>(null);
    }

    public void adicionarSapo(Sapo sapo) {
        sapos.add(sapo);
    }

    public void registrarVencedor(Sapo sapo) {
        vencedor.compareAndSet(null, sapo);
    }

    public boolean temVencedor() {
        return vencedor.get() != null;
    }

    public synchronized void exibirProgresso() {
        long agora = System.currentTimeMillis();

        if (agora - ultimaExibicao < INTERVALO_EXIBICAO_MS) {
            return;
        }
        ultimaExibicao = agora;

        System.out.print("\033[H\033[2J");
        System.out.flush();

        System.out.println(ANSI_BOLD + ANSI_CYAN + "╔══════════════════════════════════════════════════╗");
        System.out.println("║           CORRIDA DOS SAPOS  \uD83D\uDC38                    ║");
        System.out.println("╚══════════════════════════════════════════════════╝" + ANSI_RESET);
        System.out.println();

        for (Sapo sapo : sapos) {
            renderizarSapo(sapo);
        }

        System.out.println();
        System.out.println(ANSI_YELLOW + "Distância total: " + distanciaTotal + "m" + ANSI_RESET);

        try {
            Thread.sleep(INTERVALO_EXIBICAO_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void renderizarSapo(Sapo sapo) {
        int posicao = sapo.getPosicao();
        int preenchimento = (int) ((double) posicao / distanciaTotal * LARGURA_BARRA);

        StringBuilder barra = new StringBuilder();
        barra.append(ANSI_GREEN).append("[");

        for (int i = 0; i < LARGURA_BARRA; i++) {
            if (i < preenchimento) {
                barra.append("=");
            } else if (i == preenchimento && posicao < distanciaTotal) {
                barra.append(ANSI_YELLOW).append(">").append(ANSI_GREEN);
            } else {
                barra.append(" ");
            }
        }

        barra.append("]").append(ANSI_RESET);

        String marcador = sapo.isTerminou() ? " \uD83C\uDFC6" : "";
        System.out.printf("%-12s %s %3d/%dm%s%n",
                sapo.getNome() + ":", barra, posicao, distanciaTotal, marcador);
    }

    public void iniciar() throws InterruptedException {
        if (sapos.isEmpty()) {
            System.out.println("Nenhum sapo foi adicionado à corrida!");
            return;
        }

        exibirContagem();

        for (Sapo sapo : sapos) {
            sapo.start();
        }

        for (Sapo sapo : sapos) {
            sapo.join();
        }

        exibirResultadoFinal();
    }

    private void exibirContagem() throws InterruptedException {
        System.out.print("\033[H\033[2J");
        System.out.flush();

        System.out.println(ANSI_BOLD + ANSI_CYAN + "\n  CORRIDA DOS SAPOS  \uD83D\uDC38\n" + ANSI_RESET);
        System.out.println("Participantes:");

        for (Sapo sapo : sapos) {
            System.out.println("  \uD83D\uDC38 " + sapo.getNome());
        }

        System.out.println("\nDistância: " + distanciaTotal + " metros\n");

        for (int i = 3; i >= 1; i--) {
            System.out.println(ANSI_BOLD + ANSI_YELLOW + "  A corrida começa em " + i + "..." + ANSI_RESET);
            Thread.sleep(1000);
        }

        System.out.println(ANSI_BOLD + ANSI_GREEN + "\n  JÁ!!!\n" + ANSI_RESET);
        Thread.sleep(500);
    }

    private void exibirResultadoFinal() {
        ultimaExibicao = 0;
        exibirProgresso();

        Sapo campeao = vencedor.get();
        System.out.println();
        System.out.println(ANSI_BOLD + ANSI_CYAN + "╔══════════════════════════════════════════════════╗");
        System.out.printf( "║  \uD83C\uDFC6  VENCEDOR: %-34s ║%n", campeao.getNome() + "!");
        System.out.println("╚══════════════════════════════════════════════════╝" + ANSI_RESET);
    }
}

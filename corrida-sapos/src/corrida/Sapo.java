package corrida;

import java.util.Random;

public class Sapo extends Thread {

    private final String nome;
    private final int distanciaTotal;
    private final Corrida corrida;
    private final Random random;

    private volatile int posicao;
    private volatile boolean terminou;

    public Sapo(String nome, int distanciaTotal, Corrida corrida) {
        super(nome);
        this.nome = nome;
        this.distanciaTotal = distanciaTotal;
        this.corrida = corrida;
        this.random = new Random();
        this.posicao = 0;
        this.terminou = false;
    }

    @Override
    public void run() {
        while (!terminou && !corrida.temVencedor()) {
            int avanco = random.nextInt(5) + 1;
            posicao = Math.min(posicao + avanco, distanciaTotal);

            corrida.exibirProgresso();

            if (posicao >= distanciaTotal) {
                terminou = true;
                corrida.registrarVencedor(this);
                break;
            }

            try {
                long pausa = 100 + random.nextInt(401);
                Thread.sleep(pausa);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public String getNome() {
        return nome;
    }

    public int getPosicao() {
        return posicao;
    }

    public int getDistanciaTotal() {
        return distanciaTotal;
    }

    public boolean isTerminou() {
        return terminou;
    }
}

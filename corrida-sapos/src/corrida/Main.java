package corrida;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        final int DISTANCIA = 20;

        Corrida corrida = new Corrida(DISTANCIA);

        corrida.adicionarSapo(new Sapo("Kermit", DISTANCIA, corrida));
        corrida.adicionarSapo(new Sapo("Bobo", DISTANCIA, corrida));
        corrida.adicionarSapo(new Sapo("Perereca", DISTANCIA, corrida));
        corrida.adicionarSapo(new Sapo("Verdão", DISTANCIA, corrida));
        corrida.adicionarSapo(new Sapo("Crocão", DISTANCIA, corrida));

        corrida.iniciar();
    }
}

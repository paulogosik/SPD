# Atividade - Sockets e Multithreading

Servidor de cálculo distribuído em Java, onde múltiplos clientes enviam operações matemáticas via socket e o servidor responde o resultado. Cada cliente é atendido por uma thread separada.

## Estrutura do Projeto

```
atividade_multithreading/
└── src/main/java/com/spd/calculator/
    ├── common/
    │   └── ExpressionEvaluator.java   # Avaliador de expressões matemáticas
    ├── server/
    │   ├── CalculatorServer.java      # Servidor principal (escuta na porta)
    │   └── ClientHandler.java         # Thread que atende cada cliente
    └── client/
        └── CalculatorClient.java      # Cliente que se conecta ao servidor
```

## Como Compilar

A partir da pasta `atividade_multithreading`:

```bash
javac -d out src/main/java/com/spd/calculator/common/*.java src/main/java/com/spd/calculator/server/*.java src/main/java/com/spd/calculator/client/*.java
```

## Como Executar

### 1. Iniciar o servidor (porta padrão 5000)

```bash
java -cp out com.spd.calculator.server.CalculatorServer
```

Ou com porta customizada:

```bash
java -cp out com.spd.calculator.server.CalculatorServer 6000
```

### 2. Iniciar um ou mais clientes (em terminais separados)

```bash
java -cp out com.spd.calculator.client.CalculatorClient
```

Ou apontando para host/porta customizados:

```bash
java -cp out com.spd.calculator.client.CalculatorClient localhost 6000
```

## Exemplos de Uso

Após conectado, o cliente pode digitar:

```
> 2 + 3
Resultado: 5
> 10 / 2
Resultado: 5
> 3 * (4 + 5)
Resultado: 27
> 10 / 0
Resultado: ERRO: Divisão por zero
> sair
```

## Operações Suportadas

- Adição (`+`)
- Subtração (`-`)
- Multiplicação (`*`)
- Divisão (`/`)
- Parênteses `(` e `)`
- Números decimais e negativos

## Arquitetura - Multithreading

- O `CalculatorServer` utiliza um `ExecutorService` com pool fixo de 20 threads.
- Cada conexão aceita pelo `ServerSocket` é encapsulada em um `ClientHandler` (Runnable) e submetida ao pool.
- Isso permite atender múltiplos clientes simultaneamente de forma eficiente, reutilizando threads.

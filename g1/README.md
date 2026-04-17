# G1 — Plataforma de Leilão Eletrônico

## Requisitos

- Java 11+

## Como compilar

```bash
javac Server.java Client.java
```

## Como executar

**1. Inicie o servidor** (deixe rodando):

```bash
java Server
```

**2. Em outros terminais, inicie os clientes:**

```bash
java Client
# ou com IP/porta customizados:
java Client <ip> <porta>
```

## Comandos do servidor

| Comando | Descrição |
|---|---|
| `ADD <item> <preco>` | Cadastra item e abre o leilão |
| `CLOSE` | Encerra o leilão e salva o histórico |
| `STATUS` | Exibe o estado atual do leilão |

## Comandos do cliente

| Comando | Descrição |
|---|---|
| `BID <valor>` | Envia um lance (ex: `BID 250.0`) |
| `SAIR` | Encerra a conexão |

## Histórico

Ao encerrar um leilão com `CLOSE`, os dados são salvos em `historico_leiloes.json`.
Cada leilão encerrado é adicionado ao arquivo sem apagar os anteriores.

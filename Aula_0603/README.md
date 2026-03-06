# Atividade SPD – Cliente-Servidor TCP com criptografia simétrica

Implementação em Python para a atividade da disciplina Sistemas Paralelos e Distribuídos: servidor TCP, cliente TCP, serialização de dicionários (JSON) e simulação de criptografia com uma string como chave.

## Como executar

1. Inicie o servidor (deixe rodando):
   ```bash
   python server.py
   ```
   (Se necessário, use `python3` no lugar de `python`.)

2. Em outro terminal, execute o cliente:
   ```bash
   python client.py
   ```

O servidor usa `127.0.0.1:5000` por padrão. Para alterar host/porta, edite as constantes `HOST` e `PORT` no início de `server.py` e `client.py`.

---

## Explicação do código

### `crypto_simple.py`

Simulação de criptografia: não há algoritmo real, apenas o uso de uma **string como chave**.

- **`encrypt(dados, chave)`**: concatena `chave + "|" + dados` e retorna essa string. Simula “enviar dados protegidos pela chave”.
- **`decrypt(mensagem, chave)`**: verifica se a mensagem começa com `chave + "|"` e, em caso positivo, devolve o trecho após o separador (os “dados”). Simula “só quem tem a chave consegue extrair os dados”.

Objetivo didático: mostrar o fluxo em que o servidor gera uma chave, o cliente a recebe e passa a enviar mensagens “com” essa chave, sem implementar criptografia de verdade.

### `server.py`

- Cria um **socket TCP** (`socket.AF_INET`, `socket.SOCK_STREAM`), faz `bind` e `listen` na porta configurada.
- Em loop, aceita conexões com `accept`. Para cada conexão:
  - **Identifica o cliente** pelo par endereço:porta (`client_id = f"{addr[0]}:{addr[1]}"`).
  - **Gera uma chave única** (string aleatória com `random` e `string`) e envia essa chave ao cliente na primeira mensagem (uma linha terminada em `\n`).
  - Em uma **thread**, fica em loop: recebe mensagens no formato “criptografado” (tamanho em 4 bytes + payload), usa `crypto_simple.decrypt` e `json.loads` para obter o dicionário, processa (monta uma resposta em dict), serializa com `json.dumps`, “criptografa” com `crypto_simple.encrypt` e envia a resposta no mesmo formato.
- O protocolo de mensagens (tamanho + corpo) evita misturar várias mensagens no mesmo fluxo TCP.

### `client.py`

- Cria um socket TCP e **conecta** ao servidor.
- **Recebe a chave** lendo uma linha (`receber_chave`).
- Monta um **dicionário** em Python, serializa com `json.dumps`, “criptografa” com `crypto_simple.encrypt` e envia usando o mesmo protocolo (4 bytes de tamanho + payload).
- **Recebe** a resposta (tamanho + payload), decripta com `crypto_simple.decrypt`, deserializa com `json.loads` e exibe o resultado.

Assim, após receber a chave, todo o tráfego de dados (dicionários) trafega no formato simulado de criptografia.

---

## Associação com a atividade (requisitos do slide)

| Requisito da atividade | Onde está no código |
|------------------------|----------------------|
| **Servidor TCP que escuta e processa dados, retornando resposta** | `server.py`: `bind`, `listen`, `accept`; na thread, `receber_msg`, `decrypt`, `json.loads`, montagem da resposta, `json.dumps`, `encrypt`, `enviar_msg`. |
| **Identificação de cada cliente** | `server.py`: `client_id = f"{addr[0]}:{addr[1]}"` (endereço e porta do cliente). |
| **Chave única por cliente para criptografia simétrica** | `server.py`: `gerar_chave()` ao aceitar a conexão; envio da chave na primeira mensagem; uso da mesma chave em `crypto_simple.encrypt`/`decrypt` nas mensagens daquele cliente. |
| **Cliente envia dicionários Python** | `client.py`: construção do dict, `json.dumps` para serializar; no servidor, `json.loads` para obter o dicionário. |
| **Após receber a chave, cliente envia dados criptografados** | `client.py`: primeiro `receber_chave(conn)`; em seguida, `crypto_simple.encrypt(dados_json, chave)` antes de enviar; servidor usa `crypto_simple.decrypt` para obter o JSON. |

Todos os itens da atividade estão cobertos por essa implementação na pasta `Aula_0603`.

"""
Servidor TCP: escuta conexões, identifica cada cliente, gera chave única por cliente,
processa dados recebidos e retorna resposta (dados em formato simulado de criptografia).
"""

import json
import random
import socket
import string
import struct
import threading

import crypto_simple

HOST = "127.0.0.1"
PORT = 5000


def gerar_chave():
    """Gera uma string aleatória para usar como chave (simulação)."""
    return "".join(random.choices(string.ascii_letters + string.digits, k=32))


def enviar_msg(conn: socket.socket, msg: bytes):
    """Envia mensagem com prefixo de 4 bytes indicando o tamanho."""
    conn.sendall(struct.pack(">I", len(msg)) + msg)


def receber_msg(conn: socket.socket) -> bytes:
    """Recebe mensagem cujo tamanho vem nos primeiros 4 bytes."""
    raw_len = _recv_exatos(conn, 4)
    if not raw_len:
        return b""
    tamanho = struct.unpack(">I", raw_len)[0]
    return _recv_exatos(conn, tamanho)


def _recv_exatos(conn: socket.socket, n: int) -> bytes:
    """Lê exatamente n bytes do socket."""
    buf = b""
    while len(buf) < n:
        pedaco = conn.recv(n - len(buf))
        if not pedaco:
            return b""
        buf += pedaco
    return buf


def atender_cliente(conn: socket.socket, addr: tuple, chave: str):
    """Atende um cliente em uma thread: envia chave, depois processa mensagens."""
    client_id = f"{addr[0]}:{addr[1]}"
    try:
        # 1. Enviar a chave ao cliente (primeira mensagem)
        conn.sendall((chave + "\n").encode("utf-8"))

        while True:
            payload = receber_msg(conn)
            if not payload:
                break
            texto = payload.decode("utf-8")
            # 2. Decriptar (simulação) e deserializar
            dados = crypto_simple.decrypt(texto, chave)
            obj = json.loads(dados)
            print(f"[{client_id}] Mensagem recebida: {obj}")
            print(f"---")
            # 3. Processar e montar resposta (ex.: ecoar com confirmação)
            resposta = {"recebido": True, "dados": obj, "cliente": client_id}
            resposta_serializada = json.dumps(resposta)
            resposta_cripto = crypto_simple.encrypt(resposta_serializada, chave)
            enviar_msg(conn, resposta_cripto.encode("utf-8"))
    except (ConnectionResetError, ValueError, json.JSONDecodeError):
        pass
    finally:
        conn.close()


def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((HOST, PORT))
    server.listen(5)
    print(f"Servidor escutando em {HOST}:{PORT}")
    print(f"---")

    while True:
        conn, addr = server.accept()
        client_id = f"{addr[0]}:{addr[1]}"
        chave = gerar_chave()
        print(f"Cliente conectado: {client_id}, chave gerada.")
        t = threading.Thread(target=atender_cliente, args=(conn, addr, chave))
        t.daemon = True
        t.start()


if __name__ == "__main__":
    main()

"""
Cliente TCP: conecta ao servidor, recebe a chave, envia dicionários
serializados e "criptografados" (simulação).
"""

import json
import socket
import struct

import crypto_simple

HOST = "127.0.0.1"
PORT = 5000


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


def receber_chave(conn: socket.socket) -> str:
    """Lê a chave enviada pelo servidor (uma linha terminada em \\n)."""
    buf = b""
    while b"\n" not in buf:
        buf += conn.recv(1)
    return buf.decode("utf-8").strip()


def main():
    conn = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    conn.connect((HOST, PORT))

    # 1. Receber a chave do servidor
    chave = receber_chave(conn)
    print(f"Chave recebida: {chave[:8]}...")

    # 2. Montar um dicionário, serializar, "criptografar" e enviar
    dados = {"nome": "Cliente", "mensagem": "Olá, servidor!"}
    dados_json = json.dumps(dados)
    payload = crypto_simple.encrypt(dados_json, chave)
    enviar_msg(conn, payload.encode("utf-8"))

    # 3. Receber resposta, decriptar e exibir
    resposta_bytes = receber_msg(conn)
    resposta_texto = resposta_bytes.decode("utf-8")
    resposta_decripto = crypto_simple.decrypt(resposta_texto, chave)
    resposta_obj = json.loads(resposta_decripto)
    print("Resposta do servidor:", resposta_obj)

    conn.close()


if __name__ == "__main__":
    main()

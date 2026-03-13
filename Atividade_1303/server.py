import socket
import threading
import time

HOST = "127.0.0.1"
PORTA = 5000
TIMEOUT_HEARTBEAT = 30

nos = {}
lock_nos = threading.Lock()


def obter_nos_ativos():
    agora = time.time()
    with lock_nos:
        return [
            nid for nid, (ts, _) in nos.items()
            if (agora - ts) <= TIMEOUT_HEARTBEAT
        ]


def expirar_nos_inativos():
    while True:
        time.sleep(5)
        agora = time.time()
        fechar = []
        with lock_nos:
            for nid, (ts, conexao) in list(nos.items()):
                if (agora - ts) > TIMEOUT_HEARTBEAT:
                    fechar.append((nid, conexao))
            for nid, _ in fechar:
                del nos[nid]
        for nid, conexao in fechar:
            try:
                conexao.close()
                print(f"Desconectado por timeout: {nid}")
            except OSError:
                pass


def tratar_cliente(conexao, endereco):
    try:
        buffer = b""
        while True:
            data = conexao.recv(1024)
            if not data:
                break
            buffer += data
            while b"\n" in buffer:
                linha, buffer = buffer.split(b"\n", 1)
                msg = linha.decode("utf-8", errors="replace").strip()
                if not msg:
                    continue
                resposta = processar_mensagem(msg, conexao)
                if resposta is None:
                    return
                conexao.sendall((resposta + "\n").encode("utf-8"))
    except (ConnectionResetError, BrokenPipeError, OSError):
        pass
    finally:
        conexao.close()


def processar_mensagem(msg, conexao):
    print(f"  <- {msg}")
    msg_upper = msg.upper()
    if msg_upper.startswith("REGISTER:"):
        node_id = msg.split(":", 1)[1].strip()
        if not node_id:
            return "ERROR:INVALID_NODE_ID"
        with lock_nos:
            nos[node_id] = (time.time(), conexao)
        return "OK:REGISTERED"

    if msg_upper.startswith("HEARTBEAT:"):
        node_id = msg.split(":", 1)[1].strip()
        with lock_nos:
            if node_id in nos:
                nos[node_id] = (time.time(), nos[node_id][1])
        return "OK:HEARTBEAT"

    if msg_upper == "LIST":
        ativos = obter_nos_ativos()
        return "NODES:" + ",".join(ativos) if ativos else "NODES:"

    if msg_upper.startswith("QUIT:"):
        node_id = msg.split(":", 1)[1].strip()
        with lock_nos:
            nos.pop(node_id, None)
        conexao.sendall("OK:BYE\n".encode("utf-8"))
        return None

    return "ERROR:UNKNOWN_COMMAND"


def main():
    servidor = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    servidor.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    servidor.bind((HOST, PORTA))
    servidor.listen(5)
    print(f"Servidor em {HOST}:{PORTA} (timeout: {TIMEOUT_HEARTBEAT}s)")

    thread_expirar = threading.Thread(target=expirar_nos_inativos)
    thread_expirar.daemon = True
    thread_expirar.start()

    while True:
        conexao, endereco = servidor.accept()
        t = threading.Thread(target=tratar_cliente, args=(conexao, endereco))
        t.daemon = True
        t.start()


if __name__ == "__main__":
    main()

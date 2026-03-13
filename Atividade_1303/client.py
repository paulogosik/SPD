import socket
import sys

HOST_PADRAO = "127.0.0.1"
PORTA_PADRAO = 5000
NO_PADRAO = "no1"


def enviar_comando(sock, cmd):
    sock.sendall((cmd + "\n").encode("utf-8"))
    data = b""
    while b"\n" not in data:
        chunk = sock.recv(1024)
        if not chunk:
            raise ConnectionError("Conexão encerrada pelo servidor")
        data += chunk
    return data.decode("utf-8").strip()


def main():
    host = sys.argv[1] if len(sys.argv) > 1 else HOST_PADRAO
    porta = int(sys.argv[2]) if len(sys.argv) > 2 else PORTA_PADRAO
    no_id = sys.argv[3] if len(sys.argv) > 3 else NO_PADRAO

    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((host, porta))
    except OSError as e:
        print(f"Erro ao conectar em {host}:{porta}: {e}")
        sys.exit(1)

    try:
        resp = enviar_comando(sock, f"REGISTER:{no_id}")
        print(f"REGISTER -> {resp}")
        if not resp.startswith("OK:"):
            print("Falha no registro.")
            return

        while True:
            print("\n1=HEARTBEAT  2=LISTAR  3=SAIR")
            op = input("Opção (1/2/3): ").strip()

            if op == "1":
                resp = enviar_comando(sock, f"HEARTBEAT:{no_id}")
                print(f"HEARTBEAT -> {resp}")
            elif op == "2":
                resp = enviar_comando(sock, "LIST")
                print(f"LIST -> {resp}")
                if resp.startswith("NODES:"):
                    lista = resp[6:] or "(nenhum)"
                    print(f"  Nós ativos: {lista}")
            elif op == "3":
                enviar_comando(sock, f"QUIT:{no_id}")
                print("Desconectado.")
                break
            else:
                print("Opção inválida.")
    except ConnectionError as e:
        print(e)
    finally:
        sock.close()


if __name__ == "__main__":
    main()

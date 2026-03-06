"""
Simulação de criptografia: usa apenas uma string como chave.
Não é criptografia real — apenas prefixo chave + separador + dados.
"""

SEPARADOR = "|"


def encrypt(dados: str, chave: str) -> str:
    """Simula criptografia: concatena chave + separador + dados."""
    return chave + SEPARADOR + dados


def decrypt(mensagem: str, chave: str) -> str:
    """Simula decriptografia: verifica o prefixo (chave) e devolve os dados."""
    prefixo_esperado = chave + SEPARADOR
    if not mensagem.startswith(prefixo_esperado):
        raise ValueError("Chave inválida ou mensagem corrompida")
    return mensagem[len(prefixo_esperado) :]

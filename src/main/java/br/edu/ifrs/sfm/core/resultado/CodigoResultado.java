package br.edu.ifrs.sfm.core.resultado;

/** Resultado de uma operação, sem texto de interface: cada UI traduz pelo seu catálogo. */
public enum CodigoResultado {
    SUCESSO,
    SENHA_FRACA,
    CONFIRMACAO_DIVERGENTE,
    AUTENTICACAO_FALHOU,
    ARQUIVO_NAO_ENCONTRADO,
    ARQUIVO_MUITO_GRANDE,
    PASTA_VAZIA,
    ERRO_DE_GRAVACAO,
    OPERACAO_CANCELADA,
    FORMATO_INVALIDO
}

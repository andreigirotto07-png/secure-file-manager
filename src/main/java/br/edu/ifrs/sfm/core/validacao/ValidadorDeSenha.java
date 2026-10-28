package br.edu.ifrs.sfm.core.validacao;

import br.edu.ifrs.sfm.core.resultado.CodigoResultado;

import java.util.Arrays;

public final class ValidadorDeSenha {
    public static final int TAMANHO_MINIMO = 8;

    private ValidadorDeSenha() {}

    /** Retorna SUCESSO, SENHA_FRACA ou CONFIRMACAO_DIVERGENTE. */
    public static CodigoResultado validar(char[] senha, char[] confirmacao) {
        if (senha == null || senha.length < TAMANHO_MINIMO) return CodigoResultado.SENHA_FRACA;
        if (confirmacao == null || !Arrays.equals(senha, confirmacao)) return CodigoResultado.CONFIRMACAO_DIVERGENTE;
        return CodigoResultado.SUCESSO;
    }
}

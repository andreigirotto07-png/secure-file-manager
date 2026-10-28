package br.edu.ifrs.sfm.core.validacao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ForcaDeSenhaTest {

    @Test
    void forca_da_senha_classifica_exemplos() {
        assertEquals(ForcaDeSenha.VAZIA, ForcaDeSenha.avaliar("".toCharArray()));
        assertEquals(ForcaDeSenha.FRACA, ForcaDeSenha.avaliar("abc1".toCharArray()));
        assertEquals(ForcaDeSenha.FRACA, ForcaDeSenha.avaliar("12345678".toCharArray()));
        assertEquals(ForcaDeSenha.FRACA, ForcaDeSenha.avaliar("aaaaaaaaaa".toCharArray()));
        assertEquals(ForcaDeSenha.MEDIA, ForcaDeSenha.avaliar("gatinho42".toCharArray()));
        assertEquals(ForcaDeSenha.FORTE, ForcaDeSenha.avaliar("Cafe&Pao2026!".toCharArray()));
        assertEquals(ForcaDeSenha.FORTE, ForcaDeSenha.avaliar("minhacasatemquintal".toCharArray()));
    }

    @Test
    void validador_exige_minimo_e_confirmacao() {
        assertEquals(br.edu.ifrs.sfm.core.resultado.CodigoResultado.SENHA_FRACA, ValidadorDeSenha.validar("curta".toCharArray(), "curta".toCharArray()));
        assertEquals(br.edu.ifrs.sfm.core.resultado.CodigoResultado.CONFIRMACAO_DIVERGENTE, ValidadorDeSenha.validar("senha-forte-1".toCharArray(), "senha-forte-2".toCharArray()));
        assertEquals(br.edu.ifrs.sfm.core.resultado.CodigoResultado.SUCESSO, ValidadorDeSenha.validar("senha-forte-1".toCharArray(), "senha-forte-1".toCharArray()));
    }
}

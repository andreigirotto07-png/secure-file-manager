package br.edu.ifrs.sfm.core.validacao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LimitesDeArquivoTest {

    @Test
    void limite_e_exibido_como_texto() {
        assertEquals("100 MB", LimitesDeArquivo.tamanhoMaximoTexto());
        assertTrue(LimitesDeArquivo.excede(LimitesDeArquivo.TAMANHO_MAXIMO_BYTES + 1));
        assertFalse(LimitesDeArquivo.excede(LimitesDeArquivo.TAMANHO_MAXIMO_BYTES));
    }
}

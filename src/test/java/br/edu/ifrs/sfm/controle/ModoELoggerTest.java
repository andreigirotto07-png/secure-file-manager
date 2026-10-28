package br.edu.ifrs.sfm.controle;

import br.edu.ifrs.sfm.core.resultado.CodigoResultado;
import br.edu.ifrs.sfm.teste.EventLogger;
import br.edu.ifrs.sfm.teste.TipoEvento;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ModoELoggerTest {
    @TempDir Path dir;

    @Test
    void troca_de_modo_e_registrada_quando_permitida() {
        EventLogger log = new EventLogger();
        GerenciadorDeModo g = new GerenciadorDeModo(ModoInterface.TAREFAS, true, log);
        assertTrue(g.alternar());
        assertEquals(ModoInterface.TECNICA, g.modoAtual());
        assertEquals(TipoEvento.TROCA_DE_MODO, log.eventos().get(0).tipo());
    }

    @Test
    void troca_e_bloqueada_no_modo_de_teste() {
        EventLogger log = new EventLogger();
        GerenciadorDeModo g = new GerenciadorDeModo(ModoInterface.TECNICA, false, log);
        assertFalse(g.alternar());
        assertEquals(ModoInterface.TECNICA, g.modoAtual());
        assertTrue(log.eventos().isEmpty());
    }

    @Test
    void csv_tem_cabecalho_e_nao_guarda_dados_pessoais() throws IOException {
        EventLogger log = new EventLogger();
        log.definirTarefa("T5");
        log.registrar(TipoEvento.ERRO_EXIBIDO, ModoInterface.TAREFAS, CodigoResultado.AUTENTICACAO_FALHOU);
        Path csv = dir.resolve("eventos.csv");
        log.exportarCsv(csv);
        String texto = Files.readString(csv);
        assertTrue(texto.startsWith("instante,tipo,modo,tarefa,codigo"));
        assertTrue(texto.contains("ERRO_EXIBIDO,TAREFAS,T5,AUTENTICACAO_FALHOU"));
    }
}

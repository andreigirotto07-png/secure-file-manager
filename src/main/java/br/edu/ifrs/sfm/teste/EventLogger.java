package br.edu.ifrs.sfm.teste;

import br.edu.ifrs.sfm.controle.ModoInterface;
import br.edu.ifrs.sfm.core.resultado.CodigoResultado;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Registra eventos de uso para análise (tempo, erros, tentativas). Exporta CSV. */
public final class EventLogger {
    private final List<EventoTeste> eventos = Collections.synchronizedList(new ArrayList<>());
    private volatile String tarefaAtual = "";

    public void definirTarefa(String tarefa) { this.tarefaAtual = tarefa == null ? "" : tarefa; }

    public void registrar(TipoEvento tipo, ModoInterface modo, CodigoResultado codigo) {
        eventos.add(new EventoTeste(Instant.now(), tipo, modo, tarefaAtual, codigo));
    }

    public void registrar(TipoEvento tipo, ModoInterface modo) { registrar(tipo, modo, null); }

    public List<EventoTeste> eventos() {
        synchronized (eventos) { return List.copyOf(eventos); }
    }

    public void exportarCsv(Path destino) throws IOException {
        StringBuilder sb = new StringBuilder("instante,tipo,modo,tarefa,codigo\n");
        for (EventoTeste e : eventos()) {
            sb.append(e.instante()).append(',').append(e.tipo()).append(',').append(e.modo()).append(',')
              .append(e.tarefa()).append(',').append(e.codigo() == null ? "" : e.codigo()).append('\n');
        }
        Files.writeString(destino, sb.toString(), StandardCharsets.UTF_8);
    }
}

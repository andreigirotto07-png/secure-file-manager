package br.edu.ifrs.sfm.controle;

import br.edu.ifrs.sfm.teste.EventLogger;
import br.edu.ifrs.sfm.teste.TipoEvento;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Guarda o modo atual. Durante os testes a troca fica bloqueada (o pesquisador define o modo inicial). */
public final class GerenciadorDeModo {
    private ModoInterface modo;
    private final boolean trocaPermitida;
    private final EventLogger logger;
    private final List<Consumer<ModoInterface>> ouvintes = new ArrayList<>();

    public GerenciadorDeModo(ModoInterface modoInicial, boolean trocaPermitida, EventLogger logger) {
        this.modo = modoInicial;
        this.trocaPermitida = trocaPermitida;
        this.logger = logger;
    }

    public ModoInterface modoAtual() { return modo; }
    public boolean trocaPermitida() { return trocaPermitida; }
    public void aoTrocar(Consumer<ModoInterface> ouvinte) { ouvintes.add(ouvinte); }

    /** Alterna entre os modos; retorna false se a troca estiver bloqueada. */
    public boolean alternar() {
        if (!trocaPermitida) return false;
        modo = modo == ModoInterface.TECNICA ? ModoInterface.TAREFAS : ModoInterface.TECNICA;
        logger.registrar(TipoEvento.TROCA_DE_MODO, modo);
        ouvintes.forEach(o -> o.accept(modo));
        return true;
    }
}

package br.edu.ifrs.sfm.ui;

import br.edu.ifrs.sfm.controle.GerenciadorDeModo;
import br.edu.ifrs.sfm.controle.ModoInterface;
import br.edu.ifrs.sfm.controle.OperacaoControlador;
import br.edu.ifrs.sfm.ui.tarefas.InterfaceTarefas;
import br.edu.ifrs.sfm.ui.tecnica.InterfaceTecnica;

import javafx.scene.Scene;
import javafx.stage.Stage;

/** Mostra a interface do modo atual e a substitui quando o modo muda. */
public final class JanelaPrincipal {
    private final Stage stage;
    private final GerenciadorDeModo modo;
    private final OperacaoControlador controlador;
    private final Scene cena;
    private InterfaceUsuario atual;

    public JanelaPrincipal(Stage stage, GerenciadorDeModo modo, OperacaoControlador controlador) {
        this.stage = stage;
        this.modo = modo;
        this.controlador = controlador;
        this.cena = new Scene(new javafx.scene.layout.StackPane(), 960, 620);
        cena.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        modo.aoTrocar(m -> mostrar());
    }

    public void abrir() {
        stage.setScene(cena);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        mostrar();
        stage.show();
    }

    public void fechar() {
        if (atual != null) atual.encerrar();
    }

    private void mostrar() {
        if (atual != null) atual.encerrar();
        atual = modo.modoAtual() == ModoInterface.TECNICA
                ? new InterfaceTecnica(controlador, modo, stage)
                : new InterfaceTarefas(controlador, modo, stage);
        stage.setTitle(modo.modoAtual() == ModoInterface.TECNICA
                ? "Secure File Manager — Modo técnico" : "Secure File Manager");
        cena.setRoot(atual.raiz());
        atual.exibir();
    }
}

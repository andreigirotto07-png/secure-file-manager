package br.edu.ifrs.sfm.ui.componentes;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Botão único "adicionar": o usuário escolhe um arquivo ou uma pasta no mesmo diálogo. */
public final class SeletorDeItem {
    private SeletorDeItem() {}

    public static void escolher(Stage dono, String titulo, Consumer<Path> aoEscolher) {
        Node raiz = dono.getScene().getRoot();
        raiz.setDisable(true); // enquanto o diálogo está aberto, a janela do programa fica bloqueada
        Thread t = new Thread(() -> {
            File f;
            boolean falhou = false;
            try {
                f = SeletorSwing.escolher(titulo);
            } catch (Throwable e) {
                f = null;
                falhou = true;
            }
            File resultado = f;
            boolean usarAlternativo = falhou;
            Platform.runLater(() -> {
                raiz.setDisable(false);
                if (usarAlternativo) { // se o diálogo único não estiver disponível, usa o seletor comum de arquivos
                    FileChooser fc = new FileChooser();
                    fc.setTitle(titulo);
                    File alt = fc.showOpenDialog(dono);
                    if (alt != null) aoEscolher.accept(alt.toPath());
                } else if (resultado != null) {
                    aoEscolher.accept(resultado.toPath());
                }
            });
        }, "seletor-de-item");
        t.setDaemon(true);
        t.start();
    }
}

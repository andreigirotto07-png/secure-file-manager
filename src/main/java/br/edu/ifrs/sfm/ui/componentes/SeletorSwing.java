package br.edu.ifrs.sfm.ui.componentes;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Diálogo único que aceita arquivo OU pasta (os seletores nativos do JavaFX escolhem só um dos dois).
 * Deve ser chamado fora da thread do JavaFX: bloqueia até o usuário fechar o diálogo.
 */
final class SeletorSwing {
    private SeletorSwing() {}

    static File escolher(String titulo) throws Exception {
        AtomicReference<File> escolhido = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignorada) { }
            JDialog pai = new JDialog();      // pai invisível e "sempre no topo" evita o diálogo ficar atrás da janela
            pai.setAlwaysOnTop(true);
            pai.setLocationRelativeTo(null);
            try {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle(titulo);
                fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                fc.setApproveButtonText("Adicionar");
                fc.setApproveButtonToolTipText("Adiciona o arquivo ou a pasta selecionada");
                if (fc.showOpenDialog(pai) == JFileChooser.APPROVE_OPTION) escolhido.set(fc.getSelectedFile());
            } finally {
                pai.dispose();
            }
        });
        return escolhido.get();
    }
}

package br.edu.ifrs.sfm.ui;

import javafx.scene.Parent;

/** Contrato comum às duas interfaces: ambas usam o mesmo núcleo e o mesmo controlador. */
public interface InterfaceUsuario {
    Parent raiz();
    /** Chamado quando a interface passa a ser mostrada. */
    void exibir();
    /** Chamado antes de a interface ser substituída ou fechada. */
    void encerrar();
}

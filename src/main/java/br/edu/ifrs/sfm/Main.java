package br.edu.ifrs.sfm;

/**
 * Ponto de entrada. Não herda de Application de propósito: assim o programa abre
 * também ao ser executado pelo botão Run do VS Code/IDE (sem module path do JavaFX).
 */
public final class Main {
    public static void main(String[] args) { App.main(args); }
}

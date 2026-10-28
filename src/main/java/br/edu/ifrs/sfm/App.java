package br.edu.ifrs.sfm;

import br.edu.ifrs.sfm.controle.GerenciadorDeModo;
import br.edu.ifrs.sfm.controle.ModoInterface;
import br.edu.ifrs.sfm.controle.OperacaoControlador;
import br.edu.ifrs.sfm.core.cripto.ProtecaoAesGcmService;
import br.edu.ifrs.sfm.teste.EventLogger;
import br.edu.ifrs.sfm.ui.JanelaPrincipal;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.List;

/**
 * Argumentos (usados pelo pesquisador):
 *   --modo=tecnico|tarefas   interface inicial (padrão: técnica)
 *   --teste                  bloqueia e esconde o botão de troca de modo
 *   --tarefa=T1              rótulo da tarefa gravado no registro de eventos
 *   --log=arquivo.csv        exporta o registro de eventos ao fechar (com --teste e sem --log, grava em Documentos)
 */
public class App extends Application {
    private OperacaoControlador controlador;
    private JanelaPrincipal janela;
    private EventLogger logger;
    private Path arquivoLog;

    @Override
    public void start(Stage stage) {
        List<String> args = getParameters().getRaw();
        ModoInterface inicial = ModoInterface.TECNICA;
        boolean teste = false;
        logger = new EventLogger();
        // Propriedades -Dsfm.* permitem fixar a configuração dentro de um instalador (jpackage --java-options).
        if ("tarefas".equals(System.getProperty("sfm.modo"))) inicial = ModoInterface.TAREFAS;
        else if ("tecnico".equals(System.getProperty("sfm.modo"))) inicial = ModoInterface.TECNICA;
        teste = Boolean.getBoolean("sfm.teste");
        String propLog = System.getProperty("sfm.log");
        if (propLog != null && !propLog.isBlank()) arquivoLog = Path.of(propLog);
        String propTarefa = System.getProperty("sfm.tarefa");
        if (propTarefa != null) logger.definirTarefa(propTarefa);
        for (String a : args) {
            if (a.equals("--modo=tecnico")) inicial = ModoInterface.TECNICA;
            else if (a.equals("--modo=tarefas")) inicial = ModoInterface.TAREFAS;
            else if (a.equals("--teste")) teste = true;
            else if (a.startsWith("--tarefa=")) logger.definirTarefa(a.substring("--tarefa=".length()));
            else if (a.startsWith("--log=")) arquivoLog = Path.of(a.substring("--log=".length()));
        }
        if (teste && arquivoLog == null) arquivoLog = registroPadrao(inicial);
        GerenciadorDeModo modo = new GerenciadorDeModo(inicial, !teste, logger);
        controlador = new OperacaoControlador(new ProtecaoAesGcmService(), logger, modo, Platform::runLater);
        janela = new JanelaPrincipal(stage, modo, controlador);
        janela.abrir();
    }

    /** Nos testes sem --log, grava o CSV em Documentos/SecureFileManager-testes (a pasta de instalação pode ser somente leitura). */
    private static Path registroPadrao(ModoInterface modo) {
        Path pasta = Path.of(System.getProperty("user.home"), "Documents", "SecureFileManager-testes");
        String carimbo = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return pasta.resolve("registro_" + modo.name().toLowerCase() + "_" + carimbo + ".csv");
    }

    @Override
    public void stop() throws Exception {
        if (janela != null) janela.fechar();
        if (controlador != null) controlador.encerrar();
        if (arquivoLog != null && logger != null) {
            if (arquivoLog.getParent() != null) java.nio.file.Files.createDirectories(arquivoLog.getParent());
            logger.exportarCsv(arquivoLog);
        }
    }

    public static void main(String[] args) { launch(args); }
}

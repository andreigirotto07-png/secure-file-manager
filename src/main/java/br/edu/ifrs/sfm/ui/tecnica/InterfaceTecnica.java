package br.edu.ifrs.sfm.ui.tecnica;

import br.edu.ifrs.sfm.controle.GerenciadorDeModo;
import br.edu.ifrs.sfm.controle.OperacaoControlador;
import br.edu.ifrs.sfm.core.arquivo.FormatoArquivoProtegido;
import br.edu.ifrs.sfm.core.arquivo.InfoDeEntrada;
import br.edu.ifrs.sfm.core.cripto.AlgoritmoCifra;
import br.edu.ifrs.sfm.core.cripto.DerivacaoDeChave;
import br.edu.ifrs.sfm.core.resultado.CodigoResultado;
import br.edu.ifrs.sfm.core.resultado.Resultado;
import br.edu.ifrs.sfm.core.resultado.ResumoOperacao;
import br.edu.ifrs.sfm.core.validacao.ForcaDeSenha;
import br.edu.ifrs.sfm.core.validacao.LimitesDeArquivo;
import br.edu.ifrs.sfm.core.validacao.ValidadorDeSenha;
import br.edu.ifrs.sfm.ui.InterfaceUsuario;
import br.edu.ifrs.sfm.ui.ajuda.JanelaAjuda;
import br.edu.ifrs.sfm.ui.componentes.IndicadorDeForca;
import br.edu.ifrs.sfm.ui.componentes.SeletorDeItem;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Interface técnica, no estilo de gerenciadores como o VeraCrypt: opções expostas, log de operação. */
public final class InterfaceTecnica implements InterfaceUsuario {
    private final CatalogoTecnico cat = new CatalogoTecnico();
    private final OperacaoControlador ctrl;
    private final GerenciadorDeModo modo;
    private final Stage stage;

    private final BorderPane raiz = new BorderPane();
    private final RadioButton rbCriptografar = new RadioButton("Criptografar");
    private final RadioButton rbDescriptografar = new RadioButton("Descriptografar");
    private final TextField campoEntrada = new TextField();
    private final TextField campoSaida = new TextField();
    private final Label legenda = new Label();
    private final Label tamanhoArquivo = new Label();
    private final ComboBox<AlgoritmoCifra> algoritmo = new ComboBox<>();
    private final Label rotuloAlgoritmo = new Label("Algoritmo");
    private final ComboBox<DerivacaoDeChave> derivacao = new ComboBox<>();
    private final Label rotuloDerivacao = new Label("Derivação de chave");
    private final IndicadorDeForca forca = new IndicadorDeForca(cat);
    private final PasswordField senha = new PasswordField();
    private final PasswordField confirmacao = new PasswordField();
    private final Label rotuloConfirmacao = new Label("Confirmar senha");
    private final CheckBox apagarOriginal = new CheckBox("Apagar o arquivo original após criptografar");
    private final ProgressBar progresso = new ProgressBar(0);
    private final TextArea log = new TextArea();
    private final Button executar = new Button("Executar");
    private final Button cancelar = new Button("Cancelar");
    private final Label status = new Label("Pronto.");

    public InterfaceTecnica(OperacaoControlador ctrl, GerenciadorDeModo modo, Stage stage) {
        this.ctrl = ctrl;
        this.modo = modo;
        this.stage = stage;
        montar();
    }

    private void montar() {
        raiz.getStyleClass().add("tecnica");

        // Barra de título com botão de troca de modo (somente fora do modo de teste)
        Label titulo = new Label("Secure File Manager — Modo técnico");
        titulo.getStyleClass().add("t-titulo");
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox topo = new HBox(10, titulo, espaco);
        topo.setAlignment(Pos.CENTER_LEFT);
        topo.setPadding(new Insets(8, 14, 8, 14));
        topo.getStyleClass().add("t-barra");
        Button ajuda = new Button("Ajuda");
        ajuda.getStyleClass().add("t-ajuda");
        ajuda.setOnAction(e -> { ctrl.ajudaAberta(); JanelaAjuda.exibir(stage, cat.ajuda(), "tecnica"); });
        topo.getChildren().add(ajuda);
        if (modo.trocaPermitida()) {
            Button trocar = new Button("Modo simples (guiado por tarefas)");
            trocar.getStyleClass().add("t-trocar");
            trocar.setOnAction(e -> modo.alternar());
            topo.getChildren().add(trocar);
        }
        raiz.setTop(topo);

        // Formulário
        ToggleGroup grupo = new ToggleGroup();
        rbCriptografar.setToggleGroup(grupo);
        rbDescriptografar.setToggleGroup(grupo);
        rbCriptografar.setSelected(true);
        HBox operacao = new HBox(20, rbCriptografar, rbDescriptografar);

        Button procurarEntrada = new Button("Procurar...");
        procurarEntrada.setOnAction(e -> escolherEntrada());
        legenda.setText(cat.legendaTamanhoMaximo());
        legenda.getStyleClass().add("t-legenda");
        legenda.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        campoEntrada.setPromptText(cat.dicaArrastar());
        VBox colunaEntrada = new VBox(2, procurarEntrada, legenda);
        colunaEntrada.setAlignment(Pos.TOP_LEFT);

        Button procurarSaida = new Button("Procurar...");
        procurarSaida.setOnAction(e -> escolherSaida());

        algoritmo.getItems().addAll(AlgoritmoCifra.values());
        algoritmo.getSelectionModel().select(AlgoritmoCifra.PADRAO);
        tamanhoArquivo.getStyleClass().add("t-legenda");
        campoEntrada.textProperty().addListener((o, a, b) -> atualizarTamanho());
        senha.textProperty().addListener((o, a, b) -> forca.atualizar(ForcaDeSenha.avaliar(b.toCharArray())));
        derivacao.getItems().addAll(DerivacaoDeChave.values());
        derivacao.getSelectionModel().select(DerivacaoDeChave.PADRAO);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.setPadding(new Insets(14));
        ColumnConstraints c0 = new ColumnConstraints(150);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(c0, c1, new ColumnConstraints(260));
        int r = 0;
        form.add(new Label("Operação"), 0, r);
        form.add(operacao, 1, r++);
        form.add(new Label("Arquivo de entrada"), 0, r);
        form.add(campoEntrada, 1, r);
        form.add(colunaEntrada, 2, r++);
        form.add(tamanhoArquivo, 1, r++);
        form.add(new Label("Arquivo de saída"), 0, r);
        form.add(campoSaida, 1, r);
        form.add(procurarSaida, 2, r++);
        form.add(rotuloAlgoritmo, 0, r);
        form.add(algoritmo, 1, r++);
        form.add(rotuloDerivacao, 0, r);
        form.add(derivacao, 1, r++);
        form.add(new Label("Senha"), 0, r);
        form.add(senha, 1, r++);
        form.add(forca, 1, r++);
        form.add(rotuloConfirmacao, 0, r);
        form.add(confirmacao, 1, r++);
        form.add(apagarOriginal, 1, r++);

        progresso.setMaxWidth(Double.MAX_VALUE);
        log.setEditable(false);
        log.setPrefRowCount(7);
        log.getStyleClass().add("t-log");
        executar.setDefaultButton(true);
        executar.setOnAction(e -> executar());
        cancelar.setDisable(true);
        cancelar.setOnAction(e -> ctrl.cancelar());
        HBox botoes = new HBox(10, executar, cancelar);
        botoes.setAlignment(Pos.CENTER_RIGHT);

        VBox centro = new VBox(8, form, progresso, log, botoes);
        centro.setPadding(new Insets(0, 14, 10, 14));
        VBox.setVgrow(log, Priority.ALWAYS);
        raiz.setCenter(centro);

        HBox barraStatus = new HBox(status);
        barraStatus.setPadding(new Insets(4, 14, 4, 14));
        barraStatus.getStyleClass().add("t-status");
        raiz.setBottom(barraStatus);

        raiz.setOnDragOver(ev -> {
            if (ev.getDragboard().hasFiles()) {
                ev.acceptTransferModes(TransferMode.COPY);
                if (!raiz.getStyleClass().contains("arrastando")) raiz.getStyleClass().add("arrastando");
            }
            ev.consume();
        });
        raiz.setOnDragExited(ev -> raiz.getStyleClass().remove("arrastando"));
        raiz.setOnDragDropped(ev -> {
            raiz.getStyleClass().remove("arrastando");
            boolean ok = ev.getDragboard().hasFiles();
            if (ok) receberArrastado(ev.getDragboard().getFiles());
            ev.setDropCompleted(ok);
            ev.consume();
        });

        grupo.selectedToggleProperty().addListener((o, a, b) -> aoTrocarOperacao());
        aoTrocarOperacao();
    }

    private boolean criptografando() { return rbCriptografar.isSelected(); }

    private void aoTrocarOperacao() {
        boolean c = criptografando();
        legenda.setVisible(c);
        legenda.setManaged(c);
        forca.setVisible(c);
        forca.setManaged(c);
        algoritmo.setDisable(!c);
        derivacao.setDisable(!c);
        rotuloDerivacao.setText(c ? "Derivação de chave" : "Derivação (detectada no arquivo)");
        rotuloAlgoritmo.setText(c ? "Algoritmo" : "Algoritmo (detectado no arquivo)");
        confirmacao.setVisible(c);
        confirmacao.setManaged(c);
        rotuloConfirmacao.setVisible(c);
        rotuloConfirmacao.setManaged(c);
        apagarOriginal.setVisible(c);
        apagarOriginal.setManaged(c);
        sugerirSaida();
        atualizarTamanho();
    }

    /** Mostra o tamanho do item escolhido (arquivo ou pasta), em MB, e o limite quando for criptografar. */
    private void atualizarTamanho() {
        tamanhoArquivo.getStyleClass().remove("t-erro");
        String e = campoEntrada.getText().trim();
        Path p;
        try { p = e.isEmpty() ? null : Path.of(e); } catch (RuntimeException ex) { p = null; }
        boolean existe = p != null && (java.nio.file.Files.isRegularFile(p) || java.nio.file.Files.isDirectory(p));
        if (!existe) {
            tamanhoArquivo.setText(e.isEmpty() ? "" : "Arquivo ou pasta não encontrado.");
            apagarOriginal.setDisable(false);
            return;
        }
        InfoDeEntrada info = InfoDeEntrada.de(p);
        apagarOriginal.setDisable(info.pasta());
        if (info.pasta()) apagarOriginal.setSelected(false);
        String txt = (info.pasta() ? "Pasta: " + info.arquivos() + (info.arquivos() == 1 ? " arquivo · " : " arquivos · ") : "")
                + "Tamanho: " + LimitesDeArquivo.formatar(info.tamanhoBytes());
        if (criptografando()) {
            txt += " (máximo: " + LimitesDeArquivo.tamanhoMaximoTexto() + ")";
            if (LimitesDeArquivo.excede(info.tamanhoBytes())) {
                txt += " — acima do limite";
                tamanhoArquivo.getStyleClass().add("t-erro");
            }
        }
        tamanhoArquivo.setText(txt);
    }

    private void escolherEntrada() {
        if (criptografando()) {
            // Diálogo único: aceita arquivo ou pasta.
            SeletorDeItem.escolher(stage, "Selecione o arquivo ou a pasta de entrada", p -> {
                campoEntrada.setText(p.toAbsolutePath().toString());
                sugerirSaida();
            });
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Arquivo de entrada");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos protegidos", "*" + FormatoArquivoProtegido.EXTENSAO));
        File f = fc.showOpenDialog(stage);
        if (f != null) {
            campoEntrada.setText(f.getAbsolutePath());
            sugerirSaida();
        }
    }

    private void receberArrastado(java.util.List<File> arquivos) {
        if (arquivos.size() != 1) { escrever("[ERRO] " + cat.arrasteVariosItens()); return; }
        Path p = arquivos.get(0).toPath();
        boolean protegido = java.nio.file.Files.isRegularFile(p) && p.getFileName().toString().endsWith(FormatoArquivoProtegido.EXTENSAO);
        // O sentido da operação acompanha o item arrastado: .protegido abre; o resto é criptografado.
        (protegido ? rbDescriptografar : rbCriptografar).setSelected(true);
        campoEntrada.setText(p.toAbsolutePath().toString());
        sugerirSaida();
    }

    private void escolherSaida() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Arquivo de saída — escolha a pasta e confirme o nome");
        String atual = campoSaida.getText().trim();
        if (!atual.isEmpty()) {
            try {
                Path p = Path.of(atual).toAbsolutePath();
                if (p.getParent() != null && java.nio.file.Files.isDirectory(p.getParent())) fc.setInitialDirectory(p.getParent().toFile());
                fc.setInitialFileName(p.getFileName().toString());
            } catch (RuntimeException ignorada) { /* caminho digitado inválido: abre no local padrão */ }
        }
        File f = fc.showSaveDialog(stage);
        if (f != null) campoSaida.setText(f.getAbsolutePath());
    }

    private void sugerirSaida() {
        String e = campoEntrada.getText().trim();
        if (e.isEmpty()) { campoSaida.clear(); return; }
        Path entrada = Path.of(e);
        campoSaida.setText((criptografando() ? FormatoArquivoProtegido.saidaProtegida(entrada)
                : FormatoArquivoProtegido.saidaAberta(entrada)).toString());
    }

    private void executar() {
        if (campoEntrada.getText().isBlank() || campoSaida.getText().isBlank()) {
            escrever("[ERRO] Informe os arquivos de entrada e de saída.");
            ctrl.registrarErro(CodigoResultado.ARQUIVO_NAO_ENCONTRADO);
            return;
        }
        Path entrada = Path.of(campoEntrada.getText().trim());
        Path saida = Path.of(campoSaida.getText().trim());
        if (criptografando()) {
            CodigoResultado v = ValidadorDeSenha.validar(senha.getText().toCharArray(), confirmacao.getText().toCharArray());
            if (v != CodigoResultado.SUCESSO) {
                escrever("[ERRO] " + cat.mensagem(v));
                ctrl.registrarErro(v);
                return;
            }
        }
        definirOcupado(true);
        escrever("[INFO] Iniciando " + (criptografando() ? "criptografia (" + algoritmo.getValue() + ", " + derivacao.getValue() + ")..." : "descriptografia (algoritmo e derivação lidos do cabeçalho do arquivo)..."));
        if (criptografando()) {
            ctrl.proteger(entrada, saida, senha.getText().toCharArray(), confirmacao.getText().toCharArray(),
                    apagarOriginal.isSelected(), algoritmo.getValue(), derivacao.getValue(), progresso::setProgress, this::aoTerminar);
        } else {
            ctrl.abrir(entrada, saida, senha.getText().toCharArray(), progresso::setProgress, this::aoTerminar);
        }
    }

    private void aoTerminar(Resultado r) {
        definirOcupado(false);
        if (r.sucesso()) {
            ResumoOperacao s = r.resumo();
            escrever("[OK] " + cat.mensagem(r.codigo()) + " Saída: " + s.caminhoSaida() + " | " + s.algoritmo() + " | " + s.derivacao() + " | " + LimitesDeArquivo.formatar(s.tamanhoBytes())
                    + " | " + s.duracaoMs() + " ms | original " + (s.originalPreservado() ? "preservado" : "removido"));
            status.setText("Concluído.");
            senha.clear();
            confirmacao.clear();
        } else {
            escrever("[ERRO] " + cat.mensagem(r.codigo()));
            status.setText("Falhou: " + r.codigo());
            progresso.setProgress(0);
        }
    }

    private void definirOcupado(boolean ocupado) {
        executar.setDisable(ocupado);
        cancelar.setDisable(!ocupado);
        status.setText(ocupado ? "Processando..." : status.getText());
        if (ocupado) progresso.setProgress(0);
    }

    private void escrever(String linha) {
        log.appendText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "  " + linha + "\n");
    }

    @Override public Parent raiz() { return raiz; }
    @Override public void exibir() { ctrl.telaAberta(); }
    @Override public void encerrar() { senha.clear(); confirmacao.clear(); }
}

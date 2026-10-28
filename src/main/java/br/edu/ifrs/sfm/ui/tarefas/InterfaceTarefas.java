package br.edu.ifrs.sfm.ui.tarefas;

import br.edu.ifrs.sfm.controle.GerenciadorDeModo;
import br.edu.ifrs.sfm.controle.OperacaoControlador;
import br.edu.ifrs.sfm.core.arquivo.FormatoArquivoProtegido;
import br.edu.ifrs.sfm.core.arquivo.InfoDeEntrada;
import br.edu.ifrs.sfm.core.resultado.CodigoResultado;
import br.edu.ifrs.sfm.core.resultado.Resultado;
import br.edu.ifrs.sfm.core.validacao.ForcaDeSenha;
import br.edu.ifrs.sfm.core.validacao.LimitesDeArquivo;
import br.edu.ifrs.sfm.core.validacao.ValidadorDeSenha;
import br.edu.ifrs.sfm.ui.InterfaceUsuario;
import br.edu.ifrs.sfm.ui.ajuda.JanelaAjuda;
import br.edu.ifrs.sfm.ui.componentes.IndicadorDeForca;
import br.edu.ifrs.sfm.ui.componentes.SeletorDeItem;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/** Interface orientada a tarefas: o usuário escolhe o que quer fazer; o sistema conduz em passos. */
public final class InterfaceTarefas implements InterfaceUsuario {
    private enum Fluxo { PROTEGER, ABRIR }

    private final OperacaoControlador ctrl;
    private final GerenciadorDeModo modo;
    private final Stage stage;
    private final CatalogoTarefas cat = new CatalogoTarefas();
    private final BorderPane raiz = new BorderPane();

    private Fluxo fluxo;
    private Path arquivo;
    private Path saida;
    private boolean apagarOriginal;
    private boolean noInicio;
    private Consumer<Path> aoReceberItem;
    private Consumer<String> mostrarErroItem;

    public InterfaceTarefas(OperacaoControlador ctrl, GerenciadorDeModo modo, Stage stage) {
        this.ctrl = ctrl;
        this.modo = modo;
        this.stage = stage;
        raiz.getStyleClass().add("tarefas");
        configurarArrastar();
        telaInicio();
    }

    // ---------------------------------------------------------------- estrutura comum

    private HBox barraSuperior(boolean inicio) {
        Label marca = new Label("Secure File Manager");
        marca.getStyleClass().add("marca");
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox barra = new HBox(12, marca, espaco);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("barra-superior");
        Button ajuda = botao("Ajuda", false);
        ajuda.setOnAction(e -> { ctrl.ajudaAberta(); JanelaAjuda.exibir(stage, cat.ajuda(), "tarefas"); });
        barra.getChildren().add(ajuda);
        if (inicio) {
            if (modo.trocaPermitida()) {
                Button tecnico = botao("Modo técnico", false);
                tecnico.setOnAction(e -> modo.alternar());
                barra.getChildren().add(tecnico);
            }
        } else {
            Button inicioBtn = botao("Voltar ao início", false);
            inicioBtn.setOnAction(e -> { ctrl.cancelar(); telaInicio(); });
            barra.getChildren().add(inicioBtn);
        }
        return barra;
    }

    private static Button botao(String texto, boolean primario) {
        Button b = new Button(texto);
        b.getStyleClass().add(primario ? "botao-primario" : "botao");
        return b;
    }

    private static Label titulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("titulo");
        l.setWrapText(true);
        return l;
    }

    private static Label subtitulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("subtitulo");
        l.setWrapText(true);
        return l;
    }

    private static VBox caixa(String classe, String negrito, String texto) {
        Label n = new Label(negrito);
        n.getStyleClass().add("caixa-negrito");
        n.setWrapText(true);
        Label t = new Label(texto);
        t.setWrapText(true);
        VBox c = new VBox(4);
        if (negrito != null && !negrito.isEmpty()) c.getChildren().add(n);
        c.getChildren().add(t);
        c.getStyleClass().addAll("caixa", classe);
        return c;
    }

    private static HBox indicador(int passoAtual, String... rotulos) {
        HBox h = new HBox(10);
        h.setAlignment(Pos.CENTER);
        h.setPadding(new Insets(22, 32, 0, 32));
        for (int i = 0; i < rotulos.length; i++) {
            int n = i + 1;
            Label bola = new Label(String.valueOf(n));
            bola.getStyleClass().add("passo-bola");
            if (n <= passoAtual) bola.getStyleClass().add("passo-feito");
            Label nome = new Label(rotulos[i]);
            nome.getStyleClass().add(n == passoAtual ? "passo-nome-atual" : "passo-nome");
            h.getChildren().add(new HBox(8, bola, nome));
            ((HBox) h.getChildren().get(h.getChildren().size() - 1)).setAlignment(Pos.CENTER_LEFT);
            if (n < rotulos.length) {
                Region linha = new Region();
                linha.getStyleClass().add("passo-linha");
                linha.setPrefWidth(36);
                linha.setMaxHeight(2);
                linha.setPrefHeight(2);
                h.getChildren().add(linha);
            }
        }
        return h;
    }

    private String[] rotulosPassos() {
        return fluxo == Fluxo.PROTEGER
                ? new String[]{"Arquivo", "Senha", "Proteção", "Pronto"}
                : new String[]{"Arquivo", "Senha", "Abrindo", "Pronto"};
    }

    private void mostrar(boolean inicio, int passo, Node corpo, Node rodape) {
        noInicio = false;
        aoReceberItem = null;
        mostrarErroItem = null;
        VBox centro = new VBox();
        if (passo > 0) centro.getChildren().add(indicador(passo, rotulosPassos()));
        VBox areaCorpo = new VBox(18, corpo);
        areaCorpo.setPadding(new Insets(22, 80, 0, 80));
        VBox.setVgrow(areaCorpo, Priority.ALWAYS);
        centro.getChildren().add(areaCorpo);
        raiz.setTop(barraSuperior(inicio));
        raiz.setCenter(centro);
        raiz.setBottom(rodape);
        ctrl.telaAberta();
    }

    private static HBox rodape(Node esquerda, Node direita) {
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox h = new HBox(12);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(20, 80, 28, 80));
        if (esquerda != null) h.getChildren().add(esquerda);
        h.getChildren().add(espaco);
        if (direita != null) h.getChildren().add(direita);
        return h;
    }

    // ---------------------------------------------------------------- início

    private void telaInicio() {
        fluxo = null;
        arquivo = null;
        saida = null;
        apagarOriginal = false;

        Button proteger = cartaoDeAcao("Proteger um arquivo", "Cria uma cópia do arquivo protegida por senha.");
        proteger.setOnAction(e -> { fluxo = Fluxo.PROTEGER; telaArquivo(); });
        Button abrir = cartaoDeAcao("Abrir um arquivo protegido", "Use a senha para ver o conteúdo de novo.");
        abrir.setOnAction(e -> { fluxo = Fluxo.ABRIR; telaArquivo(); });
        HBox cartoes = new HBox(24, proteger, abrir);
        cartoes.setAlignment(Pos.CENTER);
        HBox.setHgrow(proteger, Priority.ALWAYS);
        HBox.setHgrow(abrir, Priority.ALWAYS);

        Label titulo = titulo("O que você quer fazer?");
        VBox corpo = new VBox(26, titulo, cartoes);
        corpo.setAlignment(Pos.CENTER);
        VBox.setVgrow(cartoes, Priority.NEVER);
        Label nota = new Label("Seus arquivos são protegidos no seu próprio computador.");
        nota.getStyleClass().add("subtitulo");
        HBox pe = new HBox(nota);
        pe.setAlignment(Pos.CENTER);
        pe.setPadding(new Insets(0, 0, 28, 0));
        mostrar(true, 0, corpo, pe);
        noInicio = true;
    }

    private Button cartaoDeAcao(String titulo, String texto) {
        Label t = new Label(titulo);
        t.getStyleClass().add("cartao-titulo");
        Label d = new Label(texto);
        d.setWrapText(true);
        d.getStyleClass().add("cartao-texto");
        VBox conteudo = new VBox(8, t, d);
        conteudo.setAlignment(Pos.CENTER_LEFT);
        Button b = new Button();
        b.setGraphic(conteudo);
        b.getStyleClass().add("cartao-acao");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setPrefHeight(170);
        return b;
    }

    // ---------------------------------------------------------------- arrastar e soltar

    private void configurarArrastar() {
        raiz.setOnDragOver(e -> {
            if ((noInicio || aoReceberItem != null) && e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
                if (!raiz.getStyleClass().contains("arrastando")) raiz.getStyleClass().add("arrastando");
            }
            e.consume();
        });
        raiz.setOnDragExited(e -> raiz.getStyleClass().remove("arrastando"));
        raiz.setOnDragDropped(e -> {
            raiz.getStyleClass().remove("arrastando");
            boolean ok = false;
            if (e.getDragboard().hasFiles()) {
                receberArrastado(e.getDragboard().getFiles());
                ok = true;
            }
            e.setDropCompleted(ok);
            e.consume();
        });
    }

    private void receberArrastado(List<java.io.File> arquivos) {
        if (arquivos.isEmpty()) return;
        Path p = arquivos.get(0).toPath();
        if (noInicio) { // da tela inicial: .protegido abre; qualquer outra coisa é para proteger
            boolean protegido = Files.isRegularFile(p) && p.getFileName().toString().endsWith(FormatoArquivoProtegido.EXTENSAO);
            fluxo = arquivos.size() == 1 && protegido ? Fluxo.ABRIR : Fluxo.PROTEGER;
            telaArquivo();
        }
        if (aoReceberItem == null) return;
        if (arquivos.size() != 1) { mostrarErroItem.accept(cat.arrasteVariosItens()); return; }
        aoReceberItem.accept(p);
    }

    // ---------------------------------------------------------------- passo 1: arquivo

    private void telaArquivo() {
        boolean proteger = fluxo == Fluxo.PROTEGER;
        Label erro = new Label();
        erro.getStyleClass().add("texto-erro");
        erro.setWrapText(true);

        Label nome = new Label();
        nome.getStyleClass().add("caixa-negrito");
        Label detalhe = new Label();
        detalhe.getStyleClass().add("subtitulo");
        detalhe.setWrapText(true);
        VBox info = new VBox(2, nome, detalhe);

        // Botão único: ao proteger, aceita arquivo ou pasta; o sistema se adapta ao que foi escolhido.
        Button escolher = botao(proteger ? "Adicionar arquivo ou pasta" : "Escolher arquivo", false);
        HBox botoes = new HBox(10, escolher);
        // Legenda do tamanho máximo, logo abaixo dos botões de adicionar (apenas ao proteger).
        VBox colunaBotao = new VBox(4, botoes);
        colunaBotao.setAlignment(Pos.CENTER_RIGHT);
        if (proteger) {
            Label legenda = new Label(cat.legendaTamanhoMaximo());
            legenda.getStyleClass().add("legenda");
            colunaBotao.getChildren().add(legenda);
        }

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        info.setMaxWidth(420);
        HBox cartao = new HBox(12, info, espaco, colunaBotao);
        cartao.setAlignment(Pos.CENTER_LEFT);
        cartao.getStyleClass().addAll("caixa", "caixa-neutra");
        Label dica = new Label(cat.dicaArrastar());
        dica.getStyleClass().add("legenda");

        CheckBox apagar = new CheckBox("Apagar o original depois de proteger");
        apagar.setSelected(apagarOriginal);
        apagar.selectedProperty().addListener((o, a, b) -> apagarOriginal = b);
        Label destino = new Label();
        destino.setWrapText(true);
        VBox caixaDestino = new VBox(6);
        caixaDestino.getStyleClass().addAll("caixa", "caixa-info");
        Label tituloDestino = new Label("Onde ficará o resultado");
        tituloDestino.getStyleClass().add("caixa-negrito");
        caixaDestino.getChildren().addAll(tituloDestino, destino, apagar);

        Button continuar = botao("Continuar", true);
        Button voltar = botao("Voltar", false);
        voltar.setOnAction(e -> telaInicio());
        continuar.setOnAction(e -> telaSenha(null));

        Runnable atualizar = () -> {
            erro.setText("");
            caixaDestino.setVisible(arquivo != null);
            caixaDestino.setManaged(arquivo != null);
            if (arquivo == null) {
                nome.setText("Nada escolhido ainda");
                detalhe.setText(proteger ? "Escolha um arquivo ou uma pasta para começar." : "Escolha um arquivo com final .protegido.");
                continuar.setDisable(true);
                destino.setText("");
                apagar.setVisible(proteger);
                apagar.setManaged(proteger);
                return;
            }
            InfoDeEntrada item = InfoDeEntrada.de(arquivo);
            nome.setText(arquivo.getFileName() + (item.pasta() ? " (pasta)" : ""));
            Path pai = arquivo.getParent();
            String nomePai = pai == null || pai.getFileName() == null ? "" : pai.getFileName().toString();
            detalhe.setText((item.pasta() ? "" : "Pasta " + nomePai + " · ")
                    + (item.pasta() ? item.arquivos() + (item.arquivos() == 1 ? " arquivo · " : " arquivos · ") : "")
                    + "Tamanho: " + LimitesDeArquivo.formatar(item.tamanhoBytes())
                    + (proteger ? " (máximo: " + LimitesDeArquivo.tamanhoMaximoTexto() + ")" : ""));
            boolean bloqueado = false;
            if (proteger && LimitesDeArquivo.excede(item.tamanhoBytes())) {
                erro.setText(cat.mensagem(CodigoResultado.ARQUIVO_MUITO_GRANDE));
                bloqueado = true;
            } else if (proteger && item.pasta() && item.arquivos() == 0) {
                erro.setText(cat.mensagem(CodigoResultado.PASTA_VAZIA));
                bloqueado = true;
            }
            continuar.setDisable(bloqueado);
            boolean mostraApagar = proteger && !item.pasta();
            if (!mostraApagar) { apagar.setSelected(false); apagarOriginal = false; }
            apagar.setVisible(mostraApagar);
            apagar.setManaged(mostraApagar);
            boolean pastaNoArquivo = !proteger && FormatoArquivoProtegido.contemPasta(arquivo);
            saida = proteger ? FormatoArquivoProtegido.saidaProtegida(arquivo) : FormatoArquivoProtegido.saidaAberta(arquivo);
            if (proteger) {
                destino.setText(item.pasta()
                        ? "Vamos reunir os arquivos da pasta em um único arquivo protegido, salvo ao lado dela, com o nome " + saida.getFileName() + ". A pasta original continua onde está."
                        : "Vamos salvar uma cópia protegida na mesma pasta, com o nome " + saida.getFileName() + ". O arquivo original continua onde está.");
            } else {
                destino.setText(pastaNoArquivo
                        ? "Vamos recriar a pasta no mesmo lugar do arquivo protegido, com o nome " + saida.getFileName() + ". O arquivo protegido continua onde está."
                        : "Vamos salvar o arquivo aberto na mesma pasta, com o nome " + saida.getFileName() + ". O arquivo protegido continua onde está.");
            }
        };
        Consumer<Path> selecionar = p -> {
            if (!proteger && !(Files.isRegularFile(p) && p.getFileName().toString().endsWith(FormatoArquivoProtegido.EXTENSAO))) {
                erro.setText(cat.arrasteParaAbrirInvalido());
                return;
            }
            if (proteger && !(Files.isRegularFile(p) || Files.isDirectory(p))) return;
            arquivo = p;
            escolher.setText(proteger ? "Trocar arquivo ou pasta" : "Trocar arquivo");
            atualizar.run();
        };
        escolher.setOnAction(e -> {
            if (proteger) {
                SeletorDeItem.escolher(stage, "Selecione o arquivo ou a pasta que você quer proteger", selecionar);
            } else {
                FileChooser fc = new FileChooser();
                fc.setTitle("Escolha o arquivo protegido");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos protegidos", "*" + FormatoArquivoProtegido.EXTENSAO));
                File f = fc.showOpenDialog(stage);
                if (f != null) selecionar.accept(f.toPath());
            }
        });
        atualizar.run();

        VBox corpo = new VBox(18, titulo(proteger ? "Escolha o que você quer proteger" : "Escolha o arquivo protegido que você quer abrir"),
                cartao, dica, erro, caixaDestino);
        mostrar(false, 1, corpo, rodape(voltar, continuar));
        aoReceberItem = selecionar;
        mostrarErroItem = erro::setText;
    }

    // ---------------------------------------------------------------- passo 2: senha

    private void telaSenha(String mensagemErro) {
        boolean proteger = fluxo == Fluxo.PROTEGER;
        PasswordField s1 = campoSenha();
        PasswordField s2 = campoSenha();
        Label erro = new Label(mensagemErro == null ? "" : mensagemErro);
        erro.getStyleClass().add("texto-erro");
        erro.setWrapText(true);

        VBox corpo = new VBox(18);
        if (proteger) {
            IndicadorDeForca forca = new IndicadorDeForca(cat);
            s1.textProperty().addListener((o, a, b) -> forca.atualizar(ForcaDeSenha.avaliar(b.toCharArray())));
            corpo.getChildren().addAll(titulo("Crie uma senha para este arquivo"),
                    subtitulo("Use pelo menos " + ValidadorDeSenha.TAMANHO_MINIMO + " caracteres, misturando letras e números."),
                    new HBox(20, rotulado("Senha", s1), rotulado("Repita a senha", s2)),
                    forca,
                    erro,
                    caixa("caixa-aviso", "Atenção:", "se você esquecer esta senha, não será possível abrir o arquivo depois. Anote em um lugar seguro."));
        } else {
            corpo.getChildren().addAll(titulo("Digite a senha do arquivo"),
                    rotulado("Digite a senha de " + arquivo.getFileName(), s1), erro);
        }

        Button voltar = botao("Voltar", false);
        voltar.setOnAction(e -> telaArquivo());
        Button seguir = botao(proteger ? "Proteger arquivo" : "Abrir arquivo", true);
        seguir.setOnAction(e -> {
            if (proteger) {
                CodigoResultado v = ValidadorDeSenha.validar(s1.getText().toCharArray(), s2.getText().toCharArray());
                if (v != CodigoResultado.SUCESSO) {
                    ctrl.registrarErro(v);
                    erro.setText(cat.mensagem(v));
                    return;
                }
            } else if (s1.getText().isEmpty()) {
                erro.setText("Digite a senha para continuar.");
                return;
            }
            executar(s1.getText(), s2.getText());
        });
        s1.setOnAction(e -> { if (!proteger) seguir.fire(); else s2.requestFocus(); });
        s2.setOnAction(e -> seguir.fire());
        mostrar(false, 2, corpo, rodape(voltar, seguir));
        s1.requestFocus();
    }

    private static PasswordField campoSenha() {
        PasswordField p = new PasswordField();
        p.getStyleClass().add("campo-senha");
        p.setPrefHeight(48);
        return p;
    }

    private static VBox rotulado(String texto, Control campo) {
        Label l = new Label(texto);
        l.getStyleClass().add("rotulo-campo");
        l.setLabelFor(campo);
        VBox v = new VBox(6, l, campo);
        HBox.setHgrow(v, Priority.ALWAYS);
        campo.setMaxWidth(Double.MAX_VALUE);
        return v;
    }

    // ---------------------------------------------------------------- passo 3: progresso

    private void executar(String senha, String confirmacao) {
        boolean proteger = fluxo == Fluxo.PROTEGER;
        ProgressBar barra = new ProgressBar(0);
        barra.setPrefWidth(600);
        barra.setPrefHeight(22);
        VBox corpo = new VBox(18, titulo(proteger ? "Protegendo seu arquivo…" : "Abrindo seu arquivo…"),
                subtitulo("Isso leva só alguns instantes. Não feche o programa."), barra);
        Button cancelar = botao("Cancelar", false);
        cancelar.setOnAction(e -> ctrl.cancelar());
        mostrar(false, 3, corpo, rodape(cancelar, null));

        if (proteger) {
            ctrl.proteger(arquivo, saida, senha.toCharArray(), confirmacao.toCharArray(), apagarOriginal,
                    barra::setProgress, this::aoTerminar);
        } else {
            ctrl.abrir(arquivo, saida, senha.toCharArray(), barra::setProgress, this::aoTerminar);
        }
    }

    private void aoTerminar(Resultado r) {
        if (r.sucesso()) telaResultado(r);
        else if (r.codigo() == CodigoResultado.AUTENTICACAO_FALHOU && fluxo == Fluxo.ABRIR) telaSenhaIncorreta();
        else telaErro(r.codigo());
    }

    // ---------------------------------------------------------------- passo 4: resultado e erros

    private void telaResultado(Resultado r) {
        boolean proteger = fluxo == Fluxo.PROTEGER;
        boolean pasta = r.resumo().pasta();
        Path caminho = r.arquivoGerado();
        Path pastaDoResultado = caminho.getParent();
        String nomePasta = pastaDoResultado == null || pastaDoResultado.getFileName() == null ? "." : pastaDoResultado.getFileName().toString();
        boolean preservado = r.resumo().originalPreservado();

        Label ok = new Label("✔");
        ok.getStyleClass().add("icone-ok");
        String tituloOk = proteger ? "Pronto! Seu arquivo está protegido."
                : (pasta ? "Pronto! Sua pasta foi aberta." : "Pronto! Seu arquivo foi aberto.");
        HBox cab = new HBox(14, ok, titulo(tituloOk));
        cab.setAlignment(Pos.CENTER_LEFT);

        String criado;
        if (proteger) criado = pasta ? "Um arquivo protegido chamado " + caminho.getFileName() + ", com todos os arquivos da pasta."
                : "Um arquivo protegido chamado " + caminho.getFileName() + ".";
        else criado = pasta ? "Uma pasta chamada " + caminho.getFileName() + ", com os arquivos que estavam protegidos."
                : "Um arquivo aberto chamado " + caminho.getFileName() + ".";

        String oQueAconteceu;
        if (proteger) {
            if (pasta) oQueAconteceu = "A pasta continua no mesmo lugar, sem nenhuma alteração.";
            else oQueAconteceu = preservado ? "Ele continua na mesma pasta, sem nenhuma alteração." : "Ele foi apagado, como você pediu. Guarde bem a senha.";
        } else {
            oQueAconteceu = "O arquivo protegido continua na mesma pasta, sem nenhuma alteração.";
        }
        VBox corpo = new VBox(18, cab,
                caixa("caixa-neutra", "O que foi criado", criado),
                caixa("caixa-neutra", "Onde está salvo", "Na pasta " + nomePasta + (proteger && preservado ? ", junto do original." : ".")),
                caixa("caixa-neutra", proteger ? "O que aconteceu com o original" : "O que aconteceu com o arquivo protegido", oQueAconteceu));

        Button abrirPasta = botao("Abrir a pasta", false);
        abrirPasta.setOnAction(e -> abrirPasta(pastaDoResultado));
        Button outro = botao(proteger ? "Proteger outro" : "Abrir outro arquivo", false);
        Fluxo atual = fluxo;
        outro.setOnAction(e -> { telaInicio(); fluxo = atual; arquivo = null; telaArquivo(); });
        Button concluir = botao("Concluir", true);
        concluir.setOnAction(e -> telaInicio());
        mostrar(false, 4, corpo, rodape(abrirPasta, new HBox(12, outro, concluir)));
    }

    private void telaSenhaIncorreta() {
        PasswordField s = campoSenha();
        s.getStyleClass().add("campo-erro");
        VBox corpo = new VBox(18, titulo("Essa senha não abriu o arquivo"),
                caixa("caixa-erro", "A senha não confere.",
                        "Confira se a tecla Caps Lock está ativada e se é a mesma senha que você criou ao proteger o arquivo. Seu arquivo não foi alterado."),
                rotulado("Digite a senha de " + arquivo.getFileName(), s));
        Button inicio = botao("Voltar ao início", false);
        inicio.setOnAction(e -> telaInicio());
        Button tentar = botao("Tentar de novo", true);
        tentar.setOnAction(e -> {
            if (s.getText().isEmpty()) return;
            executar(s.getText(), "");
        });
        s.setOnAction(e -> tentar.fire());
        mostrar(false, 2, corpo, rodape(inicio, tentar));
        s.requestFocus();
    }

    private void telaErro(CodigoResultado codigo) {
        boolean cancelada = codigo == CodigoResultado.OPERACAO_CANCELADA;
        VBox corpo = new VBox(18,
                titulo(cancelada ? "Operação cancelada" : "Não foi possível concluir"),
                caixa(cancelada ? "caixa-neutra" : "caixa-erro", "", cat.mensagem(codigo)));
        Button inicio = botao("Voltar ao início", false);
        inicio.setOnAction(e -> telaInicio());
        Button tentar = botao("Tentar de novo", true);
        tentar.setOnAction(e -> telaArquivo());
        mostrar(false, 0, corpo, rodape(inicio, tentar));
    }

    private void abrirPasta(Path pasta) {
        if (pasta == null) return;
        Thread t = new Thread(() -> {
            try {
                if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().open(pasta.toFile());
            } catch (Exception ignorada) { /* sem navegador de arquivos disponível */ }
        }, "abrir-pasta");
        t.setDaemon(true);
        t.start();
    }

    @Override public Parent raiz() { return raiz; }
    @Override public void exibir() { /* a tela já registra TELA_ABERTA ao ser montada */ }
    @Override public void encerrar() { ctrl.cancelar(); }
}

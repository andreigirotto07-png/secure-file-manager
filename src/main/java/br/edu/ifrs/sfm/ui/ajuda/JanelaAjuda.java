package br.edu.ifrs.sfm.ui.ajuda;

import br.edu.ifrs.sfm.ui.ajuda.BlocoDeAjuda.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/** Janela de ajuda: cabeçalho, abas por assunto e blocos visuais (passos, dicas, avisos, cartões). */
public final class JanelaAjuda {
    private JanelaAjuda() {}

    /** @param tema "tarefas" ou "tecnica": define as cores (classes em app.css). */
    public static void exibir(Stage dono, ConteudoDeAjuda conteudo, String tema) {
        Stage janela = new Stage();

        Label titulo = new Label(conteudo.titulo());
        titulo.getStyleClass().add("ajuda-cabecalho-titulo");
        Label subtitulo = new Label(conteudo.subtitulo());
        subtitulo.getStyleClass().add("ajuda-cabecalho-subtitulo");
        subtitulo.setWrapText(true);
        VBox cabecalho = new VBox(4, titulo, subtitulo);
        cabecalho.getStyleClass().add("ajuda-cabecalho");

        // Abas: um botão por seção; só o conteúdo da seção escolhida aparece.
        FlowPane abas = new FlowPane(8, 8);
        abas.getStyleClass().add("ajuda-abas");
        ToggleGroup grupo = new ToggleGroup();
        StackPane area = new StackPane();
        for (SecaoDeAjuda secao : conteudo.secoes()) {
            ToggleButton aba = new ToggleButton(secao.titulo());
            aba.getStyleClass().add("ajuda-aba");
            aba.setToggleGroup(grupo);
            aba.setUserData(montarSecao(secao));
            abas.getChildren().add(aba);
        }
        grupo.selectedToggleProperty().addListener((o, antigo, novo) -> {
            if (novo == null) { if (antigo != null) antigo.setSelected(true); return; } // sempre uma aba ativa
            area.getChildren().setAll((Node) novo.getUserData());
        });
        if (!abas.getChildren().isEmpty()) ((ToggleButton) abas.getChildren().get(0)).setSelected(true);

        Button fechar = new Button("Fechar");
        fechar.getStyleClass().add("ajuda-fechar");
        fechar.setOnAction(e -> janela.close());
        HBox rodape = new HBox(fechar);
        rodape.setAlignment(Pos.CENTER_RIGHT);
        rodape.getStyleClass().add("ajuda-rodape");

        VBox topo = new VBox(cabecalho, abas);
        BorderPane raiz = new BorderPane(area);
        raiz.setTop(topo);
        raiz.setBottom(rodape);
        raiz.getStyleClass().addAll(tema, "ajuda");

        Scene cena = new Scene(raiz, 720, 640);
        cena.getStylesheets().addAll(dono.getScene().getStylesheets());
        cena.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) janela.close(); });
        janela.setScene(cena);
        janela.setTitle(conteudo.titulo());
        janela.setMinWidth(560);
        janela.setMinHeight(480);
        janela.initOwner(dono);
        janela.initModality(Modality.WINDOW_MODAL);
        janela.show();
    }

    private static Node montarSecao(SecaoDeAjuda secao) {
        VBox coluna = new VBox(14);
        coluna.getStyleClass().add("ajuda-conteudo");
        for (BlocoDeAjuda bloco : secao.blocos()) {
            if (bloco instanceof Passos p) coluna.getChildren().add(passos(p));
            else if (bloco instanceof Paragrafo p) coluna.getChildren().add(texto(p.texto(), "ajuda-paragrafo"));
            else if (bloco instanceof Dica d) coluna.getChildren().add(caixa("Dica", d.texto(), "ajuda-dica"));
            else if (bloco instanceof Aviso a) coluna.getChildren().add(caixa("Atenção", a.texto(), "ajuda-aviso"));
            else if (bloco instanceof Termos t) coluna.getChildren().add(termos(t));
        }
        ScrollPane rolagem = new ScrollPane(coluna);
        rolagem.setFitToWidth(true);
        rolagem.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rolagem.getStyleClass().add("ajuda-rolagem");
        return rolagem;
    }

    private static Label texto(String t, String classe) {
        Label l = new Label(t);
        l.setWrapText(true);
        l.getStyleClass().add(classe);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private static VBox passos(Passos p) {
        VBox lista = new VBox(10);
        int n = 1;
        for (String item : p.itens()) {
            Label numero = new Label(String.valueOf(n++));
            numero.getStyleClass().add("ajuda-passo-numero");
            numero.setMinSize(32, 32);
            numero.setPrefSize(32, 32);
            numero.setAlignment(Pos.CENTER);
            Label t = texto(item, "ajuda-passo-texto");
            HBox.setHgrow(t, Priority.ALWAYS);
            HBox linha = new HBox(14, numero, t);
            linha.setAlignment(Pos.TOP_LEFT);
            linha.getStyleClass().add("ajuda-passo");
            lista.getChildren().add(linha);
        }
        return lista;
    }

    private static VBox caixa(String rotulo, String t, String classe) {
        Label r = new Label(rotulo);
        r.getStyleClass().add("ajuda-caixa-rotulo");
        VBox c = new VBox(4, r, texto(t, "ajuda-caixa-texto"));
        c.getStyleClass().addAll("ajuda-caixa", classe);
        return c;
    }

    private static VBox termos(Termos t) {
        VBox lista = new VBox(10);
        for (Termo termo : t.itens()) {
            Label nome = new Label(termo.nome());
            nome.getStyleClass().add("ajuda-termo-nome");
            HBox cab = new HBox(10, nome);
            cab.setAlignment(Pos.CENTER_LEFT);
            if (termo.selo() != null) {
                Label selo = new Label(termo.selo());
                selo.getStyleClass().add("ajuda-selo");
                cab.getChildren().add(selo);
            }
            VBox cartao = new VBox(4, cab, texto(termo.descricao(), "ajuda-termo-descricao"));
            cartao.getStyleClass().add("ajuda-termo");
            lista.getChildren().add(cartao);
        }
        return lista;
    }
}

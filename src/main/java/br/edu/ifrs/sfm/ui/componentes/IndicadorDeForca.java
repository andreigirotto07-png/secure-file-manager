package br.edu.ifrs.sfm.ui.componentes;

import br.edu.ifrs.sfm.core.validacao.ForcaDeSenha;
import br.edu.ifrs.sfm.ui.CatalogoDeMensagens;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Barra de 3 segmentos + texto, igual nas duas interfaces (o texto vem do catálogo de cada uma). */
public final class IndicadorDeForca extends HBox {
    private final Region[] segmentos = {new Region(), new Region(), new Region()};
    private final Label texto = new Label();
    private final CatalogoDeMensagens catalogo;

    public IndicadorDeForca(CatalogoDeMensagens catalogo) {
        super(8);
        this.catalogo = catalogo;
        setAlignment(Pos.CENTER_LEFT);
        HBox barra = new HBox(4);
        barra.setAlignment(Pos.CENTER_LEFT);
        for (Region r : segmentos) {
            r.getStyleClass().add("forca-seg");
            r.setPrefSize(60, 8);
            r.setMinSize(60, 8);
            barra.getChildren().add(r);
        }
        texto.getStyleClass().add("forca-texto");
        texto.setWrapText(true);
        HBox.setHgrow(texto, Priority.ALWAYS);
        getChildren().addAll(barra, texto);
        atualizar(ForcaDeSenha.VAZIA);
    }

    public void atualizar(ForcaDeSenha forca) {
        int n = switch (forca) { case VAZIA -> 0; case FRACA -> 1; case MEDIA -> 2; case FORTE -> 3; };
        String classe = switch (forca) { case FRACA -> "forca-fraca"; case MEDIA -> "forca-media"; case FORTE -> "forca-forte"; default -> "forca-vazia"; };
        for (int i = 0; i < segmentos.length; i++) {
            segmentos[i].getStyleClass().removeAll("forca-fraca", "forca-media", "forca-forte", "forca-vazia");
            segmentos[i].getStyleClass().add(i < n ? classe : "forca-vazia");
        }
        texto.setText(catalogo.forcaDaSenha(forca));
    }
}

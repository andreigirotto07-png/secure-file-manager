package br.edu.ifrs.sfm.ui.ajuda;

import java.util.List;

/** Uma aba da ajuda. */
public record SecaoDeAjuda(String titulo, List<BlocoDeAjuda> blocos) {
    public static SecaoDeAjuda de(String titulo, BlocoDeAjuda... blocos) { return new SecaoDeAjuda(titulo, List.of(blocos)); }
}

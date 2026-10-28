package br.edu.ifrs.sfm.ui.ajuda;

import java.util.List;

/** Conteúdo da ajuda, independente de JavaFX: cada interface monta o seu no próprio vocabulário. */
public record ConteudoDeAjuda(String titulo, String subtitulo, List<SecaoDeAjuda> secoes) {}

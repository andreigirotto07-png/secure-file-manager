package br.edu.ifrs.sfm.ui.ajuda;

import java.util.List;

/** Tipos de bloco que a janela de ajuda sabe desenhar. */
public sealed interface BlocoDeAjuda {
    /** Lista numerada de passos. */
    record Passos(List<String> itens) implements BlocoDeAjuda {
        public static Passos de(String... itens) { return new Passos(List.of(itens)); }
    }
    record Paragrafo(String texto) implements BlocoDeAjuda {}
    /** Caixa de dica (informação útil). */
    record Dica(String texto) implements BlocoDeAjuda {}
    /** Caixa de atenção (algo que pode causar perda ou erro). */
    record Aviso(String texto) implements BlocoDeAjuda {}
    /** Cartões de termo + descrição, com selo opcional (ex.: "padrão"). */
    record Termos(List<Termo> itens) implements BlocoDeAjuda {
        public static Termos de(Termo... itens) { return new Termos(List.of(itens)); }
    }
    record Termo(String nome, String descricao, String selo) {
        public static Termo de(String nome, String descricao) { return new Termo(nome, descricao, null); }
        public static Termo padrao(String nome, String descricao) { return new Termo(nome, descricao, "padrão"); }
    }
}

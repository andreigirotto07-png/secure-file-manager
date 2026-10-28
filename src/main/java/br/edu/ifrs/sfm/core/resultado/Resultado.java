package br.edu.ifrs.sfm.core.resultado;

import java.nio.file.Path;

public record Resultado(CodigoResultado codigo, Path arquivoGerado, ResumoOperacao resumo) {
    public boolean sucesso() { return codigo == CodigoResultado.SUCESSO; }
    public static Resultado falha(CodigoResultado codigo) { return new Resultado(codigo, null, null); }
}

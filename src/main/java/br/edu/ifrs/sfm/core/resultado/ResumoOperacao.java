package br.edu.ifrs.sfm.core.resultado;

import java.nio.file.Path;

public record ResumoOperacao(Path caminhoSaida, long tamanhoBytes, boolean pasta, String algoritmo, String derivacao,
                             long duracaoMs, boolean originalPreservado) {}

package br.edu.ifrs.sfm.core.cripto;

import br.edu.ifrs.sfm.core.resultado.Resultado;

import java.nio.file.Path;

/** Núcleo único compartilhado pelas duas interfaces. Nunca produz texto de interface. */
public interface ProtecaoService {

    interface Progresso { void aoProgredir(double fracao); }

    Resultado proteger(Path entrada, Path saida, char[] senha, char[] confirmacao, boolean apagarOriginal,
                       AlgoritmoCifra algoritmo, DerivacaoDeChave derivacao, Progresso progresso);

    /** Usa a derivação padrão (PBKDF2-HMAC-SHA256). */
    default Resultado proteger(Path entrada, Path saida, char[] senha, char[] confirmacao,
                               boolean apagarOriginal, AlgoritmoCifra algoritmo, Progresso progresso) {
        return proteger(entrada, saida, senha, confirmacao, apagarOriginal, algoritmo, DerivacaoDeChave.PADRAO, progresso);
    }

    /** Usa o algoritmo e a derivação padrão (AES-256-GCM e PBKDF2-HMAC-SHA256). */
    default Resultado proteger(Path entrada, Path saida, char[] senha, char[] confirmacao,
                               boolean apagarOriginal, Progresso progresso) {
        return proteger(entrada, saida, senha, confirmacao, apagarOriginal, AlgoritmoCifra.PADRAO, progresso);
    }

    Resultado abrir(Path entrada, Path saida, char[] senha, Progresso progresso);

    /** Pede o cancelamento da operação em andamento (sem deixar arquivo parcial). */
    void cancelar();
}

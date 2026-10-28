package br.edu.ifrs.sfm.core.validacao;

/**
 * Limite de tamanho do arquivo a proteger. O processamento é feito em memória
 * (o modo GCM só libera o texto aberto após verificar a autenticidade de tudo),
 * então o limite evita estourar a memória em computadores comuns.
 */
public final class LimitesDeArquivo {
    public static final long TAMANHO_MAXIMO_BYTES = 100L * 1024 * 1024; // 100 MB

    private LimitesDeArquivo() {}

    /** Texto exibido nas duas interfaces, ex.: "100 MB". */
    public static String tamanhoMaximoTexto() {
        return (TAMANHO_MAXIMO_BYTES / (1024 * 1024)) + " MB";
    }

    public static boolean excede(long tamanhoBytes) {
        return tamanhoBytes > TAMANHO_MAXIMO_BYTES;
    }

    /** Formata bytes para exibição (ex.: "4,3 MB"). */
    public static String formatar(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f KB", kb);
        return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f MB", kb / 1024.0);
    }
}

package br.edu.ifrs.sfm.core.validacao;

import java.util.Set;

/** Indicador de força exibido nas duas interfaces. É orientação: só o mínimo de 8 caracteres bloqueia. */
public enum ForcaDeSenha {
    VAZIA, FRACA, MEDIA, FORTE;

    private static final Set<String> COMUNS = Set.of("12345678", "123456789", "1234567890", "password", "senha123",
            "qwertyui", "abc12345", "11111111", "00000000", "senhasenha", "password1", "12341234", "123mudar");

    public static ForcaDeSenha avaliar(char[] senha) {
        if (senha == null || senha.length == 0) return VAZIA;
        if (senha.length < ValidadorDeSenha.TAMANHO_MINIMO) return FRACA;
        String s = new String(senha);
        if (COMUNS.contains(s.toLowerCase())) return FRACA;
        boolean minus = false, maius = false, digito = false, simbolo = false;
        for (char c : senha) {
            if (Character.isLowerCase(c)) minus = true;
            else if (Character.isUpperCase(c)) maius = true;
            else if (Character.isDigit(c)) digito = true;
            else simbolo = true;
        }
        int classes = (minus ? 1 : 0) + (maius ? 1 : 0) + (digito ? 1 : 0) + (simbolo ? 1 : 0);
        long distintos = s.chars().distinct().count();
        if (distintos < 4) return FRACA;
        if (senha.length >= 16 || (senha.length >= 12 && classes >= 3)) return FORTE;
        if (classes >= 2) return MEDIA;
        return FRACA;
    }
}

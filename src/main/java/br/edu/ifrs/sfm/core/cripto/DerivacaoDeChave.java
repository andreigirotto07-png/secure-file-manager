package br.edu.ifrs.sfm.core.cripto;

/**
 * Funções de derivação de chave oferecidas. O "custo" é um único número gravado no cabeçalho:
 * PBKDF2 = iterações; scrypt = N (potência de 2, com r=8 e p=1); Argon2id = memória em KiB (com t=3 e p=1).
 */
public enum DerivacaoDeChave {
    PBKDF2_SHA256(1, "PBKDF2-HMAC-SHA256", 210_000, 100_000, 100_000, 5_000_000),
    PBKDF2_SHA512(2, "PBKDF2-HMAC-SHA512", 120_000, 100_000, 100_000, 5_000_000),
    SCRYPT(3, "scrypt (N=32768, r=8, p=1)", 32_768, 16_384, 16_384, 262_144),
    ARGON2ID(4, "Argon2id (64 MiB, t=3, p=1)", 65_536, 16_384, 16_384, 262_144);

    public static final DerivacaoDeChave PADRAO = PBKDF2_SHA256;

    private final int id;
    private final String nome;
    private final int custoPadrao;
    private final int custoLeve;
    private final int custoMinimo;
    private final int custoMaximo;

    DerivacaoDeChave(int id, String nome, int custoPadrao, int custoLeve, int custoMinimo, int custoMaximo) {
        this.id = id;
        this.nome = nome;
        this.custoPadrao = custoPadrao;
        this.custoLeve = custoLeve;
        this.custoMinimo = custoMinimo;
        this.custoMaximo = custoMaximo;
    }

    public int id() { return id; }
    public String nome() { return nome; }
    public int custoPadrao() { return custoPadrao; }
    /** Custo reduzido, usado apenas em testes automatizados. */
    public int custoLeve() { return custoLeve; }

    /** Rejeita custos fora da faixa (um cabeçalho malicioso poderia pedir memória/tempo excessivos). */
    public boolean custoValido(int custo) {
        if (custo < custoMinimo || custo > custoMaximo) return false;
        return this != SCRYPT || Integer.bitCount(custo) == 1;
    }

    public static DerivacaoDeChave porId(int id) {
        for (DerivacaoDeChave d : values()) if (d.id == id) return d;
        return null;
    }

    @Override public String toString() { return nome; }
}

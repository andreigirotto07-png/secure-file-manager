package br.edu.ifrs.sfm.core.cripto;

import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.spec.AlgorithmParameterSpec;

/** Algoritmos autenticados oferecidos. O identificador é gravado no cabeçalho do arquivo. */
public enum AlgoritmoCifra {
    AES_256_GCM(1, "AES-256-GCM", "AES/GCM/NoPadding", "AES", 256),
    AES_128_GCM(2, "AES-128-GCM", "AES/GCM/NoPadding", "AES", 128),
    CHACHA20_POLY1305(3, "ChaCha20-Poly1305", "ChaCha20-Poly1305", "ChaCha20", 256);

    public static final AlgoritmoCifra PADRAO = AES_256_GCM;
    private static final int ETIQUETA_BITS = 128;

    private final int id;
    private final String nome;
    private final String transformacao;
    private final String familiaDaChave;
    private final int bitsDaChave;

    AlgoritmoCifra(int id, String nome, String transformacao, String familiaDaChave, int bitsDaChave) {
        this.id = id;
        this.nome = nome;
        this.transformacao = transformacao;
        this.familiaDaChave = familiaDaChave;
        this.bitsDaChave = bitsDaChave;
    }

    public int id() { return id; }
    public String nome() { return nome; }
    public String transformacao() { return transformacao; }
    public String familiaDaChave() { return familiaDaChave; }
    public int bitsDaChave() { return bitsDaChave; }

    public AlgorithmParameterSpec parametros(byte[] iv) {
        return this == CHACHA20_POLY1305 ? new IvParameterSpec(iv) : new GCMParameterSpec(ETIQUETA_BITS, iv);
    }

    public static AlgoritmoCifra porId(int id) {
        for (AlgoritmoCifra a : values()) if (a.id == id) return a;
        return null;
    }

    @Override public String toString() { return nome; }
}

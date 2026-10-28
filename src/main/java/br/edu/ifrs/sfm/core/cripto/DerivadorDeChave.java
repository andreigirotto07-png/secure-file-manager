package br.edu.ifrs.sfm.core.cripto;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.crypto.params.Argon2Parameters;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/** Deriva a chave do tamanho exigido pelo algoritmo, usando a função de derivação escolhida. */
public final class DerivadorDeChave {
    private DerivadorDeChave() {}

    public static SecretKey derivar(char[] senha, byte[] sal, DerivacaoDeChave kdf, int custo, AlgoritmoCifra algoritmo)
            throws GeneralSecurityException {
        int bytesDaChave = algoritmo.bitsDaChave() / 8;
        byte[] bruto = switch (kdf) {
            case PBKDF2_SHA256 -> pbkdf2("PBKDF2WithHmacSHA256", senha, sal, custo, algoritmo.bitsDaChave());
            case PBKDF2_SHA512 -> pbkdf2("PBKDF2WithHmacSHA512", senha, sal, custo, algoritmo.bitsDaChave());
            case SCRYPT -> comSenhaEmBytes(senha, p -> SCrypt.generate(p, sal, custo, 8, 1, bytesDaChave));
            case ARGON2ID -> comSenhaEmBytes(senha, p -> {
                Argon2Parameters parametros = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                        .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                        .withIterations(3).withMemoryAsKB(custo).withParallelism(1).withSalt(sal).build();
                Argon2BytesGenerator gerador = new Argon2BytesGenerator();
                gerador.init(parametros);
                byte[] saida = new byte[bytesDaChave];
                gerador.generateBytes(p, saida);
                return saida;
            });
        };
        try {
            return new SecretKeySpec(bruto, algoritmo.familiaDaChave());
        } finally {
            Arrays.fill(bruto, (byte) 0);
        }
    }

    private interface Funcao { byte[] aplicar(byte[] senhaEmBytes); }

    private static byte[] comSenhaEmBytes(char[] senha, Funcao f) {
        ByteBuffer bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap(senha));
        byte[] bytes = new byte[bb.remaining()];
        bb.get(bytes);
        try {
            return f.aplicar(bytes);
        } finally {
            Arrays.fill(bytes, (byte) 0);
            Arrays.fill(bb.array(), (byte) 0);
        }
    }

    private static byte[] pbkdf2(String nome, char[] senha, byte[] sal, int iteracoes, int bits) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(senha, sal, iteracoes, bits);
        try {
            return SecretKeyFactory.getInstance(nome).generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}

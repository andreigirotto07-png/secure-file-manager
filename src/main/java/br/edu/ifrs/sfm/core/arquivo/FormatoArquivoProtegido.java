package br.edu.ifrs.sfm.core.arquivo;

import br.edu.ifrs.sfm.core.cripto.AlgoritmoCifra;
import br.edu.ifrs.sfm.core.cripto.DerivacaoDeChave;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Formato único usado pelas duas interfaces:
 * "SFM1" | versão(1) | algoritmo(1) | derivação(1) | tipo(1: 0=arquivo, 1=pasta) | custo(4) | sal(16) | IV(12) | texto cifrado + etiqueta GCM.
 * O cabeçalho inteiro é passado como dado autenticado (AAD): alterá-lo invalida o arquivo.
 */
public final class FormatoArquivoProtegido {
    public static final String EXTENSAO = ".protegido";
    public static final int VERSAO = 1;
    public static final int TAMANHO_SAL = 16;
    public static final int TAMANHO_IV = 12;
    private static final byte[] MAGIA = "SFM1".getBytes(StandardCharsets.US_ASCII);
    public static final int TAMANHO_CABECALHO = MAGIA.length + 1 + 1 + 1 + 1 + 4 + TAMANHO_SAL + TAMANHO_IV;

    public record Cabecalho(AlgoritmoCifra algoritmo, DerivacaoDeChave derivacao, boolean pasta, int custo, byte[] sal, byte[] iv) {
        public byte[] bytes() {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(TAMANHO_CABECALHO);
            try (DataOutputStream out = new DataOutputStream(bos)) {
                escrever(out);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return bos.toByteArray();
        }

        private void escrever(DataOutputStream out) throws IOException {
            out.write(MAGIA);
            out.writeByte(VERSAO);
            out.writeByte(algoritmo.id());
            out.writeByte(derivacao.id());
            out.writeByte(pasta ? 1 : 0);
            out.writeInt(custo);
            out.write(sal);
            out.write(iv);
        }
    }

    private FormatoArquivoProtegido() {}

    /** Lê o cabeçalho; retorna null se o arquivo não tiver o formato esperado. */
    public static Cabecalho ler(DataInputStream in) throws IOException {
        byte[] magia = new byte[MAGIA.length];
        try {
            in.readFully(magia);
            if (!Arrays.equals(magia, MAGIA)) return null;
            if (in.readUnsignedByte() != VERSAO) return null;
            AlgoritmoCifra algoritmo = AlgoritmoCifra.porId(in.readUnsignedByte());
            if (algoritmo == null) return null;
            DerivacaoDeChave derivacao = DerivacaoDeChave.porId(in.readUnsignedByte());
            if (derivacao == null) return null;
            int tipo = in.readUnsignedByte();
            if (tipo > 1) return null;
            int custo = in.readInt();
            if (!derivacao.custoValido(custo)) return null;
            byte[] sal = new byte[TAMANHO_SAL];
            byte[] iv = new byte[TAMANHO_IV];
            in.readFully(sal);
            in.readFully(iv);
            return new Cabecalho(algoritmo, derivacao, tipo == 1, custo, sal, iv);
        } catch (java.io.EOFException e) {
            return null;
        }
    }

    /** Indica se o arquivo protegido contém uma pasta (false se inválido ou ilegível). */
    public static boolean contemPasta(Path arquivo) {
        try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(Files.newInputStream(arquivo)))) {
            Cabecalho c = ler(in);
            return c != null && c.pasta();
        } catch (IOException e) {
            return false;
        }
    }

    /** Nome sugerido para proteger: relatorio.pdf -> relatorio.pdf.protegido (sem sobrescrever). */
    public static Path saidaProtegida(Path entrada) {
        return semColisao(entrada.resolveSibling(entrada.getFileName() + EXTENSAO));
    }

    /** Nome sugerido para abrir: relatorio.pdf.protegido -> relatorio.pdf (sem sobrescrever). */
    public static Path saidaAberta(Path entrada) {
        String nome = entrada.getFileName().toString();
        String base = nome.endsWith(EXTENSAO) && nome.length() > EXTENSAO.length()
                ? nome.substring(0, nome.length() - EXTENSAO.length()) : nome + ".aberto";
        return semColisao(entrada.resolveSibling(base));
    }

    private static Path semColisao(Path candidato) {
        if (!Files.exists(candidato)) return candidato;
        String nome = candidato.getFileName().toString();
        int ponto = nome.lastIndexOf('.');
        String raiz = ponto > 0 ? nome.substring(0, ponto) : nome;
        String ext = ponto > 0 ? nome.substring(ponto) : "";
        for (int i = 1; i < 1000; i++) {
            Path p = candidato.resolveSibling(raiz + " (" + i + ")" + ext);
            if (!Files.exists(p)) return p;
        }
        return candidato;
    }
}

package br.edu.ifrs.sfm.core.cripto;

import br.edu.ifrs.sfm.core.arquivo.FormatoArquivoProtegido;
import br.edu.ifrs.sfm.core.arquivo.InfoDeEntrada;
import br.edu.ifrs.sfm.core.arquivo.PacoteDePasta;
import br.edu.ifrs.sfm.core.resultado.CodigoResultado;
import br.edu.ifrs.sfm.core.resultado.Resultado;
import br.edu.ifrs.sfm.core.resultado.ResumoOperacao;
import br.edu.ifrs.sfm.core.validacao.LimitesDeArquivo;
import br.edu.ifrs.sfm.core.validacao.ValidadorDeSenha;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.*;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipException;

public final class ProtecaoAesGcmService implements ProtecaoService {
    public static final String ALGORITMO = AlgoritmoCifra.PADRAO.nome();
    private static final int ETIQUETA_BITS = 128;
    private static final int BLOCO = 1024 * 1024;
    /** Folga para o ZIP de uma pasta (dados já compactados podem crescer um pouco). */
    private static final long FOLGA_PACOTE = 1024L * 1024;

    private final SecureRandom aleatorio = new SecureRandom();
    private final boolean leve;
    private final AtomicBoolean cancelado = new AtomicBoolean(false);

    public ProtecaoAesGcmService() { this(false); }

    /** Com leve=true usa custos de derivação reduzidos — apenas para testes automatizados. */
    public ProtecaoAesGcmService(boolean leve) { this.leve = leve; }

    @Override public void cancelar() { cancelado.set(true); }

    @Override
    public Resultado proteger(Path entrada, Path saida, char[] senha, char[] confirmacao,
                              boolean apagarOriginal, AlgoritmoCifra algoritmoEscolhido,
                              DerivacaoDeChave derivacaoEscolhida, Progresso progresso) {
        cancelado.set(false);
        long inicio = System.nanoTime();
        final AlgoritmoCifra algoritmo = algoritmoEscolhido == null ? AlgoritmoCifra.PADRAO : algoritmoEscolhido;
        final DerivacaoDeChave derivacao = derivacaoEscolhida == null ? DerivacaoDeChave.PADRAO : derivacaoEscolhida;
        final int custo = leve ? derivacao.custoLeve() : derivacao.custoPadrao();
        CodigoResultado validacao = ValidadorDeSenha.validar(senha, confirmacao);
        if (validacao != CodigoResultado.SUCESSO) return Resultado.falha(validacao);
        CodigoResultado pre = verificarEntradaESaida(entrada, saida, true);
        if (pre != CodigoResultado.SUCESSO) return Resultado.falha(pre);

        final boolean pasta = Files.isDirectory(entrada);
        Path temp = null;
        try {
            byte[] dados;
            if (pasta) {
                dados = PacoteDePasta.empacotar(entrada, cancelado::get);
                if (dados == null) return Resultado.falha(CodigoResultado.PASTA_VAZIA);
            } else {
                dados = Files.readAllBytes(entrada);
            }
            byte[] sal = new byte[FormatoArquivoProtegido.TAMANHO_SAL];
            byte[] iv = new byte[FormatoArquivoProtegido.TAMANHO_IV];
            aleatorio.nextBytes(sal);
            aleatorio.nextBytes(iv);
            var cab = new FormatoArquivoProtegido.Cabecalho(algoritmo, derivacao, pasta, custo, sal, iv);
            byte[] cabBytes = cab.bytes();

            notificar(progresso, 0.05);
            SecretKey chave = DerivadorDeChave.derivar(senha, sal, derivacao, custo, algoritmo);
            if (cancelado.get()) return Resultado.falha(CodigoResultado.OPERACAO_CANCELADA);
            notificar(progresso, 0.2);

            Cipher cifra = Cipher.getInstance(algoritmo.transformacao());
            cifra.init(Cipher.ENCRYPT_MODE, chave, algoritmo.parametros(iv));
            cifra.updateAAD(cabBytes);

            temp = saida.resolveSibling(saida.getFileName() + ".tmp");
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(temp,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
                out.write(cabBytes);
                for (int pos = 0; pos < dados.length; pos += BLOCO) {
                    if (cancelado.get()) { out.close(); apagarSilencioso(temp); return Resultado.falha(CodigoResultado.OPERACAO_CANCELADA); }
                    int n = Math.min(BLOCO, dados.length - pos);
                    byte[] parte = cifra.update(dados, pos, n);
                    if (parte != null) out.write(parte);
                    notificar(progresso, 0.2 + 0.75 * (pos + n) / Math.max(1, dados.length));
                }
                out.write(cifra.doFinal());
            }
            Arrays.fill(dados, (byte) 0);
            if (cancelado.get()) { apagarSilencioso(temp); return Resultado.falha(CodigoResultado.OPERACAO_CANCELADA); }
            Files.move(temp, saida, StandardCopyOption.ATOMIC_MOVE);
            temp = null;

            // Apagar o original só vale para arquivo único; pastas nunca são apagadas por segurança.
            boolean preservado = true;
            if (apagarOriginal && !pasta) {
                try { Files.delete(entrada); preservado = false; } catch (IOException e) { preservado = true; }
            }
            notificar(progresso, 1.0);
            return sucesso(saida, pasta, inicio, preservado, algoritmo, derivacao);
        } catch (CancellationException e) {
            if (temp != null) apagarSilencioso(temp);
            return Resultado.falha(CodigoResultado.OPERACAO_CANCELADA);
        } catch (IOException | GeneralSecurityException | RuntimeException | OutOfMemoryError e) {
            if (temp != null) apagarSilencioso(temp);
            return Resultado.falha(CodigoResultado.ERRO_DE_GRAVACAO);
        }
    }

    @Override
    public Resultado abrir(Path entrada, Path saida, char[] senha, Progresso progresso) {
        cancelado.set(false);
        long inicio = System.nanoTime();
        CodigoResultado pre = verificarEntradaESaida(entrada, saida, false);
        if (pre != CodigoResultado.SUCESSO) return Resultado.falha(pre);
        if (senha == null || senha.length == 0) return Resultado.falha(CodigoResultado.AUTENTICACAO_FALHOU);

        Path temp = null;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(entrada)))) {
            var cab = FormatoArquivoProtegido.ler(in);
            if (cab == null) return Resultado.falha(CodigoResultado.FORMATO_INVALIDO);
            byte[] cifrado = in.readAllBytes();
            if (cifrado.length < ETIQUETA_BITS / 8) return Resultado.falha(CodigoResultado.FORMATO_INVALIDO);

            notificar(progresso, 0.05);
            SecretKey chave = DerivadorDeChave.derivar(senha, cab.sal(), cab.derivacao(), cab.custo(), cab.algoritmo());
            if (cancelado.get()) return Resultado.falha(CodigoResultado.OPERACAO_CANCELADA);
            notificar(progresso, 0.3);

            Cipher cifra = Cipher.getInstance(cab.algoritmo().transformacao());
            cifra.init(Cipher.DECRYPT_MODE, chave, cab.algoritmo().parametros(cab.iv()));
            cifra.updateAAD(cab.bytes());
            byte[] aberto;
            try {
                aberto = cifra.doFinal(cifrado);
            } catch (AEADBadTagException e) {
                // Senha errada e arquivo adulterado são indistinguíveis no GCM.
                return Resultado.falha(CodigoResultado.AUTENTICACAO_FALHOU);
            }
            if (cancelado.get()) return Resultado.falha(CodigoResultado.OPERACAO_CANCELADA);
            notificar(progresso, 0.8);

            temp = saida.resolveSibling(saida.getFileName() + ".tmp");
            if (cab.pasta()) {
                PacoteDePasta.apagarArvore(temp);
                Files.createDirectory(temp);
                PacoteDePasta.extrair(aberto, temp, cancelado::get, LimitesDeArquivo.TAMANHO_MAXIMO_BYTES + FOLGA_PACOTE);
            } else {
                Files.write(temp, aberto, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            }
            Arrays.fill(aberto, (byte) 0);
            if (cancelado.get()) { apagarSilencioso(temp); return Resultado.falha(CodigoResultado.OPERACAO_CANCELADA); }
            Files.move(temp, saida, StandardCopyOption.ATOMIC_MOVE);
            temp = null;
            notificar(progresso, 1.0);
            return sucesso(saida, cab.pasta(), inicio, true, cab.algoritmo(), cab.derivacao());
        } catch (CancellationException e) {
            if (temp != null) apagarSilencioso(temp);
            return Resultado.falha(CodigoResultado.OPERACAO_CANCELADA);
        } catch (ZipException e) {
            if (temp != null) apagarSilencioso(temp);
            return Resultado.falha(CodigoResultado.FORMATO_INVALIDO);
        } catch (IOException | GeneralSecurityException | RuntimeException | OutOfMemoryError e) {
            if (temp != null) apagarSilencioso(temp);
            return Resultado.falha(CodigoResultado.ERRO_DE_GRAVACAO);
        }
    }

    private CodigoResultado verificarEntradaESaida(Path entrada, Path saida, boolean protegendo) {
        if (entrada == null) return CodigoResultado.ARQUIVO_NAO_ENCONTRADO;
        if (protegendo ? !(Files.isRegularFile(entrada) || Files.isDirectory(entrada)) : !Files.isRegularFile(entrada))
            return CodigoResultado.ARQUIVO_NAO_ENCONTRADO;
        // Limite: arquivo (ou soma dos arquivos da pasta) ao proteger; arquivo protegido com folga ao abrir.
        long tamanho = protegendo ? InfoDeEntrada.de(entrada).tamanhoBytes() : tamanhoSeguro(entrada);
        long limite = protegendo ? LimitesDeArquivo.TAMANHO_MAXIMO_BYTES : LimitesDeArquivo.TAMANHO_MAXIMO_BYTES + FOLGA_PACOTE;
        if (tamanho > limite) return CodigoResultado.ARQUIVO_MUITO_GRANDE;
        if (saida == null || Files.exists(saida) || saida.getParent() != null && !Files.isDirectory(saida.getParent()))
            return CodigoResultado.ERRO_DE_GRAVACAO;
        return CodigoResultado.SUCESSO;
    }

    private static long tamanhoSeguro(Path p) {
        try { return Files.size(p); } catch (IOException e) { return Long.MAX_VALUE; }
    }

    /** "pasta" indica que o conteúdo (da entrada ao proteger, da saída ao abrir) é uma pasta. */
    private Resultado sucesso(Path saida, boolean pasta, long inicioNs, boolean originalPreservado,
                              AlgoritmoCifra algoritmo, DerivacaoDeChave derivacao) {
        long ms = (System.nanoTime() - inicioNs) / 1_000_000;
        return new Resultado(CodigoResultado.SUCESSO, saida,
                new ResumoOperacao(saida, InfoDeEntrada.de(saida).tamanhoBytes(), pasta, algoritmo.nome(), derivacao.nome(), ms, originalPreservado));
    }

    private static void notificar(Progresso p, double f) { if (p != null) p.aoProgredir(Math.min(1.0, f)); }

    private static void apagarSilencioso(Path p) {
        try {
            if (Files.isDirectory(p)) PacoteDePasta.apagarArvore(p); else Files.deleteIfExists(p);
        } catch (IOException ignored) { }
    }
}

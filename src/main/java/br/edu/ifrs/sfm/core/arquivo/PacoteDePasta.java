package br.edu.ifrs.sfm.core.arquivo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.zip.*;

/** Reúne os arquivos de uma pasta em um ZIP (em memória) e recria a pasta ao abrir. */
public final class PacoteDePasta {
    private PacoteDePasta() {}

    /** Retorna o ZIP, ou null se a pasta não tiver nenhum arquivo. Lança CancellationException ao cancelar. */
    public static byte[] empacotar(Path pasta, BooleanSupplier cancelado) throws IOException {
        String base = pasta.getFileName().toString();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int[] qtd = {0};
        try (ZipOutputStream zip = new ZipOutputStream(bos, StandardCharsets.UTF_8)) {
            Files.walkFileTree(pasta, new SimpleFileVisitor<>() {
                @Override public FileVisitResult visitFile(Path f, BasicFileAttributes a) throws IOException {
                    if (cancelado.getAsBoolean()) throw new CancellationException();
                    if (!a.isRegularFile()) return FileVisitResult.CONTINUE; // ignora links e arquivos especiais
                    StringBuilder nome = new StringBuilder(base);
                    for (Path parte : pasta.relativize(f)) nome.append('/').append(parte);
                    zip.putNextEntry(new ZipEntry(nome.toString()));
                    Files.copy(f, zip);
                    zip.closeEntry();
                    qtd[0]++;
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFileFailed(Path f, IOException e) { return FileVisitResult.CONTINUE; }
            });
        }
        return qtd[0] == 0 ? null : bos.toByteArray();
    }

    /**
     * Extrai para "destino" (que deve ser uma pasta já criada e vazia), removendo o primeiro nível do caminho.
     * Lança ZipException se houver caminho fora do destino ou se o total passar de limiteBytes.
     */
    public static void extrair(byte[] zip, Path destino, BooleanSupplier cancelado, long limiteBytes) throws IOException {
        Path raiz = destino.toAbsolutePath().normalize();
        long total = 0;
        byte[] buf = new byte[64 * 1024];
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                if (cancelado.getAsBoolean()) throw new CancellationException();
                String nome = e.getName();
                if (nome.contains("\\") || nome.contains(":") || nome.startsWith("/")) throw new ZipException("nome inválido");
                int barra = nome.indexOf('/');
                String relativo = barra >= 0 ? nome.substring(barra + 1) : nome;
                if (relativo.isEmpty()) continue;
                Path alvo = raiz.resolve(relativo).normalize();
                if (!alvo.startsWith(raiz) || alvo.equals(raiz)) throw new ZipException("caminho fora da pasta de destino");
                if (e.isDirectory()) { Files.createDirectories(alvo); continue; }
                Files.createDirectories(alvo.getParent());
                try (OutputStream out = Files.newOutputStream(alvo, StandardOpenOption.CREATE_NEW)) {
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        total += n;
                        if (total > limiteBytes) throw new ZipException("conteúdo maior que o permitido");
                        out.write(buf, 0, n);
                    }
                }
            }
        }
    }

    public static void apagarArvore(Path raiz) {
        if (raiz == null || !Files.exists(raiz)) return;
        try {
            Files.walkFileTree(raiz, new SimpleFileVisitor<>() {
                @Override public FileVisitResult visitFile(Path f, BasicFileAttributes a) throws IOException { Files.deleteIfExists(f); return FileVisitResult.CONTINUE; }
                @Override public FileVisitResult postVisitDirectory(Path d, IOException e) throws IOException { Files.deleteIfExists(d); return FileVisitResult.CONTINUE; }
            });
        } catch (IOException ignorada) { /* melhor esforço */ }
    }
}

package br.edu.ifrs.sfm.core.arquivo;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/** O que será protegido: um arquivo ou uma pasta (somando só os arquivos regulares; links são ignorados). */
public record InfoDeEntrada(boolean pasta, long tamanhoBytes, int arquivos) {

    public static InfoDeEntrada de(Path p) {
        try {
            if (Files.isDirectory(p)) {
                long[] soma = {0};
                int[] qtd = {0};
                Files.walkFileTree(p, new SimpleFileVisitor<>() {
                    @Override public FileVisitResult visitFile(Path f, BasicFileAttributes a) {
                        if (a.isRegularFile()) { soma[0] += a.size(); qtd[0]++; }
                        return FileVisitResult.CONTINUE;
                    }
                    @Override public FileVisitResult visitFileFailed(Path f, IOException e) { return FileVisitResult.CONTINUE; }
                });
                return new InfoDeEntrada(true, soma[0], qtd[0]);
            }
            return new InfoDeEntrada(false, Files.size(p), 1);
        } catch (IOException e) {
            return new InfoDeEntrada(false, 0, 0);
        }
    }
}

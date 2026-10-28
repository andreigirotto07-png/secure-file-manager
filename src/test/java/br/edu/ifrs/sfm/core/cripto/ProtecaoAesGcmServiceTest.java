package br.edu.ifrs.sfm.core.cripto;

import br.edu.ifrs.sfm.core.arquivo.FormatoArquivoProtegido;
import br.edu.ifrs.sfm.core.arquivo.InfoDeEntrada;
import br.edu.ifrs.sfm.core.arquivo.PacoteDePasta;
import br.edu.ifrs.sfm.core.resultado.CodigoResultado;
import br.edu.ifrs.sfm.core.resultado.Resultado;
import br.edu.ifrs.sfm.core.validacao.ForcaDeSenha;
import br.edu.ifrs.sfm.core.validacao.LimitesDeArquivo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ProtecaoAesGcmServiceTest {
    @TempDir Path dir;
    ProtecaoAesGcmService servico;
    Path original;
    final byte[] conteudo = "conteúdo confidencial de teste".repeat(1000).getBytes();

    @BeforeEach
    void preparar() throws IOException {
        servico = new ProtecaoAesGcmService(true);
        original = dir.resolve("relatorio.pdf");
        Files.write(original, conteudo);
    }

    private Resultado proteger(String senha) {
        return servico.proteger(original, FormatoArquivoProtegido.saidaProtegida(original),
                senha.toCharArray(), senha.toCharArray(), false, null);
    }

    @Test
    void ida_e_volta_recupera_o_conteudo_original() throws IOException {
        Resultado p = proteger("senha-forte-1");
        assertEquals(CodigoResultado.SUCESSO, p.codigo());
        assertTrue(p.arquivoGerado().getFileName().toString().endsWith(".protegido"));
        assertFalse(Arrays.equals(conteudo, Files.readAllBytes(p.arquivoGerado())));

        Path saida = dir.resolve("recuperado.pdf");
        Resultado a = servico.abrir(p.arquivoGerado(), saida, "senha-forte-1".toCharArray(), null);
        assertEquals(CodigoResultado.SUCESSO, a.codigo());
        assertArrayEquals(conteudo, Files.readAllBytes(saida));
    }

    @Test
    void original_e_preservado_por_padrao() throws IOException {
        Resultado p = proteger("senha-forte-1");
        assertTrue(Files.exists(original));
        assertTrue(p.resumo().originalPreservado());
        assertArrayEquals(conteudo, Files.readAllBytes(original));
    }

    @Test
    void original_so_e_apagado_com_opcao_explicita() {
        Resultado p = servico.proteger(original, FormatoArquivoProtegido.saidaProtegida(original),
                "senha-forte-1".toCharArray(), "senha-forte-1".toCharArray(), true, null);
        assertEquals(CodigoResultado.SUCESSO, p.codigo());
        assertFalse(Files.exists(original));
        assertFalse(p.resumo().originalPreservado());
    }

    @Test
    void senha_errada_falha_sem_criar_arquivo() throws IOException {
        Resultado p = proteger("senha-forte-1");
        Path saida = dir.resolve("saida.pdf");
        Resultado a = servico.abrir(p.arquivoGerado(), saida, "senha-errada-9".toCharArray(), null);
        assertEquals(CodigoResultado.AUTENTICACAO_FALHOU, a.codigo());
        assertFalse(Files.exists(saida));
        assertFalse(Files.exists(dir.resolve("saida.pdf.tmp")));
    }

    @Test
    void arquivo_adulterado_falha_na_autenticacao() throws IOException {
        Resultado p = proteger("senha-forte-1");
        byte[] bytes = Files.readAllBytes(p.arquivoGerado());
        bytes[bytes.length / 2] ^= 0x01;
        Files.write(p.arquivoGerado(), bytes);
        Resultado a = servico.abrir(p.arquivoGerado(), dir.resolve("saida.pdf"), "senha-forte-1".toCharArray(), null);
        assertEquals(CodigoResultado.AUTENTICACAO_FALHOU, a.codigo());
    }

    @Test
    void cabecalho_adulterado_tambem_e_detectado() throws IOException {
        Resultado p = proteger("senha-forte-1");
        byte[] bytes = Files.readAllBytes(p.arquivoGerado());
        bytes[FormatoArquivoProtegido.TAMANHO_CABECALHO - 1] ^= 0x01; // último byte do IV
        Files.write(p.arquivoGerado(), bytes);
        Resultado a = servico.abrir(p.arquivoGerado(), dir.resolve("saida.pdf"), "senha-forte-1".toCharArray(), null);
        assertEquals(CodigoResultado.AUTENTICACAO_FALHOU, a.codigo());
    }

    @Test
    void arquivo_que_nao_e_protegido_tem_formato_invalido() throws IOException {
        Path qualquer = dir.resolve("foto.png.protegido");
        Files.write(qualquer, "isto não é um arquivo protegido, só texto comum qualquer".getBytes());
        Resultado a = servico.abrir(qualquer, dir.resolve("foto.png"), "senha-forte-1".toCharArray(), null);
        assertEquals(CodigoResultado.FORMATO_INVALIDO, a.codigo());
    }

    @Test
    void duas_protecoes_do_mesmo_arquivo_geram_saidas_diferentes() throws IOException {
        Resultado p1 = proteger("senha-forte-1");
        Resultado p2 = proteger("senha-forte-1");
        assertNotEquals(p1.arquivoGerado(), p2.arquivoGerado());
        assertFalse(Arrays.equals(Files.readAllBytes(p1.arquivoGerado()), Files.readAllBytes(p2.arquivoGerado())));
    }

    @Test
    void senha_curta_e_confirmacao_divergente_sao_recusadas() {
        Path saida = FormatoArquivoProtegido.saidaProtegida(original);
        assertEquals(CodigoResultado.SENHA_FRACA,
                servico.proteger(original, saida, "curta".toCharArray(), "curta".toCharArray(), false, null).codigo());
        assertEquals(CodigoResultado.CONFIRMACAO_DIVERGENTE,
                servico.proteger(original, saida, "senha-forte-1".toCharArray(), "senha-forte-2".toCharArray(), false, null).codigo());
        assertFalse(Files.exists(saida));
    }

    @Test
    void arquivo_inexistente_e_informado() {
        Resultado r = servico.proteger(dir.resolve("nao-existe.txt"), dir.resolve("x.protegido"),
                "senha-forte-1".toCharArray(), "senha-forte-1".toCharArray(), false, null);
        assertEquals(CodigoResultado.ARQUIVO_NAO_ENCONTRADO, r.codigo());
    }

    @Test
    void arquivo_acima_do_limite_e_recusado_sem_deixar_saida() throws IOException {
        Path grande = dir.resolve("grande.bin");
        try (var ch = java.nio.channels.FileChannel.open(grande,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE)) {
            ch.position(LimitesDeArquivo.TAMANHO_MAXIMO_BYTES); // arquivo esparso de limite + 1 byte
            ch.write(java.nio.ByteBuffer.wrap(new byte[]{1}));
        }
        Path saida = dir.resolve("grande.bin.protegido");
        Resultado r = servico.proteger(grande, saida, "senha-forte-1".toCharArray(), "senha-forte-1".toCharArray(), false, null);
        assertEquals(CodigoResultado.ARQUIVO_MUITO_GRANDE, r.codigo());
        assertFalse(Files.exists(saida));
    }

    @Test
    void cancelamento_nao_deixa_arquivo_parcial_e_preserva_o_original() throws IOException {
        Path saida = FormatoArquivoProtegido.saidaProtegida(original);
        Resultado r = servico.proteger(original, saida, "senha-forte-1".toCharArray(), "senha-forte-1".toCharArray(),
                false, f -> servico.cancelar()); // cancela já no primeiro aviso de progresso
        assertEquals(CodigoResultado.OPERACAO_CANCELADA, r.codigo());
        assertFalse(Files.exists(saida));
        try (Stream<Path> s = Files.list(dir)) {
            assertTrue(s.noneMatch(p -> p.toString().endsWith(".tmp")));
        }
        assertArrayEquals(conteudo, Files.readAllBytes(original));
    }

    @Test
    void nao_sobrescreve_arquivo_existente() throws IOException {
        Path saida = dir.resolve("ja-existe.protegido");
        Files.writeString(saida, "importante");
        Resultado r = servico.proteger(original, saida, "senha-forte-1".toCharArray(), "senha-forte-1".toCharArray(), false, null);
        assertEquals(CodigoResultado.ERRO_DE_GRAVACAO, r.codigo());
        assertEquals("importante", Files.readString(saida));
    }

    @Test
    void nomes_sugeridos_nao_colidem() throws IOException {
        assertEquals("relatorio.pdf.protegido", FormatoArquivoProtegido.saidaProtegida(original).getFileName().toString());
        Files.writeString(dir.resolve("relatorio.pdf.protegido"), "x");
        assertEquals("relatorio.pdf (1).protegido", FormatoArquivoProtegido.saidaProtegida(original).getFileName().toString());
        assertEquals("relatorio (1).pdf", FormatoArquivoProtegido.saidaAberta(dir.resolve("relatorio.pdf.protegido")).getFileName().toString());
    }


    @Test
    void todos_os_algoritmos_fazem_ida_e_volta_e_sao_detectados_na_abertura() throws IOException {
        for (AlgoritmoCifra alg : AlgoritmoCifra.values()) {
            Path saidaP = dir.resolve("a-" + alg.id() + ".protegido");
            Resultado p = servico.proteger(original, saidaP, "senha-forte-1".toCharArray(), "senha-forte-1".toCharArray(), false, alg, null);
            assertEquals(CodigoResultado.SUCESSO, p.codigo());
            assertEquals(alg.nome(), p.resumo().algoritmo());
            Path rec = dir.resolve("r-" + alg.id() + ".pdf");
            Resultado a = servico.abrir(saidaP, rec, "senha-forte-1".toCharArray(), null);
            assertEquals(CodigoResultado.SUCESSO, a.codigo());
            assertEquals(alg.nome(), a.resumo().algoritmo());
            assertArrayEquals(conteudo, Files.readAllBytes(rec));
            Resultado errada = servico.abrir(saidaP, dir.resolve("e-" + alg.id() + ".pdf"), "senha-errada-9".toCharArray(), null);
            assertEquals(CodigoResultado.AUTENTICACAO_FALHOU, errada.codigo());
        }
    }


    @Test
    void todas_as_derivacoes_fazem_ida_e_volta_e_sao_detectadas_na_abertura() throws IOException {
        for (DerivacaoDeChave kdf : DerivacaoDeChave.values()) {
            Path saidaP = dir.resolve("k-" + kdf.id() + ".protegido");
            Resultado p = servico.proteger(original, saidaP, "senha-forte-1".toCharArray(), "senha-forte-1".toCharArray(),
                    false, AlgoritmoCifra.AES_256_GCM, kdf, null);
            assertEquals(CodigoResultado.SUCESSO, p.codigo());
            assertEquals(kdf.nome(), p.resumo().derivacao());
            Path rec = dir.resolve("kr-" + kdf.id() + ".pdf");
            Resultado a = servico.abrir(saidaP, rec, "senha-forte-1".toCharArray(), null);
            assertEquals(CodigoResultado.SUCESSO, a.codigo());
            assertEquals(kdf.nome(), a.resumo().derivacao());
            assertArrayEquals(conteudo, Files.readAllBytes(rec));
            assertEquals(CodigoResultado.AUTENTICACAO_FALHOU,
                    servico.abrir(saidaP, dir.resolve("ke-" + kdf.id() + ".pdf"), "senha-errada-9".toCharArray(), null).codigo());
        }
    }

    @Test
    void cabecalho_com_custo_absurdo_e_recusado_como_formato_invalido() throws IOException {
        Resultado p = proteger("senha-forte-1");
        byte[] bytes = Files.readAllBytes(p.arquivoGerado());
        int pos = 4 + 1 + 1 + 1 + 1; // início do campo de custo (após magia, versão, algoritmo, derivação, tipo)
        bytes[pos] = 0x7f; // custo gigantesco
        Files.write(p.arquivoGerado(), bytes);
        Resultado a = servico.abrir(p.arquivoGerado(), dir.resolve("x.pdf"), "senha-forte-1".toCharArray(), null);
        assertEquals(CodigoResultado.FORMATO_INVALIDO, a.codigo());
    }

    private Path criarPasta() throws IOException {
        Path pasta = dir.resolve("Fotos");
        Files.createDirectories(pasta.resolve("2026/viagem"));
        Files.writeString(pasta.resolve("leia-me.txt"), "olá");
        Files.write(pasta.resolve("2026/foto1.jpg"), new byte[]{1, 2, 3, 4, 5});
        Files.write(pasta.resolve("2026/viagem/foto2.jpg"), conteudo);
        return pasta;
    }

    @Test
    void pasta_ida_e_volta_recria_a_estrutura_e_preserva_o_original() throws IOException {
        Path pasta = criarPasta();
        Path protegido = FormatoArquivoProtegido.saidaProtegida(pasta);
        assertEquals("Fotos.protegido", protegido.getFileName().toString());
        Resultado p = servico.proteger(pasta, protegido, "senha-forte-1".toCharArray(), "senha-forte-1".toCharArray(), true, null);
        assertEquals(CodigoResultado.SUCESSO, p.codigo());
        assertTrue(p.resumo().pasta());
        assertTrue(Files.isDirectory(pasta)); // pasta original nunca é apagada
        assertTrue(p.resumo().originalPreservado());

        Path saida = FormatoArquivoProtegido.saidaAberta(protegido); // "Fotos" já existe -> "Fotos (1)"
        assertEquals("Fotos (1)", saida.getFileName().toString());
        Resultado a = servico.abrir(protegido, saida, "senha-forte-1".toCharArray(), null);
        assertEquals(CodigoResultado.SUCESSO, a.codigo());
        assertTrue(a.resumo().pasta());
        assertEquals("olá", Files.readString(saida.resolve("leia-me.txt")));
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, Files.readAllBytes(saida.resolve("2026/foto1.jpg")));
        assertArrayEquals(conteudo, Files.readAllBytes(saida.resolve("2026/viagem/foto2.jpg")));
        assertFalse(Files.exists(dir.resolve("Fotos (1).tmp")));
    }

    @Test
    void pasta_com_senha_errada_nao_cria_pasta() throws IOException {
        Path pasta = criarPasta();
        Path protegido = FormatoArquivoProtegido.saidaProtegida(pasta);
        servico.proteger(pasta, protegido, "senha-forte-1".toCharArray(), "senha-forte-1".toCharArray(), false, null);
        Path saida = dir.resolve("recuperada");
        Resultado a = servico.abrir(protegido, saida, "senha-errada-9".toCharArray(), null);
        assertEquals(CodigoResultado.AUTENTICACAO_FALHOU, a.codigo());
        assertFalse(Files.exists(saida));
        assertFalse(Files.exists(dir.resolve("recuperada.tmp")));
    }

    @Test
    void pasta_vazia_e_recusada() throws IOException {
        Path vazia = Files.createDirectories(dir.resolve("Vazia/sub"));
        Path protegido = dir.resolve("Vazia.protegido");
        Resultado r = servico.proteger(dir.resolve("Vazia"), protegido, "senha-forte-1".toCharArray(), "senha-forte-1".toCharArray(), false, null);
        assertEquals(CodigoResultado.PASTA_VAZIA, r.codigo());
        assertFalse(Files.exists(protegido));
        assertFalse(Files.exists(dir.resolve("Vazia.protegido.tmp")));
    }

    @Test
    void limite_vale_para_o_total_da_pasta() throws IOException {
        Path pasta = Files.createDirectories(dir.resolve("Grande"));
        for (int i = 0; i < 2; i++) {
            try (var ch = java.nio.channels.FileChannel.open(pasta.resolve("parte" + i + ".bin"),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE)) {
                ch.position(LimitesDeArquivo.TAMANHO_MAXIMO_BYTES / 2 + 10); // juntos passam do limite
                ch.write(java.nio.ByteBuffer.wrap(new byte[]{1}));
            }
        }
        Path protegido = dir.resolve("Grande.protegido");
        Resultado r = servico.proteger(pasta, protegido, "senha-forte-1".toCharArray(), "senha-forte-1".toCharArray(), false, null);
        assertEquals(CodigoResultado.ARQUIVO_MUITO_GRANDE, r.codigo());
        assertFalse(Files.exists(protegido));
    }

    @Test
    void extracao_recusa_caminho_fora_da_pasta_de_destino() throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream z = new java.util.zip.ZipOutputStream(bos)) {
            z.putNextEntry(new java.util.zip.ZipEntry("Pasta/../../fora.txt"));
            z.write(1);
            z.closeEntry();
        }
        Path destino = Files.createDirectories(dir.resolve("destino"));
        boolean recusou = false;
        try {
            PacoteDePasta.extrair(bos.toByteArray(), destino, () -> false, 1000);
        } catch (java.util.zip.ZipException e) {
            recusou = true;
        }
        assertTrue(recusou);
        assertFalse(Files.exists(dir.resolve("fora.txt")));
    }

    @Test
    void informacoes_de_entrada_somam_os_arquivos_da_pasta() throws IOException {
        Path pasta = criarPasta();
        InfoDeEntrada info = InfoDeEntrada.de(pasta);
        assertTrue(info.pasta());
        assertEquals(3, info.arquivos());
        assertEquals("olá".getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 5L + conteudo.length, info.tamanhoBytes());
        assertFalse(InfoDeEntrada.de(original).pasta());
    }
}

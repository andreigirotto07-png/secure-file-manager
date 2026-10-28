package br.edu.ifrs.sfm.ui.ajuda;

import br.edu.ifrs.sfm.ui.CatalogoDeMensagens;
import br.edu.ifrs.sfm.ui.tarefas.CatalogoTarefas;
import br.edu.ifrs.sfm.ui.tecnica.CatalogoTecnico;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConteudoDeAjudaTest {

    private static String todoTexto(ConteudoDeAjuda c) {
        StringBuilder sb = new StringBuilder(c.titulo()).append(' ').append(c.subtitulo());
        for (SecaoDeAjuda s : c.secoes()) {
            sb.append(' ').append(s.titulo());
            for (BlocoDeAjuda b : s.blocos()) {
                if (b instanceof BlocoDeAjuda.Passos p) p.itens().forEach(i -> sb.append(' ').append(i));
                else if (b instanceof BlocoDeAjuda.Paragrafo p) sb.append(' ').append(p.texto());
                else if (b instanceof BlocoDeAjuda.Dica d) sb.append(' ').append(d.texto());
                else if (b instanceof BlocoDeAjuda.Aviso a) sb.append(' ').append(a.texto());
                else if (b instanceof BlocoDeAjuda.Termos t) t.itens().forEach(i -> sb.append(' ').append(i.nome()).append(' ').append(i.descricao()));
            }
        }
        return sb.toString();
    }

    private static void validaEstrutura(CatalogoDeMensagens catalogo) {
        ConteudoDeAjuda c = catalogo.ajuda();
        assertFalse(c.secoes().isEmpty());
        for (SecaoDeAjuda s : c.secoes()) {
            assertFalse(s.titulo().isBlank());
            assertFalse(s.blocos().isEmpty());
        }
    }

    @Test
    void ajuda_das_duas_interfaces_tem_secoes_e_blocos() {
        validaEstrutura(new CatalogoTarefas());
        validaEstrutura(new CatalogoTecnico());
    }

    @Test
    void ajuda_informa_o_limite_de_tamanho() {
        assertTrue(todoTexto(new CatalogoTarefas().ajuda()).contains("100 MB"));
        assertTrue(todoTexto(new CatalogoTecnico().ajuda()).contains("100 MB"));
    }

    @Test
    void ajuda_tecnica_descreve_todos_os_algoritmos_e_derivacoes() {
        String t = todoTexto(new CatalogoTecnico().ajuda());
        for (br.edu.ifrs.sfm.core.cripto.AlgoritmoCifra a : br.edu.ifrs.sfm.core.cripto.AlgoritmoCifra.values())
            assertTrue(t.contains(a.nome()));
        for (String d : new String[]{"PBKDF2-HMAC-SHA256", "PBKDF2-HMAC-SHA512", "scrypt", "Argon2id"})
            assertTrue(t.contains(d));
    }

    @Test
    void ajuda_de_tarefas_nao_expoe_termos_tecnicos() {
        String t = todoTexto(new CatalogoTarefas().ajuda());
        for (String proibido : new String[]{"AES", "PBKDF2", "scrypt", "Argon2", "GCM", "ChaCha"})
            assertFalse(t.contains(proibido));
    }
}

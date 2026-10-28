package br.edu.ifrs.sfm.ui.tecnica;

import br.edu.ifrs.sfm.core.resultado.CodigoResultado;
import br.edu.ifrs.sfm.ui.ajuda.BlocoDeAjuda.*;
import br.edu.ifrs.sfm.ui.ajuda.ConteudoDeAjuda;
import br.edu.ifrs.sfm.ui.ajuda.SecaoDeAjuda;

import br.edu.ifrs.sfm.core.validacao.ForcaDeSenha;
import br.edu.ifrs.sfm.core.validacao.LimitesDeArquivo;
import br.edu.ifrs.sfm.ui.CatalogoDeMensagens;

public final class CatalogoTecnico implements CatalogoDeMensagens {
    @Override
    public String mensagem(CodigoResultado c) {
        return switch (c) {
            case SUCESSO -> "Operação concluída.";
            case SENHA_FRACA -> "Senha rejeitada: mínimo de 8 caracteres.";
            case CONFIRMACAO_DIVERGENTE -> "Senha e confirmação não coincidem.";
            case AUTENTICACAO_FALHOU -> "Falha de autenticação (tag GCM inválida): senha incorreta ou arquivo corrompido/alterado.";
            case ARQUIVO_NAO_ENCONTRADO -> "Arquivo de entrada não encontrado ou ilegível.";
            case ARQUIVO_MUITO_GRANDE -> "Arquivo excede o limite de " + LimitesDeArquivo.tamanhoMaximoTexto() + " (processamento em memória).";
            case PASTA_VAZIA -> "Pasta de entrada sem arquivos regulares (links e itens especiais são ignorados).";
            case ERRO_DE_GRAVACAO -> "Erro de gravação: verifique permissões, espaço em disco e se o arquivo de saída já existe.";
            case OPERACAO_CANCELADA -> "Operação cancelada pelo usuário. Nenhum arquivo de saída foi mantido.";
            case FORMATO_INVALIDO -> "Formato inválido: cabeçalho SFM1 não encontrado.";
        };
    }

    @Override
    public String legendaTamanhoMaximo() {
        return "Tamanho máximo: " + LimitesDeArquivo.tamanhoMaximoTexto();
    }

    @Override
    public String forcaDaSenha(ForcaDeSenha f) {
        return switch (f) {
            case VAZIA -> "Força da senha: —";
            case FRACA -> "Força da senha: FRACA (curta ou pouco variada)";
            case MEDIA -> "Força da senha: MÉDIA";
            case FORTE -> "Força da senha: FORTE";
        };
    }

    @Override public String dicaArrastar() { return "Selecione ou arraste um arquivo ou pasta para esta janela"; }
    @Override public String arrasteVariosItens() { return "Arraste um único arquivo ou pasta por vez."; }
    @Override public String arrasteParaAbrirInvalido() { return "Para descriptografar, use um arquivo com a extensão .protegido."; }

    @Override
    public ConteudoDeAjuda ajuda() {
        String limite = LimitesDeArquivo.tamanhoMaximoTexto();
        return new ConteudoDeAjuda("Ajuda — Modo técnico",
                "Passo a passo, opções de algoritmo e derivação de chave",
                java.util.List.of(
                SecaoDeAjuda.de("Criptografar",
                        Passos.de(
                                "Em \"Operação\", selecione Criptografar.",
                                "Em \"Arquivo de entrada\", clique em Procurar... e escolha um arquivo ou uma pasta (o programa identifica sozinho). Também vale digitar o caminho ou arrastar para a janela. Tamanho máximo: " + limite + " (no total, para pastas).",
                                "Confira o \"Arquivo de saída\". Ele é sugerido (nome + .protegido) e nunca sobrescreve um arquivo existente. Use Procurar... para trocar a pasta ou o nome.",
                                "Escolha o \"Algoritmo\" e a \"Derivação de chave\" (veja as abas ao lado).",
                                "Digite a senha (mínimo de 8 caracteres) e repita em \"Confirmar senha\". O indicador mostra a força: fraca, média ou forte.",
                                "Marque \"Apagar o arquivo original\" só se quiser removê-lo depois. Por padrão o original é mantido.",
                                "Clique em Executar. Barra de progresso e log mostram o andamento; Cancelar interrompe sem deixar arquivo parcial."),
                        new Dica("O tamanho do arquivo escolhido aparece logo abaixo do campo de entrada.")),
                SecaoDeAjuda.de("Descriptografar",
                        Passos.de(
                                "Em \"Operação\", selecione Descriptografar.",
                                "Escolha o arquivo .protegido em \"Arquivo de entrada\". Algoritmo e derivação são lidos do próprio arquivo.",
                                "Confira o \"Arquivo de saída\", digite a senha e clique em Executar."),
                        new Dica("Arrastar um arquivo .protegido para a janela já seleciona Descriptografar.")),
                SecaoDeAjuda.de("Pastas",
                        new Paragrafo("Ao criptografar uma pasta, todos os arquivos dentro dela, inclusive os das subpastas, são reunidos em um único arquivo .protegido. Ao descriptografar, a pasta é recriada com a mesma estrutura."),
                        new Dica("Links e pastas vazias não são incluídos."),
                        new Aviso("A pasta original nunca é apagada pelo programa, mesmo que você queira. A opção de apagar vale só para arquivo único.")),
                SecaoDeAjuda.de("Algoritmos",
                        new Paragrafo("Todos são cifras autenticadas: além de esconder o conteúdo, detectam se o arquivo foi alterado."),
                        Termos.de(
                                Termo.padrao("AES-256-GCM", "Chave de 256 bits. Escolha recomendada para uso geral."),
                                Termo.de("AES-128-GCM", "Chave de 128 bits. Um pouco mais rápida; ainda considerada segura."),
                                Termo.de("ChaCha20-Poly1305", "Alternativa moderna, rápida também em computadores sem aceleração de AES."))),
                SecaoDeAjuda.de("Derivação de chave",
                        new Paragrafo("A derivação transforma a senha na chave de criptografia, de forma lenta de propósito, para dificultar tentativas de adivinhar a senha."),
                        Termos.de(
                                Termo.padrao("PBKDF2-HMAC-SHA256", "Padrão amplamente adotado; custo em iterações (210.000)."),
                                Termo.de("PBKDF2-HMAC-SHA512", "Mesma família, com SHA-512 (120.000 iterações)."),
                                Termo.de("scrypt", "Exige memória (32 MiB), o que encarece ataques com hardware dedicado."),
                                Termo.de("Argon2id", "Moderna; exige memória (64 MiB) e tempo, e resiste bem a ataques com GPUs."))),
                SecaoDeAjuda.de("Cuidados",
                        new Aviso("Sem a senha não há como recuperar o conteúdo."),
                        new Dica("Falha de autenticação: com cifra autenticada, senha incorreta e arquivo alterado produzem o mesmo erro. Confira a senha e o Caps Lock."),
                        new Paragrafo("O programa processa o arquivo em memória; por isso existe o limite de " + limite + "."))));
    }
}

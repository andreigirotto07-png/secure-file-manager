package br.edu.ifrs.sfm.ui.tarefas;

import br.edu.ifrs.sfm.core.resultado.CodigoResultado;
import br.edu.ifrs.sfm.ui.ajuda.BlocoDeAjuda.*;
import br.edu.ifrs.sfm.ui.ajuda.ConteudoDeAjuda;
import br.edu.ifrs.sfm.ui.ajuda.SecaoDeAjuda;

import br.edu.ifrs.sfm.core.validacao.ForcaDeSenha;
import br.edu.ifrs.sfm.core.validacao.LimitesDeArquivo;
import br.edu.ifrs.sfm.ui.CatalogoDeMensagens;

public final class CatalogoTarefas implements CatalogoDeMensagens {
    @Override
    public String mensagem(CodigoResultado c) {
        return switch (c) {
            case SUCESSO -> "Tudo certo.";
            case SENHA_FRACA -> "A senha precisa ter pelo menos 8 caracteres.";
            case CONFIRMACAO_DIVERGENTE -> "As duas senhas não são iguais. Digite de novo.";
            case AUTENTICACAO_FALHOU -> "Não foi possível abrir o arquivo. A senha pode estar incorreta ou o arquivo pode ter sido alterado. "
                    + "Confira a senha (e o Caps Lock) e tente de novo.";
            case ARQUIVO_NAO_ENCONTRADO -> "Não encontramos esse arquivo. Ele pode ter sido movido ou apagado.";
            case ARQUIVO_MUITO_GRANDE -> "Esse arquivo é grande demais. O tamanho máximo é " + LimitesDeArquivo.tamanhoMaximoTexto() + ".";
            case PASTA_VAZIA -> "Essa pasta não tem nenhum arquivo. Escolha uma pasta que tenha pelo menos um arquivo dentro.";
            case ERRO_DE_GRAVACAO -> "Não foi possível salvar o arquivo. Verifique se há espaço na pasta e tente de novo. Seu arquivo original não foi alterado.";
            case OPERACAO_CANCELADA -> "Tudo bem, a operação foi cancelada. Nenhum arquivo foi criado.";
            case FORMATO_INVALIDO -> "Esse arquivo não parece ter sido protegido por este programa.";
        };
    }

    @Override
    public String legendaTamanhoMaximo() {
        return "Tamanho máximo do arquivo: " + LimitesDeArquivo.tamanhoMaximoTexto();
    }

    @Override
    public String forcaDaSenha(ForcaDeSenha f) {
        return switch (f) {
            case VAZIA -> "Digite uma senha para ver o quanto ela é segura.";
            case FRACA -> "Senha fraca. Use mais caracteres, misturando letras, números e símbolos.";
            case MEDIA -> "Senha média. Para ficar forte, aumente o tamanho ou misture mais tipos de caracteres.";
            case FORTE -> "Senha forte.";
        };
    }

    @Override public String dicaArrastar() { return "Você também pode arrastar um arquivo ou uma pasta para esta janela."; }
    @Override public String arrasteVariosItens() { return "Arraste um arquivo ou uma pasta de cada vez."; }
    @Override public String arrasteParaAbrirInvalido() { return "Para abrir, escolha um arquivo que termine em .protegido."; }

    @Override
    public ConteudoDeAjuda ajuda() {
        String limite = LimitesDeArquivo.tamanhoMaximoTexto();
        return new ConteudoDeAjuda("Ajuda",
                "Veja como proteger e abrir seus arquivos, passo a passo",
                java.util.List.of(
                SecaoDeAjuda.de("Proteger",
                        Passos.de(
                                "Na tela inicial, clique em “Proteger um arquivo”.",
                                "Clique em “Adicionar arquivo ou pasta” e escolha o que quer proteger. Você também pode arrastar para a janela. Tamanho máximo: " + limite + " (somando os arquivos de uma pasta).",
                                "Veja o nome do arquivo protegido: ele fica na mesma pasta e termina em .protegido. O original é mantido; só marque “Apagar o original” se tiver certeza.",
                                "Clique em “Continuar”.",
                                "Crie uma senha com pelo menos 8 caracteres e digite de novo. A barra mostra se ela está fraca, média ou forte.",
                                "Clique em “Proteger arquivo” e aguarde. Se mudar de ideia, clique em “Cancelar”: nenhum arquivo incompleto é criado.",
                                "Na tela “Pronto”, veja o nome do arquivo criado, onde ele está salvo e o que aconteceu com o original. “Abrir a pasta” mostra o arquivo no seu computador."),
                        new Dica("Ao proteger uma pasta, todos os arquivos dentro dela viram um único arquivo protegido. Ao abrir, a pasta volta como era, e a pasta original nunca é apagada.")),
                SecaoDeAjuda.de("Abrir",
                        Passos.de(
                                "Na tela inicial, clique em “Abrir um arquivo protegido”.",
                                "Clique em “Escolher arquivo” e selecione o arquivo que termina em .protegido (ou arraste-o para a janela).",
                                "Clique em “Continuar”.",
                                "Digite a senha que você criou ao proteger o arquivo e clique em “Abrir arquivo”.",
                                "Na tela “Pronto”, veja o nome do arquivo aberto e a pasta onde ele foi salvo. O arquivo protegido continua na pasta."),
                        new Dica("Se o programa disser que a senha não confere, tente de novo: você pode digitar quantas vezes precisar.")),
                SecaoDeAjuda.de("Dicas",
                        new Aviso("Se você esquecer a senha, não é possível abrir o arquivo. Anote a senha em um lugar seguro."),
                        new Dica("Se aparecer que a senha não confere, veja se a tecla Caps Lock está ativada. Essa mensagem também aparece se o arquivo protegido foi alterado depois de criado."),
                        new Dica("Você pode voltar à tela inicial a qualquer momento com o botão “Voltar ao início”."),
                        new Dica("Para levar o arquivo para outro computador, copie o arquivo .protegido inteiro; lá, o programa abre com a mesma senha."))));
    }
}

package br.edu.ifrs.sfm.ui;

import br.edu.ifrs.sfm.core.resultado.CodigoResultado;
import br.edu.ifrs.sfm.ui.ajuda.ConteudoDeAjuda;
import br.edu.ifrs.sfm.core.validacao.ForcaDeSenha;

/** Cada interface traduz os códigos do núcleo no seu próprio vocabulário. */
public interface CatalogoDeMensagens {
    String mensagem(CodigoResultado codigo);
    /** Legenda sob o botão de escolher arquivo. */
    String legendaTamanhoMaximo();
    /** Texto do indicador de força da senha. */
    String forcaDaSenha(ForcaDeSenha forca);
    /** Dica sobre arrastar e soltar. */
    String dicaArrastar();
    String arrasteVariosItens();
    String arrasteParaAbrirInvalido();
    /** Conteúdo da janela de ajuda, no vocabulário da interface. */
    ConteudoDeAjuda ajuda();
}

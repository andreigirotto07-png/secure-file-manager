package br.edu.ifrs.sfm.controle;

import br.edu.ifrs.sfm.core.cripto.AlgoritmoCifra;
import br.edu.ifrs.sfm.core.cripto.DerivacaoDeChave;
import br.edu.ifrs.sfm.core.cripto.ProtecaoService;
import br.edu.ifrs.sfm.core.resultado.CodigoResultado;
import br.edu.ifrs.sfm.core.resultado.Resultado;
import br.edu.ifrs.sfm.teste.EventLogger;
import br.edu.ifrs.sfm.teste.TipoEvento;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Ponte entre as telas e o núcleo: roda em segundo plano, registra eventos, não decide regra de negócio. */
public final class OperacaoControlador {
    private final ProtecaoService servico;
    private final EventLogger logger;
    private final GerenciadorDeModo modo;
    private final Executor entrega; // thread da interface (Platform::runLater)
    private final ExecutorService trabalho = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "operacao-cripto");
        t.setDaemon(true);
        return t;
    });

    public OperacaoControlador(ProtecaoService servico, EventLogger logger, GerenciadorDeModo modo, Executor entrega) {
        this.servico = servico;
        this.logger = logger;
        this.modo = modo;
        this.entrega = entrega;
    }

    public void proteger(Path entrada, Path saida, char[] senha, char[] confirmacao, boolean apagarOriginal,
                         Consumer<Double> progresso, Consumer<Resultado> aoTerminar) {
        proteger(entrada, saida, senha, confirmacao, apagarOriginal, AlgoritmoCifra.PADRAO, progresso, aoTerminar);
    }

    public void proteger(Path entrada, Path saida, char[] senha, char[] confirmacao, boolean apagarOriginal,
                         AlgoritmoCifra algoritmo, Consumer<Double> progresso, Consumer<Resultado> aoTerminar) {
        proteger(entrada, saida, senha, confirmacao, apagarOriginal, algoritmo, DerivacaoDeChave.PADRAO, progresso, aoTerminar);
    }

    public void proteger(Path entrada, Path saida, char[] senha, char[] confirmacao, boolean apagarOriginal,
                         AlgoritmoCifra algoritmo, DerivacaoDeChave derivacao, Consumer<Double> progresso, Consumer<Resultado> aoTerminar) {
        executar(() -> servico.proteger(entrada, saida, senha, confirmacao, apagarOriginal, algoritmo, derivacao, f -> entregar(progresso, f)),
                senha, confirmacao, aoTerminar);
    }

    public void abrir(Path entrada, Path saida, char[] senha, Consumer<Double> progresso, Consumer<Resultado> aoTerminar) {
        executar(() -> servico.abrir(entrada, saida, senha, f -> entregar(progresso, f)), senha, null, aoTerminar);
    }

    public void cancelar() { servico.cancelar(); }

    /**
    * Fecha o programa sem deixar arquivo parcial: pede o cancelamento ao núcleo e espera a operação
    * limpar o .tmp antes de a JVM sair (a thread de trabalho é daemon e seria morta no meio da limpeza).
    */
   public void encerrar() {
       servico.cancelar();
       trabalho.shutdown();
       try {
           if (!trabalho.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) trabalho.shutdownNow();
       } catch (InterruptedException e) {
           trabalho.shutdownNow();
           Thread.currentThread().interrupt();
       }
   }

    /** Erro detectado na própria tela (ex.: senha curta) antes de chamar o núcleo. */
    public void registrarErro(CodigoResultado codigo) { logger.registrar(TipoEvento.ERRO_EXIBIDO, modo.modoAtual(), codigo); }

    /** Uso da ajuda é um dado de interesse para a pesquisa. */
    public void ajudaAberta() { logger.registrar(TipoEvento.AJUDA_ABERTA, modo.modoAtual()); }

    public void telaAberta() { logger.registrar(TipoEvento.TELA_ABERTA, modo.modoAtual()); }

    private void executar(java.util.function.Supplier<Resultado> acao, char[] senha, char[] confirmacao, Consumer<Resultado> aoTerminar) {
        logger.registrar(TipoEvento.TAREFA_INICIADA, modo.modoAtual());
        trabalho.submit(() -> {
            Resultado r;
            try {
                r = acao.get();
            } catch (RuntimeException e) {
                r = Resultado.falha(CodigoResultado.ERRO_DE_GRAVACAO);
            } finally {
                if (senha != null) Arrays.fill(senha, '\0');
                if (confirmacao != null) Arrays.fill(confirmacao, '\0');
            }
            Resultado fim = r;
            logger.registrar(fim.sucesso() ? TipoEvento.TAREFA_CONCLUIDA : TipoEvento.ERRO_EXIBIDO, modo.modoAtual(), fim.codigo());
            entrega.execute(() -> aoTerminar.accept(fim));
        });
    }

    private void entregar(Consumer<Double> c, double f) { if (c != null) entrega.execute(() -> c.accept(f)); }
}

# Secure File Manager

Software do TCC *"Desenvolvimento e avaliação comparativa de interfaces técnica e orientada a tarefas para criptografia de arquivos"* (IFRS Veranópolis).

Duas interfaces JavaFX sobre **um único núcleo** de criptografia (AES-256-GCM (padrão); a interface técnica também oferece AES-128-GCM e ChaCha20-Poly1305, e as derivações PBKDF2-HMAC-SHA256 (padrão), PBKDF2-HMAC-SHA512, scrypt e Argon2id (as duas últimas usam Bouncy Castle); arquivo `.protegido`).

## Requisitos
- JDK 17 ou superior (testado com 21)
- **VS Code** com o *Extension Pack for Java* (jeito principal de rodar; importa o projeto e baixa JavaFX 21, Bouncy Castle e JUnit 5 sozinha, sem precisar do Maven no `PATH`)
- Opcional: Maven 3.9+ instalado e no `PATH`, só para rodar pelo terminal com `mvn`

## Executar

### Pelo VS Code (recomendado)
1. Instale o JDK 17+ e, no VS Code, a extensão *Extension Pack for Java*.
2. Abra a **pasta do projeto** (a que contém o `pom.xml`). Aguarde a extensão terminar de importar o projeto.
3. Abra o painel **Run and Debug** (Ctrl+Shift+D), escolha uma configuração no menu e clique no triângulo verde:

| Configuração | O que faz |
|---|---|
| `Main` | Abre o programa na interface técnica (padrão), com o botão de troca de modo |
| `Estudo (participante)` | Modo participante: pergunta o código, a interface e a tarefa (veja a seção abaixo) |

Não é preciso instalar o Maven para isso.

### Pelo terminal (alternativa)
Exige o Maven instalado e no `PATH` (confira com `mvn -v`).
```bash
mvn javafx:run                                   # abre na interface técnica (padrão)
mvn javafx:run -Djavafx.args="--modo=tarefas"    # abre na interface orientada a tarefas
```
Se o terminal responder que o `mvn` não é reconhecido, use o VS Code.

### Argumentos (para o pesquisador)
| Argumento | Efeito |
|---|---|
| `--modo=tecnico` / `--modo=tarefas` | Interface inicial (padrão: técnica) |
| `--teste` | Modo participante: bloqueia e esconde o botão de troca de modo |
| `--tarefa=T1` | Rótulo da tarefa gravado no registro de eventos |
| `--log=eventos.csv` | Exporta o registro de eventos ao fechar o programa. Com `--teste` e sem `--log`, grava em `Documentos\SecureFileManager-testes\registro_<modo>_<data>.csv` |

## Modo participante (sessões do estudo)
Usado nas sessões com participantes. O programa abre travado em uma das interfaces, sem o botão de troca de modo, e grava um registro de eventos para a análise. Cada tarefa (T1 a T6) é executada com o programa aberto e fechado em seguida.

### Pelo VS Code (recomendado)
1. Abra a pasta do projeto no VS Code (a que contém o `pom.xml`).
2. No painel **Run and Debug** (Ctrl+Shift+D), escolha a configuração **Estudo (participante)** e clique no triângulo verde.
3. Responda às três perguntas que aparecem no topo da tela:
   - **Código do participante** (ex.: `P01`);
   - **Interface** (`tecnico` ou `tarefas`);
   - **Tarefa** (`T1` a `T6`).
4. O programa abre no modo escolhido. Quando o participante terminar a tarefa, **feche a janela**: o registro só é gravado nesse momento.
5. Repita para a próxima tarefa e, ao fim das seis, para a outra interface.

O registro é salvo em `logs/<participante>_<interface>_<tarefa>.csv` (ex.: `logs/P01_tecnico_T1.csv`). A pasta `logs/` não vai para o Git.

A configuração fica em `.vscode/launch.json`:
```jsonc
{
    "type": "java",
    "name": "Estudo (participante)",
    "request": "launch",
    "mainClass": "br.edu.ifrs.sfm.Main",
    "projectName": "secure-file-manager",
    "cwd": "${workspaceFolder}",
    "args": [
        "--teste",
        "--modo=${input:modo}",
        "--tarefa=${input:tarefa}",
        "--log=logs/${input:participante}_${input:modo}_${input:tarefa}.csv"
    ]
}
```
Os campos `${input:...}` são definidos no bloco `inputs` do mesmo arquivo (`participante`, `modo` e `tarefa`).

### Pelo terminal (exige o Maven no `PATH`)
```bash
mvn javafx:run -Djavafx.args="--teste --modo=tecnico --tarefa=T1 --log=logs/P01_tecnico_T1.csv"
```

### Registro de eventos (CSV)
Colunas: `instante`, `tipo`, `modo`, `tarefa`, `codigo`. Tipos de evento: `TELA_ABERTA`, `TAREFA_INICIADA`, `TAREFA_CONCLUIDA`, `ERRO_EXIBIDO`, `TROCA_DE_MODO` e `AJUDA_ABERTA`.

| Dado | Como extrair |
|---|---|
| Tempo da tarefa | Diferença entre `TELA_ABERTA` e `TAREFA_CONCLUIDA` |
| Erros | Contagem de linhas `ERRO_EXIBIDO` |
| Tentativas | Contagem de linhas `TAREFA_INICIADA` e conferência do `codigo` |
| Uso da ajuda | Presença de `AJUDA_ABERTA` |

**Não grava senhas nem nomes de arquivos.** O arquivo só é gravado quando a janela é fechada normalmente: se o programa for encerrado à força, o registro daquela tarefa se perde. Além disso, o rótulo da tarefa é fixado na abertura, por isso o programa é aberto e fechado a cada tarefa.

## Testar
```bash
mvn test
```

## Gerar o programa para os testes com usuários (Windows) — fazer só no final, com o software fechado
Requisitos: JDK 17+ **com jpackage** (ex.: Temurin) e Maven no PATH.

```bat
empacotar\criar-instalador.bat tecnico          :: pasta pronta, abre direto na interface técnica
empacotar\criar-instalador.bat tarefas          :: pasta pronta, abre direto na interface por tarefas
empacotar\criar-instalador.bat tecnico exe      :: instalador .exe (exige WiX Toolset 3.x)
empacotar\criar-instalador.bat                  :: versão "livre", com o botão de troca de modo
```
- Sem o terceiro argumento o resultado é uma pasta (`target\instalador\SecureFileManager-...`) que roda sem instalação: copie para o computador do participante (ou para um pendrive) e abra o `.exe`.
- As versões `tecnico` e `tarefas` já saem em modo de teste: sem botão de troca e com registro automático de eventos em `Documentos\SecureFileManager-testes`.
- O Java já vai embutido; o participante não precisa instalar nada.
- Um computador onde o pacote ainda não foi testado deve ser conferido antes da sessão: abra o programa, proteja e abra um arquivo.

## Estrutura de pacotes (`br.edu.ifrs.sfm`)
```
App, Main                 ponto de entrada (Main não herda de Application, para rodar em qualquer IDE)

core                      núcleo, sem nenhuma dependência de interface
  cripto/                 ProtecaoService, ProtecaoAesGcmService, AlgoritmoCifra,
                          DerivacaoDeChave, DerivadorDeChave
  arquivo/                FormatoArquivoProtegido (cabeçalho), PacoteDePasta (ZIP seguro), InfoDeEntrada
  validacao/              ValidadorDeSenha, ForcaDeSenha, LimitesDeArquivo
  resultado/              Resultado, CodigoResultado, ResumoOperacao

controle/                 OperacaoControlador, GerenciadorDeModo, ModoInterface
teste/                    EventLogger, EventoTeste, TipoEvento (registro anônimo para a pesquisa)

ui                        InterfaceUsuario, CatalogoDeMensagens, JanelaPrincipal
  tecnica/                InterfaceTecnica, CatalogoTecnico
  tarefas/                InterfaceTarefas, CatalogoTarefas
  componentes/            IndicadorDeForca, SeletorDeItem, SeletorSwing
  ajuda/                  ConteudoDeAjuda, SecaoDeAjuda, BlocoDeAjuda (conteúdo) e JanelaAjuda (visual)
```
Dependências: `ui` → `controle` → `core`; o `core` não conhece nada de fora. Os testes seguem a mesma divisão de pacotes.

## Regras de negócio implementadas
- Senha com no mínimo 8 caracteres e confirmação ao proteger.
- Original **preservado por padrão**; só é apagado com a opção explícita.
- Cancelar não deixa arquivo parcial (grava em `.tmp` e só então renomeia); falha nunca altera o original. Fechar a janela no meio de uma operação também cancela e apaga o temporário antes de sair (espera até 5 segundos).
- Nunca sobrescreve arquivo existente; nomes sugeridos não colidem (`arquivo (1).pdf`).
- Tamanho máximo: **100 MB** (`LimitesDeArquivo.TAMANHO_MAXIMO_BYTES`), pois o processamento é em memória. A legenda sob o botão de escolher arquivo, nas duas interfaces, lê esse valor.
- Senha errada e arquivo adulterado geram o mesmo erro no GCM; as mensagens das duas interfaces dizem isso com honestidade.
- **Pastas:** é possível proteger uma pasta inteira (o mesmo botão de adicionar aceita arquivo ou pasta; o programa identifica sozinho). Todos os arquivos dentro dela, inclusive subpastas, viram um único `.protegido`; ao abrir, a pasta é recriada. O limite de 100 MB vale para o total. Links e pastas vazias não entram, e a pasta original nunca é apagada. O ZIP é extraído com proteção contra caminhos fora do destino (zip-slip) e contra conteúdo maior que o limite.
- **Arrastar e soltar:** nas duas interfaces é possível arrastar um arquivo ou pasta para a janela. Um `.protegido` arrastado abre; qualquer outro item é protegido.
- Botão **Ajuda** nas duas interfaces: janela com abas por assunto, passos numerados, caixas de dica e de atenção e (na técnica) cartões de algoritmos e derivações; cada interface com o seu vocabulário e tema visual. Cada abertura da ajuda é registrada como `AJUDA_ABERTA` no CSV de eventos.
- Fora desta versão: histórico, tema, escolha de idioma.

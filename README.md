# Estudo dirigido: CQRS na prática

**PUC Goiás** · Escola Politécnica e de Artes · Tecnologia em Análise e Desenvolvimento de Sistemas
**ADS1253 — Programação Orientada a Objetos com Banco de Dados**

Implementação do estudo dirigido *"Da consulta pelo agregado ao modelo de leitura mantido por eventos"*: a aplicação do padrão **CQRS** (*Command Query Responsibility Segregation*) a um pequeno sistema de turmas, em nove etapas, mais as atividades complementares.

O ponto de partida é um projeto em que um único serviço altera e consulta dados por meio do agregado. A cada etapa uma alteração é feita e o seu efeito é **medido**: o projeto informa quantos comandos SQL foram enviados ao banco e quais tabelas foram lidas. Ao final, o painel de ocupação das turmas, que exigia **101 comandos SQL**, passa a ser obtido com **1 comando** sobre uma tabela preparada para essa finalidade.

## Sumário

- [Requisitos e execução](#requisitos-e-execução)
- [Domínio](#domínio)
- [Arquitetura final](#arquitetura-final)
- [Como a medição funciona](#como-a-medição-funciona)
- [Etapas do estudo](#etapas-do-estudo)
- [Quadro de medições](#quadro-de-medições)
- [Atividades complementares](#atividades-complementares)
- [Observações sobre a implementação](#observações-sobre-a-implementação)

## Requisitos e execução

| Item | Versão |
|---|---|
| JDK | 21 |
| Maven | 3.9 ou superior |
| Spring Boot | 3.4.1 (Spring Data JPA) |
| Banco de dados | H2 em memória, criado a cada execução |

Não há instalação adicional. Para executar o roteiro:

```bash
mvn spring-boot:run
```

O roteiro (`Roteiro.java`) é executado na inicialização, faz a carga dos dados e imprime as medições de cada etapa no console.

## Domínio

O sistema administra **turmas** de cursos. Uma turma possui nome, capacidade e um conjunto de e-mails de estudantes inscritos. O agregado `Turma` aplica as regras de negócio:

- a turma não aceita inscrições além da capacidade;
- o mesmo e-mail não pode ser inscrito duas vezes;
- só é possível cancelar a inscrição de um e-mail que esteja inscrito.

A cada operação aceita, o agregado registra um **evento de domínio** (`TurmaAberta`, `InscricaoConfirmada`, `InscricaoCancelada`).

**Carga de dados:** 100 turmas (`Turma 001` a `Turma 100`), todas com capacidade para 30 estudantes. A quantidade de inscritos varia de 0 a 20 (a Turma 001 recebe 1, a Turma 002 recebe 2 e assim por diante, reiniciando a cada 21 turmas), totalizando 976 inscrições.

## Arquitetura final

```
src/main/java/br/pucgoias/ads1253/turmas/
├── TurmasApplication.java
├── Roteiro.java                      # executa a carga e as medições
├── apoio/
│   ├── CargaInicial.java             # cria as 100 turmas e as inscrições
│   └── MedidorSql.java               # conta os comandos SQL enviados ao banco
├── dominio/                          # regras de negócio e eventos
│   ├── Turma.java                    # raiz do agregado
│   ├── TurmaAberta.java
│   ├── InscricaoConfirmada.java
│   ├── InscricaoCancelada.java
│   └── RegraDeNegocioException.java
├── escrita/                          # lado dos COMANDOS
│   ├── AbrirTurma.java
│   ├── InscreverEstudante.java
│   ├── CancelarInscricao.java
│   ├── TurmaComandos.java
│   └── TurmaRepository.java
└── leitura/                          # lado das CONSULTAS
    ├── TurmaConsultas.java           # serviço de consultas (somente leitura)
    ├── TurmaLeituraRepository.java   # consultas que devolvem projeções
    ├── ResumoTurma.java              # projeções
    ├── OcupacaoTurma.java
    ├── DetalheTurma.java
    ├── LinhaDetalhe.java
    ├── PainelTurma.java              # tabela de leitura (painel_turma)
    ├── PainelTurmaRepository.java
    ├── PainelTurmaProjetor.java      # mantém a tabela de leitura por eventos
    └── ReconstrutorPainel.java       # recalcula a tabela de leitura
```

| Pacote | Responsabilidade |
|---|---|
| `dominio` | Regras de negócio e eventos |
| `escrita` | Alterar dados (comandos) |
| `leitura` | Consultar dados (projeções) e manter o modelo de leitura |

O lado da escrita **não conhece** o lado da leitura: a ligação entre eles é o evento de domínio.

### Fluxo de uma inscrição

```
TurmaComandos.inscrever
   └─> Turma.inscrever ........ valida as regras e registra InscricaoConfirmada
   └─> repository.save ........ grava a turma e publica o evento
          └─> PainelTurmaProjetor.aoInscrever ... incrementa painel_turma
```

O `@EventListener` é **síncrono**: executa dentro da transação do comando. A consulta feita logo após o comando já enxerga o valor atualizado (consistência imediata).

## Como a medição funciona

O método `medir` do `Roteiro` zera o contador de comandos SQL, executa uma ação e apresenta:

| Linha | Significado |
|---|---|
| Comandos SQL executados | Quantidade de comandos que o Hibernate enviou ao banco |
| Tabelas lidas | Tabelas que aparecem nas cláusulas `from` e `join` |
| Primeiro comando | Texto do primeiro comando SQL enviado |

A contagem é feita por um `StatementInspector` do Hibernate (`MedidorSql`), registrado em `application.properties`. Sem essa propriedade a medição mostraria zero comandos.

## Etapas do estudo

| Etapa | O que foi feito | Conceito |
|---|---|---|
| 1 | Execução do projeto inicial e medição do painel pelo agregado (101 comandos) | Ponto de partida |
| 2 | Classificação das operações do serviço em comandos e consultas | Comando e consulta |
| 3 | `TurmaService` dividido em `TurmaComandos` (pacote `escrita`) e `TurmaConsultas` (pacote `leitura`); comandos como *records* | Segregação de responsabilidade |
| 4 | Primeira projeção, `ResumoTurma`, com `select new ...` | Projeção (CQRS nível 1) |
| 5 | Painel por projeção com `left join` e `count` (`OcupacaoTurma`): de 101 para 1 comando | Projeção (CQRS nível 1) |
| 6 | Tabela de leitura `painel_turma` (entidade `PainelTurma`) e sua consulta | Modelo de leitura |
| 7 | `PainelTurmaProjetor` mantém a tabela de leitura a partir dos eventos | Modelo de leitura mantido por eventos (nível 2) |
| 8 | Falha provocada no projetor: o comando inteiro sofre *rollback* | Consistência imediata |
| 9 | `ReconstrutorPainel` apaga e recalcula a tabela de leitura | Modelo de leitura como dado derivado |

### Etapa 8: consistência imediata

Foi acrescentada, provisoriamente, uma falha no projetor para e-mails iniciados por `falha`. O resultado observado:

```
== Comando com falha no projetor
   Exceção: Falha simulada no projetor
   Escrita: OcupacaoTurma[... nome=Turma 001, capacidade=30, inscritos=2]
   Leitura: OcupacaoTurma[... nome=Turma 001, capacidade=30, inscritos=2]
```

A inscrição `falha@...` não foi gravada em nenhuma das tabelas: como o projetor executa na transação do comando, a exceção desfaz tudo. O trecho foi removido ao final da etapa, conforme o roteiro do estudo.

> A contrapartida é que uma falha na projeção impede o comando. Quando isso não é aceitável, ou quando o modelo de leitura fica em outro banco, adota-se a projeção assíncrona, com **consistência eventual**.

## Quadro de medições

Valores obtidos na execução (100 turmas):

| Etapa | Forma de obter o painel | Comandos SQL | Tabelas lidas |
|---|---|---|---|
| 1 | Serviço único, pelo agregado | **101** | `turma`, `turma_inscrito` |
| 3 | Serviço de consultas, pelo agregado | **101** | `turma`, `turma_inscrito` |
| 4 | Resumo das turmas (projeção simples) | 1 | `turma` |
| 5 | Projeção com junção e contagem | **1** | `turma`, `turma_inscrito` |
| 7 | Tabela de leitura | **1** | `painel_turma` |

O ganho está na ordem de grandeza: de **um comando por turma** para **um único comando**. Os 101 comandos vêm de 1 para listar as turmas mais 1 por turma para carregar a coleção de e-mails, necessária apenas para contar (`getInscritos().size()`).

Custos de outras operações (100 turmas): a inscrição de um estudante executa 6 comandos (carregar a turma, gravar a inscrição e atualizar `painel_turma`), e a reconstrução da tabela de leitura executa 202.

## Atividades complementares

### 1. Turmas com vagas

`TurmaConsultas.comVagas()` devolve, da tabela de leitura, só as turmas com `inscritos < capacidade` (1 comando, tabela `painel_turma`). Como a carga deixa todas as turmas com vaga, o roteiro lota a Turma 001 (29 inscrições) para mostrar a diferença:

| Momento | Linhas |
|---|---|
| Antes de lotar a Turma 001 | 100 |
| Depois de lotar a Turma 001 | 99 |

### 2. Novo comando e novo evento: `CancelarInscricao`

- `Turma.cancelarInscricao(email)` no agregado, com o evento `InscricaoCancelada`;
- comando `CancelarInscricao` e método `TurmaComandos.cancelar`;
- ouvinte `PainelTurmaProjetor.aoCancelar`, que decrementa o total.

Após o cancelamento, a Turma 001 passa de 2 para 1 inscrito no painel. Cancelar um e-mail que não está inscrito é recusado com `RegraDeNegocioException`.

### 3. Detalhe de uma turma

A projeção `DetalheTurma` (nome, capacidade e lista de e-mails) é obtida com **um único comando**, por `TurmaConsultas.detalhe(turmaId)`, sobre as tabelas de escrita (`turma` e `turma_inscrito`).

**Uma tabela de leitura seria necessária? Não.** A consulta é por chave primária, não exige agregação e o resultado é limitado pela capacidade da turma. Uma tabela de leitura só compensaria se a consulta fosse cara ou muito frequente, e duplicaria os e-mails já existentes nas tabelas de escrita, com custo de manutenção e risco de divergência.

### 4. Carga maior: 1000 turmas

Alterando `CargaInicial.TURMAS` para 1000 e comparando o projeto da Etapa 3 com o projeto final:

| Forma de obter o painel | Comandos SQL | Tempo |
|---|---|---|
| Etapa 3: pelo agregado | 1001 | 93 ms |
| Projeção com junção e contagem | 1 | 26 ms |
| Tabela de leitura | 1 | 13 ms |

| Carga de dados | Tempo |
|---|---|
| Sem projetor (Etapa 3) | 2,7 s |
| Com projetor (final) | 3,6 s |

O número de comandos cresce com o número de turmas no modelo de escrita (1001) e permanece constante nos modelos de leitura (1). O custo do modelo de leitura é pago na **escrita**: cada comando passa a gravar também em `painel_turma`.

> Os tempos devem ser lidos como tendência: o banco é H2 em memória, sem latência de rede, e cada valor vem de uma única execução, com aquecimento da JVM. Em um banco remoto a diferença seria maior. A quantidade de comandos é a medida confiável.

## Observações sobre a implementação

- **Exceção na Etapa 8.** O enunciado captura `IllegalStateException` no `Roteiro`, mas o Spring a converte em `InvalidDataAccessApiUsageException` ao atravessar o repositório. Para observar o *rollback*, o teste usa `catch (RuntimeException e)`. O código de falha simulada não faz parte da versão final do projeto.
- **`PainelTurma` não é um agregado.** É uma entidade JPA sem regras de negócio; nenhum comando deve alterá-la diretamente. Somente o projetor e o reconstrutor gravam nela.
- **Repositórios com `Repository<T, ID>`.** Cada repositório expõe apenas os métodos necessários. O de escrita tem só `save` e `findById`; o de leitura devolve projeções, nunca entidades.
- **Reconstrução.** A tabela de leitura não guarda informação própria, por isso pode ser apagada e recalculada a partir das tabelas de escrita (`ReconstrutorPainel`), reaproveitando a consulta da Etapa 5. Serve para corrigir um projetor defeituoso ou criar uma nova tabela de leitura para dados que já existiam.

## Questões de revisão (resumo)

1. **Separar classes ≠ separar modelos.** Na Etapa 3 a medição não mudou: a consulta ainda usava o modelo de escrita. O ganho vem de dar à leitura um modelo próprio.
2. **`resumos()` não carrega inscritos** porque `select new ...` preenche o *record* direto das colunas; nenhum `Turma` é criado e a coleção não é tocada.
3. **Projeção com junção × tabela de leitura.** A segunda é preferível quando há muitas inscrições e o painel é consultado com frequência, pois evita recontar a cada consulta.
4. **Painel vazio na Etapa 6.** Faltava o **projetor**, que escuta os eventos e grava na tabela de leitura.
5. **`TurmaComandos` não foi alterado.** O projetor toma conhecimento da inscrição pelo evento `InscricaoConfirmada`, publicado no `save`.
6. **Etapa 8.** O projetor roda na transação do comando; a exceção provoca *rollback* de tudo, inclusive da inscrição aceita pelo agregado.
7. **Comando consultando `painel_turma`?** Não: o modelo de leitura pode estar desatualizado (principalmente se a projeção for assíncrona) e não é fonte de verdade. A regra de vagas deve ser decidida pelo agregado, que conhece todos os inscritos.

---

Material didático da disciplina ADS1253 — PUC Goiás.

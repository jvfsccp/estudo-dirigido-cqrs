package br.pucgoias.ads1253.turmas;

import br.pucgoias.ads1253.turmas.apoio.CargaInicial;
import br.pucgoias.ads1253.turmas.apoio.MedidorSql;
import br.pucgoias.ads1253.turmas.dominio.RegraDeNegocioException;
import br.pucgoias.ads1253.turmas.escrita.CancelarInscricao;
import br.pucgoias.ads1253.turmas.escrita.InscreverEstudante;
import br.pucgoias.ads1253.turmas.escrita.TurmaComandos;
import br.pucgoias.ads1253.turmas.leitura.ReconstrutorPainel;
import br.pucgoias.ads1253.turmas.leitura.TurmaConsultas;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Roteiro executado na inicialização da aplicação.
 * É alterado ao longo do estudo dirigido, conforme as instruções de cada etapa.
 */
@Component
public class Roteiro implements CommandLineRunner {

    private final CargaInicial carga;
    private final TurmaComandos comandos;
    private final TurmaConsultas consultas;
    private final ReconstrutorPainel reconstrutor;

    public Roteiro(CargaInicial carga, TurmaComandos comandos, TurmaConsultas consultas,
                   ReconstrutorPainel reconstrutor) {
        this.carga = carga;
        this.comandos = comandos;
        this.consultas = consultas;
        this.reconstrutor = reconstrutor;
    }

    @Override
    public void run(String... args) {
        UUID primeiraTurma = carga.executar();
        System.out.println();
        System.out.println("Carga concluída: " + CargaInicial.TURMAS + " turmas.");

        // Etapa 4
        medir("Resumo das turmas (projeção)", () -> imprimir(consultas.resumos()));

        // Etapa 5
        medir("Painel calculado a cada consulta (projeção com junção)",
                () -> imprimir(consultas.painelCalculado()));

        // Etapa 6
        medir("Painel lido da tabela de leitura", () -> imprimir(consultas.painel()));

        // Etapa 7
        medir("Comando: nova inscrição na Turma 001",
                () -> comandos.inscrever(new InscreverEstudante(primeiraTurma, "novo@pucgoias.edu.br")));
        medir("Painel após o comando", () -> imprimir(consultas.painel()));

        // Etapa 9
        medir("Reconstrução da tabela de leitura",
                () -> System.out.println("   Linhas recriadas: " + reconstrutor.reconstruir()));
        medir("Painel após a reconstrução", () -> imprimir(consultas.painel()));

        // Atividade complementar 3: detalhe de uma turma
        medir("Detalhe da Turma 001 (projeção)",
                () -> System.out.println("   " + consultas.detalhe(primeiraTurma).orElseThrow()));

        // Atividade complementar 2: cancelamento de inscrição
        medir("Comando: cancelar inscrição de novo@ na Turma 001",
                () -> comandos.cancelar(new CancelarInscricao(primeiraTurma, "novo@pucgoias.edu.br")));
        medir("Painel após o cancelamento", () -> imprimir(consultas.painel()));
        System.out.println("   Turma 001 no painel: " + consultas.painel().get(0));
        System.out.println("   Detalhe: " + consultas.detalhe(primeiraTurma).orElseThrow());
        try {
            comandos.cancelar(new CancelarInscricao(primeiraTurma, "nunca-inscrito@pucgoias.edu.br"));
        } catch (RegraDeNegocioException e) {
            System.out.println("   Cancelamento recusado: " + e.getMessage());
        }

        // Atividade complementar 1: turmas com vagas
        medir("Turmas com vagas (antes de lotar a Turma 001)", () -> imprimir(consultas.comVagas()));
        medir("Comando: lotar a Turma 001 (29 inscrições)", () -> {
            for (int j = 2; j <= 30; j++) {
                comandos.inscrever(new InscreverEstudante(primeiraTurma, "estudante" + j + "@pucgoias.edu.br"));
            }
        });
        medir("Turmas com vagas (depois de lotar a Turma 001)", () -> imprimir(consultas.comVagas()));
        System.out.println("   Turma 001 no painel: " + consultas.painel().get(0));
    }

    /** Executa a ação informada e apresenta quantos comandos SQL foram enviados ao banco. */
    private void medir(String titulo, Runnable acao) {
        System.out.println();
        System.out.println("== " + titulo);
        MedidorSql.reiniciar();
        acao.run();
        System.out.println("   Comandos SQL executados: " + MedidorSql.total());
        System.out.println("   Tabelas lidas: " + MedidorSql.tabelasLidas());
        System.out.println("   Primeiro comando: " + MedidorSql.primeiroComando());
    }

    /** Apresenta as três primeiras linhas e a quantidade total de linhas. */
    private void imprimir(List<?> linhas) {
        linhas.stream().limit(3).forEach(linha -> System.out.println("   " + linha));
        System.out.println("   ... total de linhas: " + linhas.size());
    }
}

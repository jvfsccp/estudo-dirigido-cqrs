package br.pucgoias.ads1253.turmas.leitura;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Lado da leitura: não altera dados. */
@Service
@Transactional(readOnly = true)
public class TurmaConsultas {

    private final TurmaLeituraRepository leitura;
    private final PainelTurmaRepository paineis;

    public TurmaConsultas(TurmaLeituraRepository leitura, PainelTurmaRepository paineis) {
        this.leitura = leitura;
        this.paineis = paineis;
    }

    public List<ResumoTurma> resumos() {
        return leitura.resumos();
    }

    public List<OcupacaoTurma> painelCalculado() {
        return leitura.ocupacaoCalculada();
    }

    public List<OcupacaoTurma> painel() {
        return paineis.findAllByOrderByNomeAsc().stream()
                .map(this::paraOcupacao)
                .toList();
    }

    /** Turmas com vagas, lidas da tabela de leitura. */
    public List<OcupacaoTurma> comVagas() {
        return paineis.comVagas().stream()
                .map(this::paraOcupacao)
                .toList();
    }

    /** Detalhe de uma turma, obtido das tabelas de escrita com um único comando SQL. */
    public Optional<DetalheTurma> detalhe(UUID turmaId) {
        List<LinhaDetalhe> linhas = leitura.linhasDeDetalhe(turmaId);
        if (linhas.isEmpty()) {
            return Optional.empty();
        }
        LinhaDetalhe primeira = linhas.get(0);
        List<String> emails = linhas.stream()
                .map(LinhaDetalhe::email)
                .filter(Objects::nonNull)
                .toList();
        return Optional.of(new DetalheTurma(primeira.id(), primeira.nome(), primeira.capacidade(), emails));
    }

    private OcupacaoTurma paraOcupacao(PainelTurma p) {
        return new OcupacaoTurma(p.getTurmaId(), p.getNome(), p.getCapacidade(), p.getInscritos());
    }
}

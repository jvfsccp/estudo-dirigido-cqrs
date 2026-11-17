package br.pucgoias.ads1253.turmas.leitura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Tabela de leitura: uma linha por turma, com a quantidade de inscritos já calculada. */
@Entity
@Table(name = "painel_turma")
public class PainelTurma {

    @Id
    private UUID turmaId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private int capacidade;

    @Column(nullable = false)
    private int inscritos;

    protected PainelTurma() {
        // exigido pela JPA
    }

    public PainelTurma(UUID turmaId, String nome, int capacidade, int inscritos) {
        this.turmaId = turmaId;
        this.nome = nome;
        this.capacidade = capacidade;
        this.inscritos = inscritos;
    }

    public void registrarInscricao() {
        inscritos++;
    }

    public void registrarCancelamento() {
        inscritos--;
    }

    public UUID getTurmaId() {
        return turmaId;
    }

    public String getNome() {
        return nome;
    }

    public int getCapacidade() {
        return capacidade;
    }

    public int getInscritos() {
        return inscritos;
    }
}

package br.pucgoias.ads1253.turmas.leitura;

import java.util.List;
import java.util.UUID;

/** Projeção: dados de uma turma e a lista de e-mails dos inscritos. */
public record DetalheTurma(UUID id, String nome, int capacidade, List<String> emails) {
}

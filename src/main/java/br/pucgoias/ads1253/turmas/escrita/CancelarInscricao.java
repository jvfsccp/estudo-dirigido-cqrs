package br.pucgoias.ads1253.turmas.escrita;

import java.util.UUID;

/** Comando: intenção de cancelar a inscrição de um estudante em uma turma. */
public record CancelarInscricao(UUID turmaId, String email) {
}

package br.pucgoias.ads1253.turmas.dominio;

import java.time.Instant;
import java.util.UUID;

/** Evento de domínio: a inscrição de um estudante em uma turma foi cancelada. */
public record InscricaoCancelada(UUID turmaId, String email, Instant ocorridoEm) {
}

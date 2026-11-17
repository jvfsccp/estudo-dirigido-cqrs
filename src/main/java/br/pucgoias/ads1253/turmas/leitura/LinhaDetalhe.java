package br.pucgoias.ads1253.turmas.leitura;

import java.util.UUID;

/**
 * Linha intermediária da consulta de detalhe: uma por inscrito (o e-mail é nulo
 * quando a turma não tem inscritos). O serviço de consultas as agrupa em um DetalheTurma.
 */
public record LinhaDetalhe(UUID id, String nome, int capacidade, String email) {
}

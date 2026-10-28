package br.edu.ifrs.sfm.teste;

import br.edu.ifrs.sfm.controle.ModoInterface;
import br.edu.ifrs.sfm.core.resultado.CodigoResultado;

import java.time.Instant;

/** Evento anonimizado: sem senha, sem nome de arquivo. */
public record EventoTeste(Instant instante, TipoEvento tipo, ModoInterface modo, String tarefa, CodigoResultado codigo) {}

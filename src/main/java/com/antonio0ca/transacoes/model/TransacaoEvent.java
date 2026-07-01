package com.antonio0ca.transacoes.model;

import java.math.BigDecimal;

/**
 * Evento bruto publicado no Kafka pelo produtor.
 *
 * Ainda nao possui status: a classificacao antifraude e responsabilidade do
 * consumidor. O instante e transportado como epoch em milissegundos para manter
 * a serializacao JSON do Kafka simples e independente de modulos de data/hora.
 */
public record TransacaoEvent(
        String id,
        String contaOrigem,
        String contaDestino,
        BigDecimal valor,
        MetodoPagamento metodo,
        String uf,
        long momentoEpochMs
) {
}

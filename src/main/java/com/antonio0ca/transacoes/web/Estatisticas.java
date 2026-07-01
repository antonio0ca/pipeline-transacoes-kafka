package com.antonio0ca.transacoes.web;

import java.math.BigDecimal;
import java.util.Map;

/** Fotografia agregada do pipeline, consumida pelo dashboard. */
public record Estatisticas(
        long total,
        BigDecimal volumeTotal,
        long aprovadas,
        long suspeitas,
        long negadas,
        double taxaAprovacao,
        Map<String, BigDecimal> volumePorMetodo
) {
}

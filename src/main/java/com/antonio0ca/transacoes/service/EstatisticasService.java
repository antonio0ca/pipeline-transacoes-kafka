package com.antonio0ca.transacoes.service;

import com.antonio0ca.transacoes.model.MetodoPagamento;
import com.antonio0ca.transacoes.model.StatusTransacao;
import com.antonio0ca.transacoes.model.Transacao;
import com.antonio0ca.transacoes.web.Estatisticas;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agregados em memoria alimentados pelo consumidor.
 *
 * Mantem contadores e volumes acumulados para servir o dashboard sem precisar
 * varrer o banco a cada requisicao.
 */
@Service
public class EstatisticasService {

    private long total;
    private BigDecimal volumeTotal = BigDecimal.ZERO;
    private final Map<StatusTransacao, Long> porStatus = new EnumMap<>(StatusTransacao.class);
    private final Map<MetodoPagamento, BigDecimal> volumePorMetodo = new EnumMap<>(MetodoPagamento.class);

    public EstatisticasService() {
        for (StatusTransacao s : StatusTransacao.values()) {
            porStatus.put(s, 0L);
        }
        for (MetodoPagamento m : MetodoPagamento.values()) {
            volumePorMetodo.put(m, BigDecimal.ZERO);
        }
    }

    public synchronized void registrar(Transacao transacao) {
        total++;
        volumeTotal = volumeTotal.add(transacao.getValor());
        porStatus.merge(transacao.getStatus(), 1L, Long::sum);
        volumePorMetodo.merge(transacao.getMetodo(), transacao.getValor(), BigDecimal::add);
    }

    public synchronized Estatisticas snapshot() {
        long aprovadas = porStatus.getOrDefault(StatusTransacao.APROVADA, 0L);
        long suspeitas = porStatus.getOrDefault(StatusTransacao.SUSPEITA, 0L);
        long negadas = porStatus.getOrDefault(StatusTransacao.NEGADA, 0L);
        double taxa = total == 0 ? 0.0 : (aprovadas * 100.0) / total;

        Map<String, BigDecimal> porMetodo = new LinkedHashMap<>();
        for (Map.Entry<MetodoPagamento, BigDecimal> e : volumePorMetodo.entrySet()) {
            porMetodo.put(e.getKey().name(), e.getValue());
        }

        return new Estatisticas(
                total,
                volumeTotal,
                aprovadas,
                suspeitas,
                negadas,
                Math.round(taxa * 10.0) / 10.0,
                porMetodo
        );
    }
}

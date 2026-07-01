package com.antonio0ca.transacoes.service;

import com.antonio0ca.transacoes.model.StatusTransacao;
import com.antonio0ca.transacoes.model.TransacaoEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Regras de negocio da avaliacao antifraude.
 *
 * Combina limites de valor com uma checagem de velocidade (numero de transacoes
 * da mesma conta em uma janela curta de tempo). E deterministico e explicavel,
 * de proposito: cada decisao carrega um motivo.
 */
@Service
public class AntifraudeService {

    private final BigDecimal limiteNegacao;
    private final BigDecimal limiteSuspeita;
    private final int maxPorJanela;
    private final long janelaMs;

    private final Map<String, Deque<Long>> historicoPorConta = new ConcurrentHashMap<>();

    public AntifraudeService(
            @Value("${app.antifraude.limite-negacao:50000}") BigDecimal limiteNegacao,
            @Value("${app.antifraude.limite-suspeita:10000}") BigDecimal limiteSuspeita,
            @Value("${app.antifraude.max-por-janela:5}") int maxPorJanela,
            @Value("${app.antifraude.janela-ms:10000}") long janelaMs) {
        this.limiteNegacao = limiteNegacao;
        this.limiteSuspeita = limiteSuspeita;
        this.maxPorJanela = maxPorJanela;
        this.janelaMs = janelaMs;
    }

    public Resultado classificar(TransacaoEvent evento) {
        boolean velocidadeExcedida = registrarEChecarVelocidade(evento.contaOrigem(), evento.momentoEpochMs());

        if (evento.valor().compareTo(limiteNegacao) >= 0) {
            return new Resultado(StatusTransacao.NEGADA, "Valor acima do limite permitido");
        }
        if (evento.valor().compareTo(limiteSuspeita) >= 0) {
            return new Resultado(StatusTransacao.SUSPEITA, "Valor elevado para revisao manual");
        }
        if (velocidadeExcedida) {
            return new Resultado(StatusTransacao.SUSPEITA, "Multiplas transacoes em curto intervalo");
        }
        return new Resultado(StatusTransacao.APROVADA, null);
    }

    private boolean registrarEChecarVelocidade(String conta, long momentoMs) {
        Deque<Long> janela = historicoPorConta.computeIfAbsent(conta, c -> new ArrayDeque<>());
        synchronized (janela) {
            janela.addLast(momentoMs);
            long limite = momentoMs - janelaMs;
            while (!janela.isEmpty() && janela.peekFirst() < limite) {
                janela.pollFirst();
            }
            return janela.size() > maxPorJanela;
        }
    }

    /** Decisao da antifraude, com o motivo associado. */
    public record Resultado(StatusTransacao status, String motivo) {
    }
}

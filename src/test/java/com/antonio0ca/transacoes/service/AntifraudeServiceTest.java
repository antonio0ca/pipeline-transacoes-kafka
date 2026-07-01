package com.antonio0ca.transacoes.service;

import com.antonio0ca.transacoes.model.MetodoPagamento;
import com.antonio0ca.transacoes.model.StatusTransacao;
import com.antonio0ca.transacoes.model.TransacaoEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes das regras de negocio da antifraude. Sao unitarios puros: nao sobem o
 * contexto Spring nem dependem de Kafka ou banco de dados.
 */
class AntifraudeServiceTest {

    private AntifraudeService novoServico() {
        // limite de negacao 50.000, suspeita 10.000, no maximo 3 por janela de 10s
        return new AntifraudeService(new BigDecimal("50000"), new BigDecimal("10000"), 3, 10_000L);
    }

    private TransacaoEvent evento(String conta, BigDecimal valor, long momentoMs) {
        return new TransacaoEvent("id-" + momentoMs, conta, "****9999", valor, MetodoPagamento.PIX, "SP", momentoMs);
    }

    @Test
    void valorAcimaDoLimiteDeveSerNegado() {
        AntifraudeService antifraude = novoServico();

        AntifraudeService.Resultado r = antifraude.classificar(evento("****0001", new BigDecimal("55000"), 1_000L));

        assertEquals(StatusTransacao.NEGADA, r.status());
        assertTrue(r.motivo().toLowerCase().contains("limite"));
    }

    @Test
    void valorElevadoDeveSerSuspeito() {
        AntifraudeService antifraude = novoServico();

        AntifraudeService.Resultado r = antifraude.classificar(evento("****0002", new BigDecimal("15000"), 1_000L));

        assertEquals(StatusTransacao.SUSPEITA, r.status());
    }

    @Test
    void valorNormalDeveSerAprovado() {
        AntifraudeService antifraude = novoServico();

        AntifraudeService.Resultado r = antifraude.classificar(evento("****0003", new BigDecimal("500"), 1_000L));

        assertEquals(StatusTransacao.APROVADA, r.status());
        assertEquals(null, r.motivo());
    }

    @Test
    void muitasTransacoesDaMesmaContaDevemDispararVelocidade() {
        AntifraudeService antifraude = novoServico();
        String conta = "****0004";
        BigDecimal valorNormal = new BigDecimal("100");

        // As tres primeiras, dentro do limite, sao aprovadas
        assertEquals(StatusTransacao.APROVADA, antifraude.classificar(evento(conta, valorNormal, 1_000L)).status());
        assertEquals(StatusTransacao.APROVADA, antifraude.classificar(evento(conta, valorNormal, 1_100L)).status());
        assertEquals(StatusTransacao.APROVADA, antifraude.classificar(evento(conta, valorNormal, 1_200L)).status());

        // A quarta na mesma janela excede a velocidade e vira suspeita
        AntifraudeService.Resultado quarta = antifraude.classificar(evento(conta, valorNormal, 1_300L));
        assertEquals(StatusTransacao.SUSPEITA, quarta.status());
        assertTrue(quarta.motivo().toLowerCase().contains("intervalo"));
    }

    @Test
    void transacoesForaDaJanelaNaoDisparamVelocidade() {
        AntifraudeService antifraude = novoServico();
        String conta = "****0005";
        BigDecimal valorNormal = new BigDecimal("100");

        // Espacadas alem da janela de 10s: nunca acumulam para exceder o limite
        for (int i = 0; i < 6; i++) {
            long momento = i * 20_000L; // 20s de intervalo
            assertEquals(StatusTransacao.APROVADA, antifraude.classificar(evento(conta, valorNormal, momento)).status());
        }
    }
}

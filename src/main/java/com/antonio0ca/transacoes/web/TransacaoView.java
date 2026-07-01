package com.antonio0ca.transacoes.web;

import com.antonio0ca.transacoes.model.Transacao;

import java.math.BigDecimal;

/** Projecao leve de uma transacao para o dashboard (stream e listagem). */
public record TransacaoView(
        String id,
        String contaOrigem,
        String contaDestino,
        BigDecimal valor,
        String metodo,
        String status,
        String motivo,
        String uf,
        long momentoEpochMs
) {

    public static TransacaoView de(Transacao t) {
        String idCurto = t.getTransacaoId().length() > 8
                ? t.getTransacaoId().substring(0, 8)
                : t.getTransacaoId();
        return new TransacaoView(
                idCurto,
                t.getContaOrigem(),
                t.getContaDestino(),
                t.getValor(),
                t.getMetodo().name(),
                t.getStatus().name(),
                t.getMotivo(),
                t.getUf(),
                t.getMomento().toEpochMilli()
        );
    }
}

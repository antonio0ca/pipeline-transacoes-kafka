package com.antonio0ca.transacoes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** Transacao ja avaliada pela antifraude e persistida no banco. */
@Entity
@Table(name = "transacoes", indexes = @Index(name = "idx_transacoes_momento", columnList = "momento"))
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String transacaoId;

    @Column(nullable = false, length = 12)
    private String contaOrigem;

    @Column(nullable = false, length = 12)
    private String contaDestino;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPagamento metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatusTransacao status;

    @Column(length = 2)
    private String uf;

    @Column(length = 80)
    private String motivo;

    @Column(nullable = false)
    private Instant momento;

    protected Transacao() {
        // exigido pelo JPA
    }

    public Transacao(TransacaoEvent evento, StatusTransacao status, String motivo) {
        this.transacaoId = evento.id();
        this.contaOrigem = evento.contaOrigem();
        this.contaDestino = evento.contaDestino();
        this.valor = evento.valor();
        this.metodo = evento.metodo();
        this.uf = evento.uf();
        this.status = status;
        this.motivo = motivo;
        this.momento = Instant.ofEpochMilli(evento.momentoEpochMs());
    }

    public Long getId() {
        return id;
    }

    public String getTransacaoId() {
        return transacaoId;
    }

    public String getContaOrigem() {
        return contaOrigem;
    }

    public String getContaDestino() {
        return contaDestino;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public MetodoPagamento getMetodo() {
        return metodo;
    }

    public StatusTransacao getStatus() {
        return status;
    }

    public String getUf() {
        return uf;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getMomento() {
        return momento;
    }
}

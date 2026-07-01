package com.antonio0ca.transacoes.producer;

import com.antonio0ca.transacoes.model.MetodoPagamento;
import com.antonio0ca.transacoes.model.TransacaoEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gera um fluxo continuo de transacoes ficticias e as publica no Kafka.
 *
 * A distribuicao de valores concentra-se em quantias pequenas, com uma cauda
 * rara de valores altos para exercitar as regras antifraude. Um conjunto
 * limitado de contas faz a checagem de velocidade disparar de vez em quando.
 */
@Component
public class SimuladorTransacoes {

    private static final String[] UFS = {
            "SP", "RJ", "MG", "RS", "PR", "SC", "BA", "PE", "CE", "DF"
    };
    private static final MetodoPagamento[] METODOS = MetodoPagamento.values();

    private final TransacaoProducer producer;
    private final int qtdContas;

    public SimuladorTransacoes(TransacaoProducer producer,
                               @Value("${app.qtd-contas:40}") int qtdContas) {
        this.producer = producer;
        this.qtdContas = qtdContas;
    }

    @Scheduled(fixedRateString = "${app.intervalo-ms:900}")
    public void gerar() {
        int quantidade = 1 + ThreadLocalRandom.current().nextInt(3);
        for (int i = 0; i < quantidade; i++) {
            producer.enviar(criarTransacao());
        }
    }

    private TransacaoEvent criarTransacao() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new TransacaoEvent(
                UUID.randomUUID().toString(),
                conta(),
                conta(),
                sortearValor(),
                METODOS[random.nextInt(METODOS.length)],
                UFS[random.nextInt(UFS.length)],
                System.currentTimeMillis()
        );
    }

    private String conta() {
        return String.format("****%04d", ThreadLocalRandom.current().nextInt(qtdContas));
    }

    private BigDecimal sortearValor() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double sorteio = random.nextDouble();
        double valor;
        if (sorteio < 0.90) {
            valor = 10 + random.nextDouble() * 1990;          // 10 a 2.000
        } else if (sorteio < 0.98) {
            valor = 2000 + random.nextDouble() * 13000;       // 2.000 a 15.000
        } else {
            valor = 15000 + random.nextDouble() * 65000;      // 15.000 a 80.000
        }
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }
}

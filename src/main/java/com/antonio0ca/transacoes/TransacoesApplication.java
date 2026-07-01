package com.antonio0ca.transacoes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ponto de entrada da aplicacao.
 *
 * O mesmo processo hospeda o produtor (simulador de transacoes) e o consumidor
 * (antifraude + persistencia + stream), deixando o fluxo completo do Kafka
 * visivel em uma unica execucao.
 */
@SpringBootApplication
@EnableScheduling
public class TransacoesApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransacoesApplication.class, args);
    }
}

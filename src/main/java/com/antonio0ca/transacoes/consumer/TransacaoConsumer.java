package com.antonio0ca.transacoes.consumer;

import com.antonio0ca.transacoes.model.Transacao;
import com.antonio0ca.transacoes.model.TransacaoEvent;
import com.antonio0ca.transacoes.repository.TransacaoRepository;
import com.antonio0ca.transacoes.service.AntifraudeService;
import com.antonio0ca.transacoes.service.EstatisticasService;
import com.antonio0ca.transacoes.service.TransacaoStreamService;
import com.antonio0ca.transacoes.web.TransacaoView;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consome os eventos do Kafka e executa o coracao do pipeline: avalia a
 * antifraude, persiste o resultado, atualiza os agregados e transmite ao
 * dashboard.
 */
@Component
public class TransacaoConsumer {

    private final AntifraudeService antifraude;
    private final TransacaoRepository repository;
    private final EstatisticasService estatisticas;
    private final TransacaoStreamService stream;

    public TransacaoConsumer(AntifraudeService antifraude,
                             TransacaoRepository repository,
                             EstatisticasService estatisticas,
                             TransacaoStreamService stream) {
        this.antifraude = antifraude;
        this.repository = repository;
        this.estatisticas = estatisticas;
        this.stream = stream;
    }

    @KafkaListener(topics = "${app.topico}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumir(TransacaoEvent evento) {
        AntifraudeService.Resultado resultado = antifraude.classificar(evento);

        Transacao transacao = new Transacao(evento, resultado.status(), resultado.motivo());
        repository.save(transacao);
        estatisticas.registrar(transacao);
        stream.publicar(TransacaoView.de(transacao));
    }
}

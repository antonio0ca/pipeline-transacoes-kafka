package com.antonio0ca.transacoes.producer;

import com.antonio0ca.transacoes.model.TransacaoEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publica eventos de transacao no topico do Kafka. */
@Component
public class TransacaoProducer {

    private final KafkaTemplate<String, TransacaoEvent> kafkaTemplate;
    private final String topico;

    public TransacaoProducer(KafkaTemplate<String, TransacaoEvent> kafkaTemplate,
                             @Value("${app.topico}") String topico) {
        this.kafkaTemplate = kafkaTemplate;
        this.topico = topico;
    }

    public void enviar(TransacaoEvent evento) {
        // A conta de origem e usada como chave: transacoes da mesma conta caem
        // sempre na mesma particao, preservando a ordem por conta.
        kafkaTemplate.send(topico, evento.contaOrigem(), evento);
    }
}

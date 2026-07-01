package com.antonio0ca.transacoes.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Cria o topico de transacoes na inicializacao, caso ainda nao exista. */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic transacoesTopic(@Value("${app.topico}") String topico) {
        return TopicBuilder.name(topico)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

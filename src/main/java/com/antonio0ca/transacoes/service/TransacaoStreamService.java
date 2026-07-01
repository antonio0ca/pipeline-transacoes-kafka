package com.antonio0ca.transacoes.service;

import com.antonio0ca.transacoes.web.TransacaoView;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Transmite cada transacao processada para os navegadores conectados via SSE
 * (Server-Sent Events). Mantem a lista de inscritos e remove os que caem.
 */
@Service
public class TransacaoStreamService {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final List<SseEmitter> inscritos = new CopyOnWriteArrayList<>();

    public SseEmitter novoInscrito() {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitter.onCompletion(() -> inscritos.remove(emitter));
        emitter.onTimeout(() -> inscritos.remove(emitter));
        emitter.onError(e -> inscritos.remove(emitter));
        inscritos.add(emitter);
        return emitter;
    }

    public void publicar(TransacaoView view) {
        for (SseEmitter emitter : inscritos) {
            try {
                emitter.send(SseEmitter.event()
                        .name("transacao")
                        .data(view, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                inscritos.remove(emitter);
            }
        }
    }
}

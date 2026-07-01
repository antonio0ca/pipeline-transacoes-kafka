package com.antonio0ca.transacoes.web;

import com.antonio0ca.transacoes.repository.TransacaoRepository;
import com.antonio0ca.transacoes.service.EstatisticasService;
import com.antonio0ca.transacoes.service.TransacaoStreamService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/** Endpoints consumidos pelo dashboard: stream ao vivo, agregados e listagem. */
@RestController
@RequestMapping("/api")
public class TransacaoController {

    private final TransacaoStreamService stream;
    private final EstatisticasService estatisticas;
    private final TransacaoRepository repository;

    public TransacaoController(TransacaoStreamService stream,
                               EstatisticasService estatisticas,
                               TransacaoRepository repository) {
        this.stream = stream;
        this.estatisticas = estatisticas;
        this.repository = repository;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return stream.novoInscrito();
    }

    @GetMapping("/estatisticas")
    public Estatisticas estatisticas() {
        return estatisticas.snapshot();
    }

    @GetMapping("/transacoes/recentes")
    public List<TransacaoView> recentes() {
        return repository.findTop12ByOrderByIdDesc().stream()
                .map(TransacaoView::de)
                .toList();
    }
}

package com.antonio0ca.transacoes.repository;

import com.antonio0ca.transacoes.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acesso as transacoes persistidas. */
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    /** Ultimas transacoes gravadas, da mais recente para a mais antiga. */
    List<Transacao> findTop12ByOrderByIdDesc();
}

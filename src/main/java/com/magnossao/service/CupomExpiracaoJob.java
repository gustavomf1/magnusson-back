package com.magnossao.service;

import com.magnossao.entity.Cupom;
import com.magnossao.entity.StatusCupom;
import com.magnossao.repository.CupomRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CupomExpiracaoJob {

    private static final Logger log = LoggerFactory.getLogger(CupomExpiracaoJob.class);

    private final CupomRepository cupomRepository;

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    @Transactional
    public void expirarCupons() {
        List<Cupom> vencidos = cupomRepository.findByStatusAndExpiraEmBefore(StatusCupom.ATIVO, LocalDateTime.now());
        for (Cupom cupom : vencidos) {
            cupom.setStatus(StatusCupom.EXPIRADO);
            cupomRepository.save(cupom);
        }
        if (!vencidos.isEmpty()) {
            log.info("Expirados {} cupons de cashback", vencidos.size());
        }
    }
}

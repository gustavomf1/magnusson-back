package com.magnossao.service;

import com.magnossao.entity.Cupom;
import com.magnossao.entity.StatusCupom;
import com.magnossao.repository.CupomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CupomExpiracaoJobTest {

    @Mock
    private CupomRepository cupomRepository;

    @InjectMocks
    private CupomExpiracaoJob job;

    private Cupom cupomAtivo(LocalDateTime expiraEm) {
        Cupom cupom = new Cupom();
        cupom.setId(1L);
        cupom.setStatus(StatusCupom.ATIVO);
        cupom.setExpiraEm(expiraEm);
        return cupom;
    }

    @Test
    void marcaComoExpiradoOsCuponsVencidosRetornadosPelaQuery() {
        Cupom vencido = cupomAtivo(LocalDateTime.now().minusDays(1));
        when(cupomRepository.findByStatusAndExpiraEmBefore(eq(StatusCupom.ATIVO), any(LocalDateTime.class)))
                .thenReturn(List.of(vencido));
        when(cupomRepository.save(any(Cupom.class))).thenAnswer(inv -> inv.getArgument(0));

        job.expirarCupons();

        ArgumentCaptor<Cupom> captor = ArgumentCaptor.forClass(Cupom.class);
        verify(cupomRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusCupom.EXPIRADO);
    }

    @Test
    void naoFazNadaQuandoQueryNaoRetornaCupons() {
        when(cupomRepository.findByStatusAndExpiraEmBefore(eq(StatusCupom.ATIVO), any(LocalDateTime.class)))
                .thenReturn(List.of());

        job.expirarCupons();

        verify(cupomRepository, never()).save(any());
    }
}

package com.magnossao.dto.response;

import com.magnossao.entity.RegraCashback;

import java.math.BigDecimal;

public record RegraCashbackInfoDto(BigDecimal percentual, Integer prazoValidadeDias) {

    public static RegraCashbackInfoDto from(RegraCashback regra) {
        return new RegraCashbackInfoDto(regra.getPercentual(), regra.getPrazoValidadeDias());
    }
}

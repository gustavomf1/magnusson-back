package com.magnossao.dto.request;

import java.math.BigDecimal;

public record RegraCashbackRequest(BigDecimal percentual, Integer prazoValidadeDias) {
}

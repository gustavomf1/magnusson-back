package com.magnossao.dto.request;

public record CheckoutItemRequest(Long skuId, int quantidade, Long cupomId) {
}

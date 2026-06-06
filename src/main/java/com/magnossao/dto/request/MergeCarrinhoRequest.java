package com.magnossao.dto.request;

import java.util.List;

public record MergeCarrinhoRequest(List<CarrinhoItemRequest> itens) {}

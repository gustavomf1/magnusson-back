package com.magnossao.dto.request;

import java.util.List;

public record CheckoutRequest(
    List<CarrinhoItemRequest> itens,
    DadosNfRequest dadosNf,
    EnderecoRequest endereco
) {}

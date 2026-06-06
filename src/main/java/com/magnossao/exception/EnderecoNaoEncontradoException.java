package com.magnossao.exception;

public class EnderecoNaoEncontradoException extends RuntimeException {
    public EnderecoNaoEncontradoException(Long id) {
        super("Endereço não encontrado: " + id);
    }
}

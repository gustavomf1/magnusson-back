package com.magnossao.service;

import com.magnossao.dto.request.EnderecoRequest;
import com.magnossao.dto.response.EnderecoResponse;
import com.magnossao.entity.Endereco;
import com.magnossao.entity.Usuario;
import com.magnossao.exception.EnderecoNaoEncontradoException;
import com.magnossao.repository.EnderecoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;

    public EnderecoService(EnderecoRepository enderecoRepository) {
        this.enderecoRepository = enderecoRepository;
    }

    public List<EnderecoResponse> listar(Long usuarioId) {
        return enderecoRepository.findByUsuarioId(usuarioId).stream()
                .map(EnderecoResponse::from).toList();
    }

    public EnderecoResponse criar(Usuario usuario, EnderecoRequest req) {
        Endereco e = new Endereco();
        e.setUsuario(usuario);
        e.setLogradouro(req.logradouro());
        e.setNumero(req.numero());
        e.setComplemento(req.complemento());
        e.setBairro(req.bairro());
        e.setCep(req.cep());
        e.setCidade(req.cidade());
        e.setUf(req.uf());
        e.setPrincipal(Boolean.TRUE.equals(req.principal()));
        return EnderecoResponse.from(enderecoRepository.save(e));
    }

    public void deletar(Long usuarioId, Long enderecoId) {
        Endereco e = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new EnderecoNaoEncontradoException(enderecoId));
        if (!e.getUsuario().getId().equals(usuarioId)) {
            throw new org.springframework.security.access.AccessDeniedException("Acesso negado");
        }
        enderecoRepository.delete(e);
    }
}

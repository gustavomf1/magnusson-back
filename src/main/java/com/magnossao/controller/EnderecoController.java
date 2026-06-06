package com.magnossao.controller;

import com.magnossao.dto.request.EnderecoRequest;
import com.magnossao.dto.response.EnderecoResponse;
import com.magnossao.entity.Usuario;
import com.magnossao.repository.UsuarioRepository;
import com.magnossao.service.EnderecoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enderecos")
@RequiredArgsConstructor
public class EnderecoController {

    private final EnderecoService enderecoService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public List<EnderecoResponse> listar(Authentication auth) {
        return enderecoService.listar(usuario(auth).getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnderecoResponse criar(@RequestBody EnderecoRequest req, Authentication auth) {
        return enderecoService.criar(usuario(auth), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id, Authentication auth) {
        enderecoService.deletar(usuario(auth).getId(), id);
    }

    private Usuario usuario(Authentication auth) {
        if (auth.getPrincipal() instanceof Usuario u) return u;
        return usuarioRepository.findByEmail(auth.getName()).orElseThrow();
    }
}

package com.magnossao.controller;

import com.magnossao.dto.response.CupomResponse;
import com.magnossao.entity.Usuario;
import com.magnossao.repository.UsuarioRepository;
import com.magnossao.service.CashbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/cashback")
@RequiredArgsConstructor
public class CashbackController {

    private final CashbackService cashbackService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/carteira")
    public List<CupomResponse> carteira(Authentication authentication) {
        Usuario usuario = resolver(authentication);
        return cashbackService.listarCarteira(usuario.getId());
    }

    private Usuario resolver(Authentication auth) {
        if (auth.getPrincipal() instanceof Usuario u) return u;
        return usuarioRepository.findByEmail(auth.getName()).orElseThrow();
    }
}

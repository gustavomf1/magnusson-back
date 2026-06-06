package com.magnossao.controller;

import com.magnossao.dto.request.CheckoutRequest;
import com.magnossao.dto.response.PedidoResponse;
import com.magnossao.dto.response.PedidoResumoResponse;
import com.magnossao.entity.Usuario;
import com.magnossao.repository.UsuarioRepository;
import com.magnossao.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse checkout(@RequestBody CheckoutRequest req, Authentication auth) {
        return pedidoService.checkout(req, resolverOpcional(auth));
    }

    @GetMapping("/{id}")
    public PedidoResponse buscar(@PathVariable Long id) {
        return pedidoService.buscarPorId(id);
    }

    @GetMapping
    public List<PedidoResumoResponse> historico(Authentication auth) {
        return pedidoService.historico(resolver(auth).getId());
    }

    private Usuario resolver(Authentication auth) {
        if (auth.getPrincipal() instanceof Usuario u) return u;
        return usuarioRepository.findByEmail(auth.getName()).orElseThrow();
    }

    private Usuario resolverOpcional(Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) return null;
        if (auth.getPrincipal() instanceof Usuario u) return u;
        return usuarioRepository.findByEmail(auth.getName()).orElse(null);
    }
}

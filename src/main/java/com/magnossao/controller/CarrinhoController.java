package com.magnossao.controller;

import com.magnossao.dto.request.CarrinhoItemRequest;
import com.magnossao.dto.request.MergeCarrinhoRequest;
import com.magnossao.dto.response.CarrinhoItemResponse;
import com.magnossao.entity.Usuario;
import com.magnossao.repository.UsuarioRepository;
import com.magnossao.service.CarrinhoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carrinho")
@RequiredArgsConstructor
public class CarrinhoController {

    private final CarrinhoService carrinhoService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public List<CarrinhoItemResponse> listar(Authentication auth) {
        return carrinhoService.listar(usuario(auth).getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarrinhoItemResponse adicionar(@RequestBody CarrinhoItemRequest req, Authentication auth) {
        return carrinhoService.adicionar(usuario(auth), req);
    }

    @PatchMapping("/{skuId}")
    public CarrinhoItemResponse atualizar(@PathVariable Long skuId,
                                          @RequestBody CarrinhoItemRequest req,
                                          Authentication auth) {
        return carrinhoService.atualizar(usuario(auth).getId(), skuId, req.quantidade());
    }

    @DeleteMapping("/{skuId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long skuId, Authentication auth) {
        carrinhoService.remover(usuario(auth).getId(), skuId);
    }

    @PostMapping("/merge")
    public List<CarrinhoItemResponse> merge(@RequestBody MergeCarrinhoRequest req, Authentication auth) {
        return carrinhoService.merge(usuario(auth), req);
    }

    private Usuario usuario(Authentication auth) {
        if (auth.getPrincipal() instanceof Usuario u) return u;
        return usuarioRepository.findByEmail(auth.getName()).orElseThrow();
    }
}

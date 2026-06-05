package com.magnossao.dto.response;

import com.magnossao.entity.Role;
import com.magnossao.entity.Usuario;

public record UsuarioResponse(Long id, String nome, String email, Role role) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getRole());
    }
}

package com.magnossao.controller;

import com.magnossao.dto.request.CadastroRequest;
import com.magnossao.dto.request.LoginRequest;
import com.magnossao.dto.response.UsuarioResponse;
import com.magnossao.entity.Usuario;
import com.magnossao.service.JwtService;
import com.magnossao.service.UsuarioService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse cadastro(@RequestBody @Valid CadastroRequest req) {
        return UsuarioResponse.from(usuarioService.cadastrar(req));
    }

    @PostMapping("/login")
    public UsuarioResponse login(@RequestBody @Valid LoginRequest req, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.senha()));
        Usuario usuario = (Usuario) auth.getPrincipal();
        String token = jwtService.gerar(usuario.getEmail(), usuario.getRole().name());
        response.addCookie(criarCookie("token", token, 86400));
        return UsuarioResponse.from(usuario);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletResponse response) {
        response.addCookie(criarCookie("token", "", 0));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(UsuarioResponse.from((Usuario) auth.getPrincipal()));
    }

    private Cookie criarCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Strict");
        return cookie;
    }
}

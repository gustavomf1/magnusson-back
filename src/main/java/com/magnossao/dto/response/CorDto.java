package com.magnossao.dto.response;

import java.util.List;

public record CorDto(Long id, String nome, String token, String hex, List<ImagemDto> imagens) {}

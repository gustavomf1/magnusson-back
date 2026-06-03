package com.magnossao.catalog;

public interface StorageService {

    record PresignedUploadResult(String uploadUrl, String chave, String urlPublica) {}

    PresignedUploadResult gerarUrlUpload(String chave, String contentType);

    void deletar(String chave);
}

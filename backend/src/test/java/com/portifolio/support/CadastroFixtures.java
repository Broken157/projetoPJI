package com.portifolio.support;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class CadastroFixtures {
    private static final AtomicLong CPFS = new AtomicLong(123450000);
    private static final AtomicLong CNPJS = new AtomicLong(123450000000L);

    private CadastroFixtures() {}

    public static void identificar(Map<String, Object> payload) {
        payload.put("username", "rf01_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        payload.put("cpf", cpf());
    }

    public static Map<String, Object> artista() {
        Map<String, Object> payload = new HashMap<>();
        identificar(payload);
        payload.put("tipoUsuario", "ARTISTA");
        payload.put("tipoPerfilArtistico", "ARTISTA_SOLO");
        payload.put("areaPrincipalId", 1);
        payload.put("dataNascimento", LocalDate.now().minusYears(25).toString());
        payload.put("telefone", "11999999999");
        return payload;
    }

    public static String cpf() {
        String base = String.valueOf(CPFS.incrementAndGet());
        base += digito(base, new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2});
        return base + digito(base, new int[]{11, 10, 9, 8, 7, 6, 5, 4, 3, 2});
    }

    public static String cnpj() {
        String base = String.valueOf(CNPJS.incrementAndGet());
        base += digito(base, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return base + digito(base, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
    }

    private static int digito(String base, int[] pesos) {
        int soma = 0;
        for (int indice = 0; indice < pesos.length; indice++) soma += (base.charAt(indice) - '0') * pesos[indice];
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}

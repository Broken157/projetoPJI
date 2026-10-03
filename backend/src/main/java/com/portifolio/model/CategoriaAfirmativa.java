package com.portifolio.model;

import com.portifolio.model.enums.DatabaseEnum;

/** Valores exatos de categoria_afirmativa_enum; IDs estaveis do catalogo da API. */
public enum CategoriaAfirmativa implements DatabaseEnum {
    MULHER(1, "Mulheres"),
    ETNICO_RACIAL(2, "Étnico-racial"),
    PCD(3, "Pessoas com deficiência"),
    LGBTQIA(4, "LGBTQIA+");

    private final Integer id;
    private final String nome;

    CategoriaAfirmativa(Integer id, String nome) { this.id = id; this.nome = nome; }
    public Integer getId() { return id; }
    public String getNome() { return nome; }
    @Override public String getDatabaseValue() { return name(); }
}

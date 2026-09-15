package com.portifolio.repository;

import com.portifolio.model.Vaga;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Campos exclusivos do schema instalado, sem associações artificiais de taxonomia. */
@Repository @RequiredArgsConstructor
public class OfficialLocalVagaRepository {
    private final JdbcTemplate jdbc;
    public Long inserir(Vaga v) {
        return jdbc.queryForObject("""
            insert into vagas(contratante_id,titulo,descricao,requisitos,remunera_valor,forma_pagamento,
                cidade,estado,endereco_completo,beneficios,modelo_trabalho,tipo_contrato,categoria,
                experiencia,data_limite_candidatura,abrangencia,status,data_publicacao)
            values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, 'aberta',?) returning id
            """,Long.class,v.getContratante().getUsuarioId(),v.getTitulo(),v.getDescricao(),v.getRequisitos(),
            v.getValorMinimo(),v.getLegacyFormaPagamento(),v.getCidade(),v.getEstado(),v.getEnderecoCompleto(),
            v.getBeneficios(),v.getModeloTrabalho().name().toLowerCase(java.util.Locale.ROOT),v.getTipoContrato(),
            v.getLegacyCategoria(),v.getExperiencia(),v.getDataLimiteCandidatura(),v.getLegacyAbrangencia(),v.getDataPublicacao());
    }
    public void atualizarCampos(Vaga v) {
        jdbc.update("update vagas set forma_pagamento=?,categoria=?,abrangencia=? where id=?",
            v.getLegacyFormaPagamento(),v.getLegacyCategoria(),v.getLegacyAbrangencia(),v.getId());
    }
    public void carregarCampos(Vaga v) {
        jdbc.query("select forma_pagamento,categoria,abrangencia from vagas where id=?",rs->{
            v.setLegacyFormaPagamento(rs.getString("forma_pagamento"));
            v.setLegacyCategoria(rs.getString("categoria"));
            v.setLegacyAbrangencia(rs.getString("abrangencia"));
        },v.getId());
    }
}

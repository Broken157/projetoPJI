create table perfis_artistas (
    usuario_id bigint primary key references usuarios(id) on delete cascade,
    biografia text,
    cidade varchar(100),
    estado char(2),
    url_portfolio varchar(255),
    tipo_perfil_artistico tipo_perfil_artistico_enum not null,
    disponivel_oportunidades boolean,
    raio_atuacao  abrangencia_enum,
    nome_integrantes varchar(150),
    banner_url varchar(255),
    ultima_atualizacao timestamp default current_timestamp
);

create table perfis_contratantes (
    usuario_id bigint primary key references usuarios(id) on delete cascade,
    nome_empresa varchar(150),
    tipo_contratante tipo_contratante_enum,
    biografia text,
    cidade varchar(100),
    estado char(2),
    banner_url varchar(255)
);

create table visualizacoes_perfil (
    id bigserial primary key,
    perfil_visitado_id bigint not null references usuarios(id) on delete cascade,
    data_visualizacao timestamp default current_timestamp
);


CREATE TABLE banco_talentos (
    contratante_id bigint NOT NULL,
    artista_id bigint NOT NULL,
    data_adicao timestamp NOT NULL DEFAULT current_timestamp,

    PRIMARY KEY (contratante_id, artista_id),

    CONSTRAINT fk_banco_talentos_contratante
        FOREIGN KEY (contratante_id)
        REFERENCES usuarios(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_banco_talentos_artista
        FOREIGN KEY (artista_id)
        REFERENCES usuarios(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_banco_talentos_ids_diferentes
        CHECK (contratante_id <> artista_id)
);

CREATE INDEX idx_banco_talentos_contratante ON banco_talentos(contratante_id);
CREATE INDEX idx_banco_talentos_artista ON banco_talentos(artista_id);
create index if not exists idx_visualizacoes_perfil_visitado on visualizacoes_perfil(perfil_visitado_id);

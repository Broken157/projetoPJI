create table portfolio_arquivos (
    id bigserial primary key,
    artista_id bigint not null references perfis_artistas(usuario_id) on delete cascade,
    url_arquivo varchar(255) not null,
    nome_original varchar(150) not null,
    tamanho_bytes integer not null,
    tipo_mime varchar(50) not null,
    data_upload timestamp default current_timestamp
);

create table embeds_externos (
    id bigserial primary key,
    artista_id bigint not null references perfis_artistas(usuario_id) on delete cascade,
    plataforma plataforma_embed_enum not null,
    url_original varchar(255) not null,
    codigo_iframe text not null,
    tipo_midia tipo_midia_enum not null, -- Derivado e validado via trigger
    legenda varchar(255),
    ordem_exibicao integer default 0,
    
    -- CHECK de correspondência de domínio da URL original por plataforma
    constraint chk_embed_url_dominio check (
        (plataforma = 'YOUTUBE'    and (url_original ilike '%youtube.com%' or url_original ilike '%youtu.be%')) or
        (plataforma = 'VIMEO'      and url_original ilike '%vimeo.com%') or
        (plataforma = 'SPOTIFY'    and url_original ilike '%spotify.com%') or
        (plataforma = 'SOUNDCLOUD' and url_original ilike '%soundcloud.com%')
    )
);
create index if not exists idx_portfolio_arquivos_artista on portfolio_arquivos(artista_id);
create index if not exists idx_embeds_externos_artista on embeds_externos(artista_id);
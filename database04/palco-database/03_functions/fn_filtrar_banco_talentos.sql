CREATE OR REPLACE FUNCTION fn_listar_banco_talentos(
    p_contratante_id bigint,
    p_termo varchar DEFAULT NULL,
    p_area_id smallint DEFAULT NULL,
    p_funcao_id bigint DEFAULT NULL,
    p_especializacao_id bigint DEFAULT NULL,
    p_cidade varchar DEFAULT NULL,
    p_estado varchar DEFAULT NULL,
    p_limit integer DEFAULT 20
)
RETURNS TABLE (
    artista_id bigint,
    nome_artista varchar,
    username varchar,
    area_nome varchar,
    funcao_nome varchar,
    especializacao_nome varchar,
    biografia text,
    cidade varchar,
    estado varchar,
    data_adicao timestamp
)
LANGUAGE plpgsql
AS $$
BEGIN

    ----------------------------------------------------------------
    -- Garante que o usuário é um contratante
    ----------------------------------------------------------------

    IF NOT EXISTS (
        SELECT 1
        FROM usuarios
        WHERE id = p_contratante_id
          AND tipo_usuario = 'CONTRATANTE'::tipo_usuario_enum
    ) THEN
        RAISE EXCEPTION
            'Usuário % não é um contratante válido.',
            p_contratante_id;
    END IF;


    ----------------------------------------------------------------
    -- Busca somente artistas salvos por esse contratante
    ----------------------------------------------------------------

    RETURN QUERY

    SELECT DISTINCT ON (
        u.id,
        aa.nome,
        COALESCE(f.nome, ''),
        COALESCE(e.nome, '')
    )
        u.id AS artista_id,
        u.nome AS nome_artista,
        u.username,
        aa.nome AS area_nome,
        COALESCE(f.nome, 'N/A') AS funcao_nome,
        COALESCE(e.nome, 'N/A') AS especializacao_nome,
        p.biografia,
        p.cidade,
        p.estado,
        bt.data_adicao

    FROM banco_talentos bt

    INNER JOIN usuarios u
        ON u.id = bt.artista_id

    INNER JOIN perfis_artistas p
        ON p.usuario_id = u.id

    INNER JOIN perfil_artista_area paa
        ON paa.perfil_artista_id = u.id
       AND paa.principal = true

    INNER JOIN areas_artisticas aa
        ON aa.id = paa.area_id

    LEFT JOIN perfil_artista_funcao paf
        ON paf.perfil_artista_id = u.id
       AND paf.area_id = paa.area_id

    LEFT JOIN funcoes f
        ON f.id = paf.funcao_id

    LEFT JOIN perfil_artista_especializacao pae
        ON pae.perfil_artista_id = u.id
       AND pae.area_id = paa.area_id

    LEFT JOIN especializacoes e
        ON e.id = pae.especializacao_id

    WHERE bt.contratante_id = p_contratante_id
      AND u.perfil_completo = true
      AND u.status_conta = 'ATIVA'

      AND (
          p_termo IS NULL
          OR u.nome ILIKE '%' || p_termo || '%'
          OR u.username ILIKE '%' || p_termo || '%'
          OR p.biografia ILIKE '%' || p_termo || '%'
      )

      AND (
          p_area_id IS NULL
          OR paa.area_id = p_area_id
      )

      AND (
          p_funcao_id IS NULL
          OR paf.funcao_id = p_funcao_id
      )

      AND (
          p_especializacao_id IS NULL
          OR pae.especializacao_id = p_especializacao_id
      )

      AND (
          p_cidade IS NULL
          OR p.cidade ILIKE '%' || p_cidade || '%'
      )

      AND (
          p_estado IS NULL
          OR p.estado = p_estado
      )

    ORDER BY
        u.id,
        aa.nome,
        COALESCE(f.nome, ''),
        COALESCE(e.nome, ''),
        bt.data_adicao DESC

    LIMIT GREATEST(1, LEAST(p_limit, 100));

END;
$$;
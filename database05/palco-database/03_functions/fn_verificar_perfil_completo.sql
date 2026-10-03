CREATE OR REPLACE FUNCTION fn_verificar_perfil_completo(p_usuario_id bigint)
RETURNS boolean
LANGUAGE plpgsql
AS $$
DECLARE
    v_tipo tipo_usuario_enum;
    v_cpf varchar(14);
    v_cnpj varchar(14);
    v_completo boolean := false;

    -- ARTISTA
    v_bio_loc_ok boolean := false;
    v_area_principal_ok boolean := false;
    v_funcao_ok boolean := false;
    v_especializacao_ok boolean := false;

    -- CONTRATANTE
    v_contratante_ok boolean := false;
BEGIN

    SELECT
        tipo_usuario,
        cpf,
        cnpj
    INTO
        v_tipo,
        v_cpf,
        v_cnpj
    FROM usuarios
    WHERE id = p_usuario_id;

    IF NOT FOUND THEN
        RETURN false;
    END IF;

    IF v_cpf IS NULL AND v_cnpj IS NULL THEN

        UPDATE usuarios
        SET perfil_completo = false
        WHERE id = p_usuario_id;

        RETURN false;

    END IF;

    IF v_tipo = 'ARTISTA' THEN

        SELECT EXISTS (
            SELECT 1
            FROM perfis_artistas pa
            WHERE pa.usuario_id = p_usuario_id
              AND pa.biografia IS NOT NULL
              AND trim(pa.biografia) <> ''
              AND pa.cidade IS NOT NULL
              AND trim(pa.cidade) <> ''
              AND pa.estado IS NOT NULL
              AND trim(pa.estado) <> ''
        )
        INTO v_bio_loc_ok;

        SELECT EXISTS (
            SELECT 1
            FROM perfil_artista_area paa
            WHERE paa.perfil_artista_id = p_usuario_id
              AND paa.principal = true
              AND paa.nivel_experiencia IS NOT NULL
        )
        INTO v_area_principal_ok;


        SELECT EXISTS (
            SELECT 1
            FROM perfil_artista_area paa
            INNER JOIN perfil_artista_funcao paf
                ON paf.perfil_artista_id = paa.perfil_artista_id
               AND paf.area_id = paa.area_id
            WHERE paa.perfil_artista_id = p_usuario_id
              AND paa.principal = true
        )
        INTO v_funcao_ok;

        SELECT EXISTS (
            SELECT 1
            FROM perfil_artista_area paa

            INNER JOIN perfil_artista_funcao paf
                ON paf.perfil_artista_id = paa.perfil_artista_id
               AND paf.area_id = paa.area_id

            INNER JOIN funcao_especializacao fe
                ON fe.funcao_id = paf.funcao_id

            INNER JOIN perfil_artista_especializacao pae
                ON pae.perfil_artista_id = paa.perfil_artista_id
               AND pae.area_id = paa.area_id
               AND pae.especializacao_id = fe.especializacao_id

            WHERE paa.perfil_artista_id = p_usuario_id
              AND paa.principal = true
        )
        INTO v_especializacao_ok;


        v_completo :=
            v_bio_loc_ok
            AND v_area_principal_ok
            AND v_funcao_ok
            AND v_especializacao_ok;


  
    ELSIF v_tipo = 'CONTRATANTE' THEN

   
        SELECT EXISTS (
            SELECT 1
            FROM perfis_contratantes pc
            WHERE pc.usuario_id = p_usuario_id

              AND pc.cidade IS NOT NULL
              AND trim(pc.cidade) <> ''

              AND pc.estado IS NOT NULL
              AND trim(pc.estado) <> ''

              AND (
                    v_cnpj IS NULL
                    OR (
                        pc.nome_empresa IS NOT NULL
                        AND trim(pc.nome_empresa) <> ''
                    )
                  )
        )
        INTO v_contratante_ok;


        v_completo := v_contratante_ok;

    END IF;

    UPDATE usuarios
    SET perfil_completo = v_completo
    WHERE id = p_usuario_id;


    RETURN v_completo;

END;
$$;
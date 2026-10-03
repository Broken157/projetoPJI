CREATE OR REPLACE PROCEDURE sp_enviar_candidatura(
    INOUT p_candidatura_id bigint DEFAULT NULL,
    p_vaga_id bigint DEFAULT NULL,
    p_artista_id bigint DEFAULT NULL,
    p_mensagem text DEFAULT NULL,
    p_link_portfolio varchar DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_status_vaga status_vaga_enum;
    v_perfil_completo boolean;
    v_status_conta status_conta_enum;
    v_tipo_usuario text;

    v_candidaturas_total integer;
    v_candidatura_ativa boolean;
BEGIN
 SELECT status
    INTO v_status_vaga
    FROM vagas
    WHERE id = p_vaga_id;

    IF v_status_vaga IS NULL THEN
        RAISE EXCEPTION
            'Vaga (ID: %) não encontrada.',
            p_vaga_id
            USING ERRCODE = 'P0002';
    END IF;

    -- Nova candidatura somente pode ser feita em vaga ABERTA
    IF v_status_vaga <> 'ABERTA'::status_vaga_enum THEN
        RAISE EXCEPTION
            'Candidatura recusada: a vaga não está aberta para novas candidaturas.'
            USING ERRCODE = '22000';
    END IF;

	SELECT
        upper(u.tipo_usuario::text),
        u.perfil_completo,
        u.status_conta
    INTO
        v_tipo_usuario,
        v_perfil_completo,
        v_status_conta
    FROM usuarios u
    WHERE u.id = p_artista_id;

    -- Somente ARTISTA pode se candidatar
    IF v_tipo_usuario IS NULL
       OR v_tipo_usuario <> 'ARTISTA' THEN

        RAISE EXCEPTION
            'Apenas usuários cadastrados como ARTISTA podem se candidatar a vagas.'
            USING ERRCODE = '22000';

    END IF;


    IF v_status_conta <> 'ATIVA'::status_conta_enum THEN

        RAISE EXCEPTION
            'Candidatura recusada: a conta do artista não está ativa.'
            USING ERRCODE = '22000';

    END IF;


    IF NOT COALESCE(v_perfil_completo, false) THEN

        RAISE EXCEPTION
            'Candidatura recusada: o perfil do artista deve estar completo para se candidatar (RF06).'
            USING ERRCODE = '22000';

    END IF;

    SELECT EXISTS (
        SELECT 1
        FROM candidaturas
        WHERE vaga_id = p_vaga_id
          AND artista_id = p_artista_id
          AND status IN (
              'PENDENTE'::status_candidatura_enum,
              'EM_ANALISE'::status_candidatura_enum
          )
    )
    INTO v_candidatura_ativa;


    IF v_candidatura_ativa THEN

        RAISE EXCEPTION
            'O artista já possui uma candidatura ativa para esta vaga.'
            USING ERRCODE = '23505';

    END IF;

    SELECT COUNT(*)
    INTO v_candidaturas_total
    FROM candidaturas
    WHERE vaga_id = p_vaga_id
	AND artista_id = p_artista_id;

    IF v_candidaturas_total >= 2 THEN

        RAISE EXCEPTION
            'Limite de recandidatura atingido. O artista só pode realizar duas candidaturas para esta vaga.'
            USING ERRCODE = '23505';

    END IF;

    INSERT INTO candidaturas (
        vaga_id,
        artista_id,
        mensagem_apresentacao,
        link_portfolio_candidatura,
        status,
        data_candidatura
    )
    VALUES (
        p_vaga_id,
        p_artista_id,
        p_mensagem,
        p_link_portfolio,
        'PENDENTE'::status_candidatura_enum,
        CURRENT_TIMESTAMP
    )
    RETURNING id
    INTO p_candidatura_id;


END;
$$;
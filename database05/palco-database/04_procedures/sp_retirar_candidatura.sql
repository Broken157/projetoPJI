CREATE OR REPLACE PROCEDURE sp_retirar_candidatura(
    p_candidatura_id bigint,
    p_artista_id bigint
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_vaga_id bigint;
    v_status_vaga status_vaga_enum;
    v_status_candidatura status_candidatura_enum;
BEGIN
    -- Busca a candidatura e a vaga relacionada
    SELECT 
        c.vaga_id,
        c.status,
        v.status
    INTO 
        v_vaga_id,
        v_status_candidatura,
        v_status_vaga
    FROM candidaturas c
    JOIN vagas v ON v.id = c.vaga_id
    WHERE c.id = p_candidatura_id
      AND c.artista_id = p_artista_id;

    -- Candidatura não encontrada
    IF v_vaga_id IS NULL THEN
        RAISE EXCEPTION 'Candidatura não encontrada.'
            USING ERRCODE = 'P0002';
    END IF;

    -- Só pode retirar enquanto a vaga estiver ABERTA ou PAUSADA
    IF v_status_vaga NOT IN (
        'ABERTA'::status_vaga_enum,
        'PAUSADA'::status_vaga_enum
    ) THEN
        RAISE EXCEPTION
            'Não é possível retirar a candidatura: a vaga não está ABERTA ou PAUSADA.'
            USING ERRCODE = '22000';
    END IF;

    -- Só pode retirar uma candidatura ativa
    IF v_status_candidatura = 'RETIRADA'::status_candidatura_enum THEN
        RAISE EXCEPTION 'Esta candidatura já foi retirada.'
            USING ERRCODE = '40900';
    END IF;

    -- Retira sem apagar o histórico
    UPDATE candidaturas
    SET status = 'RETIRADA'::status_candidatura_enum
    WHERE id = p_candidatura_id;

END;
$$;
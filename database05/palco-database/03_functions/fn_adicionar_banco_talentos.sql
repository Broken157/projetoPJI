CREATE OR REPLACE FUNCTION fn_adicionar_banco_talentos(
    p_contratante_id bigint,
    p_artista_id bigint
)
RETURNS boolean
LANGUAGE plpgsql
AS $$
BEGIN

    ----------------------------------------------------------------
    -- Verifica se o contratante existe e realmente é CONTRATANTE
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
    -- Verifica se o artista existe e realmente é ARTISTA
    ----------------------------------------------------------------

    IF NOT EXISTS (
        SELECT 1
        FROM usuarios
        WHERE id = p_artista_id
          AND tipo_usuario = 'ARTISTA'::tipo_usuario_enum
    ) THEN
        RAISE EXCEPTION
            'Usuário % não é um artista válido.',
            p_artista_id;
    END IF;


    ----------------------------------------------------------------
    -- Não permite adicionar a si próprio
    ----------------------------------------------------------------

    IF p_contratante_id = p_artista_id THEN
        RAISE EXCEPTION
            'O contratante não pode adicionar a própria conta ao banco de talentos.';
    END IF;



    INSERT INTO banco_talentos (
        contratante_id,
        artista_id
    )
    VALUES (
        p_contratante_id,
        p_artista_id
    )
    ON CONFLICT (contratante_id, artista_id) DO NOTHING;


    RETURN true;

END;
$$;
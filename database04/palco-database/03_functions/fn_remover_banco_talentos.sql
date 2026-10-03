CREATE OR REPLACE FUNCTION fn_remover_banco_talentos(
    p_contratante_id bigint,
    p_artista_id bigint
)
RETURNS boolean
LANGUAGE plpgsql
AS $$
DECLARE
    v_removido boolean;
BEGIN

    DELETE FROM banco_talentos
    WHERE contratante_id = p_contratante_id
      AND artista_id = p_artista_id
    RETURNING true INTO v_removido;

    RETURN COALESCE(v_removido, false);

END;
$$;
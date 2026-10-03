CREATE OR REPLACE FUNCTION fn_artista_no_banco_talentos(
    p_contratante_id bigint,
    p_artista_id bigint
)
RETURNS boolean
LANGUAGE sql
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM banco_talentos
        WHERE contratante_id = p_contratante_id
          AND artista_id = p_artista_id
    );
$$;
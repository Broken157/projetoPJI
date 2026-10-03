CREATE OR REPLACE FUNCTION fn_buscar_vagas(
    p_termo varchar DEFAULT NULL,
    p_cidade varchar DEFAULT NULL,
    p_estado varchar DEFAULT NULL,
    p_area_id smallint DEFAULT NULL,
    p_funcoes_ids bigint[] DEFAULT NULL,
    p_especializacoes_ids bigint[] DEFAULT NULL,
    p_categoria_afirmativa_enum categoria_afirmativa_enum DEFAULT NULL,
    p_modelo_trabalho modelo_trabalho_enum DEFAULT NULL,
    p_abrangencia abrangencia_enum DEFAULT NULL,
    p_experiencia varchar DEFAULT NULL,
    p_valor_minimo numeric DEFAULT NULL,
    p_valor_maximo numeric DEFAULT NULL,
    p_limit integer DEFAULT 20,
    p_cursor_data_publicacao timestamp DEFAULT NULL,
    p_cursor_id bigint DEFAULT NULL
)
RETURNS TABLE (
    vaga_id bigint,
    v_contratante_id bigint,
    v_area_id smallint,
    v_titulo varchar,
    v_cidade varchar,
    v_estado varchar,
    v_modelo_trabalho modelo_trabalho_enum,
    v_abrangencia abrangencia_enum,
    v_experiencia varchar,
    v_forma_remuneracao forma_remuneracao_enum,
    v_valor_minimo numeric,
    v_valor_maximo numeric,
    v_data_publicacao timestamp
)
LANGUAGE plpgsql
AS $$
BEGIN

    RETURN QUERY

    SELECT
        v.id,
        v.contratante_id,
        v.area_id,
        v.titulo,
        v.cidade,
        v.estado,
        v.modelo_trabalho,
        v.abrangencia,
        v.experiencia,
        v.forma_remuneracao,
        v.valor_minimo,
        v.valor_maximo,
        v.data_publicacao

    FROM vagas v

    WHERE v.status = 'ABERTA'

      -- Busca por termo
      AND (
          p_termo IS NULL
          OR v.titulo ILIKE '%' || p_termo || '%'
          OR v.descricao ILIKE '%' || p_termo || '%'
      )

      -- Localização
      AND (
          p_cidade IS NULL
          OR v.cidade ILIKE '%' || p_cidade || '%'
      )

      AND (
          p_estado IS NULL
          OR v.estado = p_estado
      )

      -- Área
      AND (
          p_area_id IS NULL
          OR v.area_id = p_area_id
      )

      -- Funções
      AND (
          p_funcoes_ids IS NULL
          OR EXISTS (
              SELECT 1
              FROM vaga_funcao vf
              WHERE vf.vaga_id = v.id
                AND vf.funcao_id = ANY(p_funcoes_ids)
          )
      )

      -- Especializações
      AND (
          p_especializacoes_ids IS NULL
          OR EXISTS (
              SELECT 1
              FROM vaga_especializacao ve
              WHERE ve.vaga_id = v.id
                AND ve.especializacao_id = ANY(p_especializacoes_ids)
          )
      )

      -- Categoria afirmativa
      AND (
          p_categoria_afirmativa_enum IS NULL
          OR v.categoria_afirmativa = p_categoria_afirmativa_enum
      )

      -- Modelo de trabalho
      AND (
          p_modelo_trabalho IS NULL
          OR v.modelo_trabalho = p_modelo_trabalho
      )

      -- Abrangência
      AND (
          p_abrangencia IS NULL
          OR v.abrangencia = p_abrangencia
      )

      -- Experiência
      AND (
          p_experiencia IS NULL
          OR v.experiencia ILIKE '%' || p_experiencia || '%'
      )

      -- Faixa de remuneração
      AND (
          p_valor_minimo IS NULL
          OR v.valor_maximo >= p_valor_minimo
          OR v.forma_remuneracao = 'A_COMBINAR'
      )

      AND (
          p_valor_maximo IS NULL
          OR v.valor_minimo <= p_valor_maximo
          OR v.forma_remuneracao = 'A_COMBINAR'
      )

      -- Paginação por cursor
      AND (
          p_cursor_data_publicacao IS NULL
          OR p_cursor_id IS NULL
          OR (
              v.data_publicacao,
              v.id
          ) < (
              p_cursor_data_publicacao,
              p_cursor_id
          )
      )

    ORDER BY
        v.data_publicacao DESC,
        v.id DESC

    LIMIT GREATEST(1, LEAST(p_limit, 100));

END;
$$;
CREATE OR REPLACE FUNCTION fn_validar_compatibilidade_vaga_especializacao()
RETURNS trigger AS $$
BEGIN
    -- Verifica se a especialização pertence a pelo menos uma das funções vinculadas à vaga
    IF NOT EXISTS (
        SELECT 1 
        FROM vaga_funcao vf
        JOIN funcao_especializacao fe ON fe.funcao_id = vf.funcao_id
        WHERE vf.vaga_id = NEW.vaga_id
          AND fe.especializacao_id = NEW.especializacao_id
    ) THEN
        -- CORREÇÃO: O '%' indica onde a variável NEW.especializacao_id será exibida
        RAISE EXCEPTION 'Incompatibilidade de taxonomia: a especialização % não é permitida para nenhuma das funções associadas a esta vaga (RF04).', NEW.especializacao_id
            USING ERRCODE = '22000';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
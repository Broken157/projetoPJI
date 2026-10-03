CREATE OR REPLACE PROCEDURE sp_cadastrar_usuario(
    INOUT p_usuario_id bigint DEFAULT NULL,

    -- Dados básicos
    p_nome varchar DEFAULT NULL,
    p_username varchar DEFAULT NULL,
    p_data_nascimento date DEFAULT NULL,
    p_telefone varchar DEFAULT NULL,
    p_email varchar DEFAULT NULL,
    p_senha varchar DEFAULT NULL,
    p_tipo_usuario tipo_usuario_enum DEFAULT NULL,
    p_cpf varchar DEFAULT NULL,
    p_cnpj varchar DEFAULT NULL,
    p_foto_perfil_url varchar DEFAULT NULL,

    -- Específico ARTISTA
    p_tipo_perfil_artista tipo_perfil_artistico_enum DEFAULT NULL,
    p_area_principal_id smallint DEFAULT NULL,

    -- Específico CONTRATANTE
    p_tipo_contratante tipo_contratante_enum DEFAULT NULL,
    p_nome_empresa varchar DEFAULT NULL,

    -- Responsável Legal
    p_nome_resp varchar DEFAULT NULL,
    p_tel_resp varchar DEFAULT NULL,
    p_email_resp varchar DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_tipo_text text;
    v_idade integer;
BEGIN

    IF p_tipo_usuario IS NULL THEN
        RAISE EXCEPTION
            'Tipo de usuário é obrigatório.';
    END IF;

    v_tipo_text := upper(p_tipo_usuario::text);

    IF v_tipo_text NOT IN ('ARTISTA', 'CONTRATANTE') THEN
        RAISE EXCEPTION
            'Tipo de usuário inválido. Deve ser ARTISTA ou CONTRATANTE.';
    END IF;


    IF p_nome IS NULL OR trim(p_nome) = '' THEN
        RAISE EXCEPTION
            'Nome é obrigatório.';
    END IF;

    IF p_username IS NULL OR trim(p_username) = '' THEN
        RAISE EXCEPTION
            'Username é obrigatório.';
    END IF;

    IF length(trim(p_username)) > 30 THEN
        RAISE EXCEPTION
            'Username deve possuir no máximo 30 caracteres.';
    END IF;

    IF p_username !~ '^[a-zA-Z0-9._]+$' THEN
        RAISE EXCEPTION
            'Username inválido. Utilize apenas letras, números, ponto e underscore.';
    END IF;

    IF p_data_nascimento IS NULL THEN
        RAISE EXCEPTION
            'Data de nascimento é obrigatória.';
    END IF;

    IF p_telefone IS NULL OR trim(p_telefone) = '' THEN
        RAISE EXCEPTION
            'Telefone é obrigatório.';
    END IF;

    IF p_email IS NULL OR trim(p_email) = '' THEN
        RAISE EXCEPTION
            'E-mail é obrigatório.';
    END IF;

    v_idade := extract(
        year FROM age(current_date, p_data_nascimento)
    );


    IF v_idade < 0 THEN
        RAISE EXCEPTION
            'Data de nascimento inválida.';
    END IF;


    IF v_tipo_text = 'CONTRATANTE' AND v_idade < 18 THEN
        RAISE EXCEPTION
            'Cadastro negado: CONTRATANTE deve ter no mínimo 18 anos de idade (Idade informada: %).',
            v_idade;

    ELSEIF v_tipo_text = 'ARTISTA' AND v_idade < 14 THEN
        RAISE EXCEPTION
            'Cadastro negado: ARTISTA deve ter no mínimo 14 anos de idade (Idade informada: %).',
            v_idade;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM usuarios
        WHERE lower(username) = lower(trim(p_username))
    ) THEN
        RAISE EXCEPTION
            'O username "%" já está cadastrado no sistema.',
            p_username;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM usuarios
        WHERE lower(email) = lower(trim(p_email))
    ) THEN
        RAISE EXCEPTION
            'O e-mail informado (%) já está cadastrado no sistema.',
            p_email;
    END IF;


    IF p_cpf IS NOT NULL
       AND trim(p_cpf) <> ''
       AND EXISTS (
           SELECT 1
           FROM usuarios
           WHERE cpf = p_cpf
       ) THEN

        RAISE EXCEPTION
            'O CPF informado já está vinculado a outra conta.';
    END IF;


    ----------------------------------------------------------------
    -- VERIFICAÇÃO DE CNPJ
    ----------------------------------------------------------------

    IF p_cnpj IS NOT NULL
       AND trim(p_cnpj) <> ''
       AND EXISTS (
           SELECT 1
           FROM usuarios
           WHERE cnpj = p_cnpj
       ) THEN

        RAISE EXCEPTION
            'O CNPJ informado já está vinculado a outra conta.';
    END IF;


    ----------------------------------------------------------------
    -- VALIDAÇÕES ESPECÍFICAS DE ARTISTA
    ----------------------------------------------------------------

    IF v_tipo_text = 'ARTISTA' THEN

        IF p_cpf IS NULL OR trim(p_cpf) = '' THEN
            RAISE EXCEPTION
                'CPF é obrigatório para cadastros do tipo ARTISTA.';
        END IF;


        IF p_tipo_perfil_artista IS NULL THEN
            RAISE EXCEPTION
                'Subtipo de perfil artístico é obrigatório.';
        END IF;


        IF p_tipo_perfil_artista IN (
            'ESTUDIO',
            'PRODUTORA_EMPRESA'
        ) THEN

            IF p_cnpj IS NULL OR trim(p_cnpj) = '' THEN
                RAISE EXCEPTION
                    'CNPJ é obrigatório para perfis do tipo Estúdio ou Produtora/Empresa Artística.';
            END IF;

            IF v_idade < 18 THEN
                RAISE EXCEPTION
                    'O responsável por perfis de Estúdio ou Produtora/Empresa Artística deve ter no mínimo 18 anos.';
            END IF;

        END IF;

        IF p_area_principal_id IS NULL
           OR NOT EXISTS (
               SELECT 1
               FROM areas_artisticas
               WHERE id = p_area_principal_id
           ) THEN

            RAISE EXCEPTION
                'Uma área artística principal válida deve ser selecionada.';
        END IF;

        IF v_idade BETWEEN 14 AND 17 THEN

            IF p_nome_resp IS NULL
               OR trim(p_nome_resp) = ''
               OR p_tel_resp IS NULL
               OR trim(p_tel_resp) = ''
               OR p_email_resp IS NULL
               OR trim(p_email_resp) = '' THEN

                RAISE EXCEPTION
                    'Artistas entre 14 e 17 anos devem informar nome, telefone e e-mail do responsável legal.';
            END IF;

        END IF;

    END IF;

    IF v_tipo_text = 'CONTRATANTE' THEN

        IF p_tipo_contratante IS NULL THEN
            RAISE EXCEPTION
                'Subtipo de contratante é obrigatório.';
        END IF;

        IF p_tipo_contratante = 'PESSOA_FISICA' THEN

            IF p_cpf IS NULL OR trim(p_cpf) = '' THEN
                RAISE EXCEPTION
                    'CPF é obrigatório para Contratante Pessoa Física.';
            END IF;

        ELSE

            IF p_cnpj IS NULL OR trim(p_cnpj) = '' THEN
                RAISE EXCEPTION
                    'CNPJ é obrigatório para Contratantes Pessoa Jurídica / Entidades.';
            END IF;

            IF p_nome_empresa IS NULL
               OR trim(p_nome_empresa) = '' THEN

                RAISE EXCEPTION
                    'Nome da empresa/entidade é obrigatório para Contratantes Pessoa Jurídica / Entidades.';
            END IF;

        END IF;

    END IF;

    IF p_senha IS NULL OR trim(p_senha) = '' THEN
        RAISE EXCEPTION
            'Senha é obrigatória para cadastro por senha.';
    END IF;

    INSERT INTO usuarios (
        nome,
        username,
        data_nascimento,
        telefone,
        email,
        senha,
        tipo_usuario,
        cpf,
        cnpj,
        foto_perfil_url,
        status_conta,
        perfil_completo
    )
    VALUES (
        trim(p_nome),
        trim(p_username),
        p_data_nascimento,
        trim(p_telefone),
        lower(trim(p_email)),
        p_senha,
        v_tipo_text::tipo_usuario_enum,
        NULLIF(trim(p_cpf), ''),
        NULLIF(trim(p_cnpj), ''),
        p_foto_perfil_url,
        'PENDENTE_VERIFICACAO_EMAIL',
        false
    )
    RETURNING id INTO p_usuario_id;

    IF v_tipo_text = 'ARTISTA' THEN

        INSERT INTO perfis_artistas (
            usuario_id,
            tipo_perfil_artistico
        )
        VALUES (
            p_usuario_id,
            p_tipo_perfil_artista
        );


        INSERT INTO perfil_artista_area (
            perfil_artista_id,
            area_id,
            principal
        )
        VALUES (
            p_usuario_id,
            p_area_principal_id,
            true
        );

        IF v_idade BETWEEN 14 AND 17 THEN

            INSERT INTO responsaveis_legais (
                usuario_id,
                nome_responsavel,
                telefone_responsavel,
                email_responsavel
            )
            VALUES (
                p_usuario_id,
                trim(p_nome_resp),
                trim(p_tel_resp),
                lower(trim(p_email_resp))
            );

        END IF;

    END IF;

    IF v_tipo_text = 'CONTRATANTE' THEN

        INSERT INTO perfis_contratantes (
            usuario_id,
            tipo_contratante,
            nome_empresa
        )
        VALUES (
            p_usuario_id,
            p_tipo_contratante,
            CASE
                WHEN p_tipo_contratante = 'PESSOA_FISICA'
                    THEN NULL
                ELSE trim(p_nome_empresa)
            END
        );

    END IF;
    
    PERFORM fn_verificar_perfil_completo(p_usuario_id);
END;
$$;
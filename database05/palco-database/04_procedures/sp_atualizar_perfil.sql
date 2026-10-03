create or replace procedure sp_atualizar_perfil(
    p_usuario_id bigint,
 
    p_nome varchar default null,
    p_telefone varchar default null,
    p_foto_perfil varchar default null,
 
    -- A aplicação Java já validou a senha atual via
    -- BCryptPasswordEncoder.matches() ANTES de chamar este procedure.
    -- Ele só recebe o hash NOVO, nunca compara hash com hash aqui
    -- (comparar dois hashes BCrypt com <> nunca daria certo, já que
    -- cada hash tem um salt aleatório próprio).
    p_senha_nova varchar default null,
 
    p_bio text default null,
    p_cidade varchar default null,
    p_estado varchar default null,
 
    p_url_portfolio varchar default null,
    p_disponivel_oportunidades boolean default null,
    p_raio_atuacao abrangencia_enum default null,
 
    p_nome_empresa varchar default null,
 
    -- área que vira a principal (opcional, só quando o usuário está
    -- de fato trocando qual é a principal)
    p_area_principal_id smallint default null,
    p_nivel_experiencia_principal nivel_experiencia_enum default null,
 
    -- área cujas função/especializações estão sendo editadas nesta
    -- chamada — pode ser a mesma de p_area_principal_id ou uma
    -- área secundária qualquer que o artista já tenha
    p_area_alvo_taxonomia_id smallint default null,
    p_funcoes_ids bigint[] default null,
    p_especializacoes_ids bigint[] default null,
 
    -- Tópico 7: autodeclarações pelo enum fechado, não mais por ID
    -- de uma tabela-catálogo compartilhada com as vagas
    p_autodeclaracoes categoria_afirmativa_enum[] default null
)
language plpgsql as $$
declare
    v_tipo_text text;
    v_funcao_id bigint;
    v_esp_id bigint;
    v_categoria categoria_afirmativa_enum;
begin
    select upper(tipo_usuario::text) into v_tipo_text
    from usuarios where id = p_usuario_id;
 
    if v_tipo_text is null then
        raise exception 'Usuário não encontrado (ID: %)', p_usuario_id;
    end if;
 
    if p_senha_nova is not null then
        update usuarios set senha  = p_senha_nova  where id = p_usuario_id;
    end if;
 
    update usuarios
    set nome = coalesce(p_nome, nome),
        telefone = coalesce(p_telefone, telefone),
        foto_perfil = coalesce(p_foto_perfil, foto_perfil)
    where id = p_usuario_id;
 
    if v_tipo_text = 'ARTISTA' then
        update perfis_artistas
        set biografia = coalesce(p_bio, biografia),
            cidade = coalesce(p_cidade, cidade),
            estado = coalesce(p_estado, estado),
            url_portfolio = coalesce(p_url_portfolio, url_portfolio),
            disponivel_oportunidades = coalesce(p_disponivel_oportunidades, disponivel_oportunidades),
            raio_atuacao = coalesce(p_raio_atuacao, raio_atuacao),
            ultima_atualizacao = current_timestamp
        where usuario_id = p_usuario_id;
 
        -- troca de área principal
        if p_area_principal_id is not null then
            update perfil_artista_area
            set principal = false
            where perfil_artista_id = p_usuario_id;
 
            insert into perfil_artista_area (perfil_artista_id, area_id, principal, nivel_experiencia)
            values (p_usuario_id, p_area_principal_id, true, p_nivel_experiencia_principal)
            on conflict (perfil_artista_id, area_id)
            do update set principal = true,
                          nivel_experiencia = coalesce(p_nivel_experiencia_principal, perfil_artista_area.nivel_experiencia);
        end if;
 
        -- funções/especializações de QUALQUER área que o artista já
        -- tenha (não fica mais preso a "só dá pra editar se também
        -- for a principal nesta mesma chamada")
        if p_funcoes_ids is not null and p_area_alvo_taxonomia_id is not null then
            delete from perfil_artista_funcao
            where perfil_artista_id = p_usuario_id and area_id = p_area_alvo_taxonomia_id;
 
            foreach v_funcao_id in array p_funcoes_ids loop
                insert into perfil_artista_funcao (perfil_artista_id, area_id, funcao_id)
                values (p_usuario_id, p_area_alvo_taxonomia_id, v_funcao_id)
                on conflict do nothing;
            end loop;
        end if;
 
        if p_especializacoes_ids is not null and p_area_alvo_taxonomia_id is not null then
            delete from perfil_artista_especializacao
            where perfil_artista_id = p_usuario_id and area_id = p_area_alvo_taxonomia_id;
 
            foreach v_esp_id in array p_especializacoes_ids loop
                insert into perfil_artista_especializacao (perfil_artista_id, area_id, especializacao_id)
                values (p_usuario_id, p_area_alvo_taxonomia_id, v_esp_id)
                on conflict do nothing;
            end loop;
        end if;
 
        -- Tópico 7: autodeclarações via autodeclaracoes + enum fechado
        if p_autodeclaracoes is not null then
            delete from autodeclaracoes where usuario_id = p_usuario_id;
 
            foreach v_categoria in array p_autodeclaracoes loop
                insert into autodeclaracoes (usuario_id, categoria, exibicao_publica)
                values (p_usuario_id, v_categoria, false) -- exposição pública é escolha separada (RF08), começa fechada
                on conflict do nothing;
            end loop;
        end if;
 
    elsif v_tipo_text = 'CONTRATANTE' then
        update perfis_contratantes
        set biografia = coalesce(p_bio, biografia),
            cidade = coalesce(p_cidade, cidade),
            estado = coalesce(p_estado, estado),
            nome_empresa = coalesce(p_nome_empresa, nome_empresa)
        where usuario_id = p_usuario_id;
    end if;
 
    perform fn_verificar_perfil_completo(p_usuario_id);
end;
$$;
 
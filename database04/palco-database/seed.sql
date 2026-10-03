BEGIN;

-- =========================================================================
-- 1. CONTA RESERVADA DE SISTEMA (USUÁRIO FANTASMA - ID 0)
-- DOCUMENTAÇÃO LGPD (RF22)
-- =========================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM usuarios
        WHERE id = 0
    ) THEN

        INSERT INTO usuarios (
            id,
            nome,
            username,
            data_nascimento,
            telefone,
            email,
            senha,
            tipo_usuario,
            perfil_completo,
            status_conta,
            cpf,
            cnpj
        )
        VALUES (
            0,
            'Usuário Removido',
            'usuario.removido',
            '2000-01-01',
            '00000000000',
            'anonimo@sosartistas.local',
            '$2a$12$GhostUserDummyPasswordHash123456789012345',
            'ARTISTA'::tipo_usuario_enum,
            false,
            'ATIVA'::status_conta_enum,
            '00000000000',
            NULL
        );

        INSERT INTO perfis_artistas (
            usuario_id,
            biografia,
            tipo_perfil_artistico
        )
        VALUES (
            0,
            'Perfil mantido anonimamente para preservação de histórico do sistema (RF22).',
            'ARTISTA_SOLO'
        );

        INSERT INTO perfis_contratantes (
            usuario_id,
            nome_empresa,
            tipo_contratante
        )
        VALUES (
            0,
            'Entidade Removida',
            'PESSOA_FISICA'::tipo_contratante_enum
        );

    END IF;
END $$;
-- =========================================================================
-- 2. CATÁLOGO OFICIAL DE TAXONOMIA (7 ÁREAS DO RF01 + FUNÇÕES + ESPECIALIZAÇÕES)
-- =========================================================================
INSERT INTO areas_artisticas (id, nome) VALUES
(1, 'Artes Cênicas'),
(2, 'Música'),
(3, 'Dança'),
(4, 'Artes Visuais'),
(5, 'Audiovisual'),
(6, 'Arte e Tecnologia'),
(7, 'Artes Literárias')
ON CONFLICT (id) DO UPDATE SET nome = EXCLUDED.nome;

INSERT INTO funcoes (id, area_id, nome) VALUES
(1, 4, 'Pintor / Muralista'),     -- Artes Visuais
(2, 4, 'Ilustrador Digital'),    -- Artes Visuais
(3, 2, 'Compositor'),            -- Música
(4, 2, 'Instrumentista')         -- Música
ON CONFLICT (id) DO UPDATE SET nome = EXCLUDED.nome;

INSERT INTO especializacoes (id, nome) VALUES
(1, 'Aparelhagem e Tintas'),
(2, 'Vetor e Concept Art'),
(3, 'Trilhas Sonoras'),
(4, 'Violão Cordas de Aço')
ON CONFLICT (id) DO UPDATE SET nome = EXCLUDED.nome;

INSERT INTO funcao_especializacao (funcao_id, especializacao_id) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4)
ON CONFLICT DO NOTHING;

-- =========================================================================
-- 3. USUÁRIOS E RESPONSÁVEIS LEGAIS (RF01 / RF02 / RF06)
-- =========================================================================

INSERT INTO usuarios (
    id,
    nome,
    username,
    data_nascimento,
    telefone,
    email,
    senha,
    tipo_usuario,
    perfil_completo,
    status_conta,
    cpf,
    cnpj,
    email_verificado
)
VALUES

-- ========================================================================
-- ARTISTAS
-- ========================================================================

(
    1,
    'Ana Silva',
    'ana.silva',
    '2002-05-10',
    '11999991111',
    'ana@email.com',
    '$2a$12$e0MYzXyjpJS7Pd0RVvHwHeF5aXgM88qO88fJqfH8Jm6wM.wVfM2K2',
    'ARTISTA'::tipo_usuario_enum,
    true,
    'ATIVA'::status_conta_enum,
    '11122233344',
    NULL,
    true
),

(
    2,
    'Bruno Souza',
    'bruno.souza',
    '2001-08-15',
    '11999992222',
    'bruno@email.com',
    '$2a$12$e0MYzXyjpJS7Pd0RVvHwHeF5aXgM88qO88fJqfH8Jm6wM.wVfM2K2',
    'ARTISTA'::tipo_usuario_enum,
    true,
    'ATIVA'::status_conta_enum,
    '22233344455',
    NULL,
    true
),

(
    3,
    'Carla Dias',
    'carla.dias',
    '2009-02-20',
    '11999993333',
    'carla@email.com',
    '$2a$12$e0MYzXyjpJS7Pd0RVvHwHeF5aXgM88qO88fJqfH8Jm6wM.wVfM2K2',
    'ARTISTA'::tipo_usuario_enum,
    true,
    'ATIVA'::status_conta_enum,
    '33344455566',
    NULL,
    true
),

-- ========================================================================
-- CONTRATANTES
-- ========================================================================

(
    4,
    'Alpha Cultural LTDA',
    'alpha.cultural',
    '1985-03-12',
    '11888884444',
    'alpha@email.com',
    '$2a$12$e0MYzXyjpJS7Pd0RVvHwHeF5aXgM88qO88fJqfH8Jm6wM.wVfM2K2',
    'CONTRATANTE'::tipo_usuario_enum,
    true,
    'ATIVA'::status_conta_enum,
    NULL,
    '12345678000190',
    true
),

(
    5,
    'Beta Studio Mídias',
    'beta.studio',
    '1990-07-22',
    '11888885555',
    'beta@email.com',
    '$2a$12$e0MYzXyjpJS7Pd0RVvHwHeF5aXgM88qO88fJqfH8Jm6wM.wVfM2K2',
    'CONTRATANTE'::tipo_usuario_enum,
    true,
    'ATIVA'::status_conta_enum,
    NULL,
    '98765432000110',
    true
),

(
    6,
    'Gama Associação Cultural',
    'gama.associacao',
    '1982-11-30',
    '11888886666',
    'gama@email.com',
    '$2a$12$e0MYzXyjpJS7Pd0RVvHwHeF5aXgM88qO88fJqfH8Jm6wM.wVfM2K2',
    'CONTRATANTE'::tipo_usuario_enum,
    true,
    'ATIVA'::status_conta_enum,
    NULL,
    '45678912000130',
    true
)

ON CONFLICT (id) DO NOTHING;
-- =========================================================================
-- 4. PERFIS DE ARTISTA E TAXONOMIA VINCULADA (RF08)
-- =========================================================================
INSERT INTO perfis_artistas (
usuario_id,
biografia,
cidade,
estado,
url_portfolio,
tipo_perfil_artistico,
disponivel_oportunidades,
raio_atuacao,
nome_integrantes,
banner_url
)
VALUES
-- Exemplo de inserção corrigida (remova os dados de medalha e score que estavam sobrando)
(1, 'Guitarrista solo buscando oportunidades...', 'São Paulo', 'SP', 'https://...', 'ARTISTA_SOLO', true, 'MUNICIPAL', null, 'https://...'),
(2, 'Banda de rock alternativo', 'Campinas', 'SP', 'https://...', 'BANDA', true, 'NACIONAL', 'João, Marcos, Ana', 'https://...'),
(3, 'Produtora cultural independente', 'Rio de Janeiro', 'RJ', 'https://...', 'GRUPO_ARTISTICO', false, 'NACIONAL', null, null)
ON CONFLICT (usuario_id) DO NOTHING;

-- Vínculos de Área Principal
INSERT INTO perfil_artista_area (perfil_artista_id, area_id, principal, nivel_experiencia) VALUES
(1, 4, true, 'INTERMEDIARIO'::nivel_experiencia_enum), -- Artes Visuais
(2, 4, true, 'ESPECIALISTA'::nivel_experiencia_enum),   -- Artes Visuais
(3, 2, true, 'INICIANTE'::nivel_experiencia_enum)      -- Música
ON CONFLICT (perfil_artista_id, area_id) DO NOTHING;

INSERT INTO perfil_artista_funcao (perfil_artista_id, area_id, funcao_id) VALUES
(1, 4, 1),
(2, 4, 2),
(3, 2, 3)
ON CONFLICT DO NOTHING;

INSERT INTO perfil_artista_especializacao (perfil_artista_id, area_id, especializacao_id) VALUES
(1, 4, 1),
(2, 4, 2),
(3, 2, 3)
ON CONFLICT DO NOTHING;

INSERT INTO autodeclaracoes (usuario_id, categoria, exibicao_publica) VALUES
(1, 'MULHER', true),
(2, 'PCD', false)
ON CONFLICT DO NOTHING;

-- =========================================================================
-- 5. PERFIS DE CONTRATANTE (SUBTIPOS OFICIAIS RF01)
-- =========================================================================
INSERT INTO perfis_contratantes (usuario_id, nome_empresa, tipo_contratante, biografia, cidade, estado) VALUES
(4, 'Alpha Cultural LTDA', 'SETOR_PRIVADO'::tipo_contratante_enum, 'Fomento a projetos culturais e urbanos', 'São Paulo', 'SP'),
(5, 'Beta Studio Mídias', 'SETOR_PRIVADO'::tipo_contratante_enum, 'Produtora de conteúdo audiovisual e mídias virtuais', 'Rio de Janeiro', 'RJ'),
(6, 'Gama Associação Cultural', 'ONG'::tipo_contratante_enum, 'Organização não governamental voltada a editais públicos', 'Belo Horizonte', 'MG')
ON CONFLICT (usuario_id) DO NOTHING;

-- =========================================================================
-- 6. VAGAS E TAXONOMIA ASSOCIADA (RF03 / RF04)
-- =========================================================================
INSERT INTO vagas (
id, contratante_id, area_id, titulo, descricao, requisitos,
forma_remuneracao, valor_minimo, valor_maximo, cidade, estado,
endereco_completo, beneficios, modelo_trabalho, tipo_contrato,
experiencia, data_limite_candidatura, abrangencia, status,
data_publicacao, ultima_atualizacao
) VALUES
(
1, 4, 4, 'Pintura de Mural Urbano',
'Criar e executar mural artístico em fachada comercial de grande porte.',
'Experiência prévia em pintura externa e manuseio de tintas acrílicas.',
'A_COMBINAR'::forma_remuneracao_enum, 2000.00, 3000.00, 'São Paulo', 'SP',
'Av. Paulista, 1000 - Bela Vista', 'Alimentação no local e material incluso',
'PRESENCIAL'::modelo_trabalho_enum, 'PJ',
'Pleno (2+ anos)', '2026-10-01', 'MUNICIPAL'::abrangencia_enum,
'ABERTA'::status_vaga_enum, current_timestamp, current_timestamp
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO vaga_funcao (vaga_id, funcao_id) VALUES (1, 1) ON CONFLICT DO NOTHING;
INSERT INTO vaga_especializacao (vaga_id, especializacao_id) VALUES (1, 1) ON CONFLICT DO NOTHING;

INSERT INTO fotos_vaga (vaga_id, ordem, url) VALUES
(1, 1, 'https://img.com/vaga1.png')
ON CONFLICT DO NOTHING;

-- =========================================================================
-- 7. CANDIDATURAS E INTERAÇÕES DO SISTEMA (PORTFÓLIO & EMBEDS ALINHADOS)
-- =========================================================================
INSERT INTO candidaturas (vaga_id, artista_id, mensagem_apresentacao, link_portfolio_candidatura, status, data_candidatura) VALUES
(1, 1, 'Tenho grande interesse na pintura deste mural.', 'https://portfolio.com/ana', 'PENDENTE'::status_candidatura_enum, current_timestamp)
ON CONFLICT DO NOTHING;

INSERT INTO log_vagas_canceladas (vaga_id, cancelado_por_id, motivo) VALUES
(1, 4, 'Reestruturação de orçamento do projeto')
ON CONFLICT DO NOTHING;

INSERT INTO portfolio_arquivos (id, artista_id, url_arquivo, nome_original, tamanho_bytes, tipo_mime) VALUES
(1, 1, 'https://img.com/obra1.png', 'mural_obra1.png', 1048576, 'image/png')
ON CONFLICT DO NOTHING;

-- Inserções com semântica correta (tipo_midia derivado automaticamente pela trigger do banco)
INSERT INTO embeds_externos (artista_id, plataforma, url_original, codigo_iframe, legenda) VALUES
-- Spotify -> Deriva tipo_midia = 'AUDIO'
(3, 'SPOTIFY'::plataforma_embed_enum, 'https://open.spotify.com/track/1234567890', '<iframe src="https://open.spotify.com/embed/track/1234567890"></iframe>', 'Demonstração de composição autoral'),
-- YouTube -> Deriva tipo_midia = 'VIDEO'
(3, 'YOUTUBE'::plataforma_embed_enum, 'https://youtube.com/watch?v=1', '<iframe src="https://youtube.com/embed/1"></iframe>', 'Apresentação ao vivo da faixa autoral')
ON CONFLICT DO NOTHING;

-- =========================================================================
-- 8. COMUNIDADES, EDITAIS E GALERIAS
-- =========================================================================
INSERT INTO comunidades (id, criador_id, nome, descricao, categoria_artistica, privacidade) VALUES
(1, 1, 'Artistas Unidos de SP', 'Comunidade de troca de experiências em artes visuais', 'Pintura', 'PUBLICA')
ON CONFLICT (id) DO NOTHING;

INSERT INTO membros_comunidade (comunidade_id, usuario_id, papel) VALUES
(1, 1, 'ADMIN'),
(1, 2, 'MEMBRO')
ON CONFLICT DO NOTHING;

INSERT INTO editais (id, comunidade_id, publicador_id, titulo, descricao, url_arquivo_oficial, data_inicio_inscricao, data_fim_inscricao, data_resultado) VALUES
(1, 1, 4, 'Edital de Fomento das Artes 2026', 'Apoio a projetos de intervenção urbana', 'https://edital.pdf', '2026-03-01', '2026-04-01', '2026-04-15')
ON CONFLICT (id) DO NOTHING;

INSERT INTO retificacoes_edital (edital_id, titulo_retificacao, descricao_alteracoes, url_arquivo_aditivo) VALUES
(1, 'Retificação 01 - Ajuste de Cronograma', 'Prorrogação das inscrições por mais 5 dias', 'https://retif01.pdf')
ON CONFLICT DO NOTHING;

INSERT INTO galerias_virtuais (id, dono_id, comunidade_id, titulo, descricao, categoria, tipo_galeria, status) VALUES
(1, 1, 1, 'Mostra Virtual de Murais', 'Exposição de obras urbanas paulistas', 'Pintura', 'COMUNITARIA', 'ATIVA')
ON CONFLICT DO NOTHING;

INSERT INTO itens_galeria (galeria_id, arquivo_id) VALUES (1, 1) ON CONFLICT DO NOTHING;
INSERT INTO interacoes_galeria (usuario_id, arquivo_id, curtiu, comentario) VALUES (2, 1, true, 'Excelente trabalho com as cores!') ON CONFLICT DO NOTHING;

-- =========================================================================
-- 9. COMUNICAÇÃO, MODERAÇÃO E LOGS LGPD (RF09 / RF22)
-- =========================================================================
INSERT INTO notificacoes (usuario_destino_id, tipo_notificacao, mensagem_alerta, link_contexto) VALUES
(1, 'CANDIDATURA', 'Você recebeu uma atualização na sua candidatura.', '/candidaturas/1');

INSERT INTO salas_chat (id) VALUES (1) ON CONFLICT (id) DO NOTHING;
INSERT INTO participantes_chat (sala_id, usuario_id) VALUES (1, 1), (1, 4) ON CONFLICT DO NOTHING;
INSERT INTO mensagens_chat (sala_id, remetente_id, texto_mensagem) VALUES (1, 4, 'Olá Ana, recebemos seu portfólio!');

INSERT INTO denuncias_plagio (denunciante_id, perfil_denunciado_id, tipo_violacao, descricao_detalhada) VALUES
(1, 2, 'OUTRO', 'Suspeita de uso não autorizado de arte conceitual.');

INSERT INTO moderacao_conteudo (tipo_conteudo, conteudo_id, autor_id, status_moderacao) VALUES
('GALERIA'::tipo_conteudo_enum, 1, 1, 'APROVADO');

INSERT INTO reportes_usuario (denunciante_id, tipo_conteudo, conteudo_id, motivo_reporte) VALUES
(2, 'GALERIA'::tipo_conteudo_enum, 1, 'Conteúdo em desacordo com as regras da comunidade');

INSERT INTO itens_salvos (usuario_id, tipo_alvo, alvo_id) VALUES (1, 'VAGA', 1) ON CONFLICT DO NOTHING;

INSERT INTO log_exclusoes_lgpd (motivo_opcional, comprovante_hash) VALUES
('Solicitação voluntária do usuário via painel de privacidade', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855');

-- =========================================================================
-- 10. REAJUSTE DE SEQUÊNCIAS DO POSTGRESQL (PREVINE CONFLITO DE PRIMARY KEY)
-- =========================================================================
SELECT setval('usuarios_id_seq', coalesce((SELECT max(id) FROM usuarios), 1), true);
SELECT setval('funcoes_id_seq', coalesce((SELECT max(id) FROM funcoes), 1), true);
SELECT setval('especializacoes_id_seq', coalesce((SELECT max(id) FROM especializacoes), 1), true);
SELECT setval('vagas_id_seq', coalesce((SELECT max(id) FROM vagas), 1), true);
SELECT setval('portfolio_arquivos_id_seq', coalesce((SELECT max(id) FROM portfolio_arquivos), 1), true);
SELECT setval('embeds_externos_id_seq', coalesce((SELECT max(id) FROM embeds_externos), 1), true);
SELECT setval('comunidades_id_seq', coalesce((SELECT max(id) FROM comunidades), 1), true);
SELECT setval('editais_id_seq', coalesce((SELECT max(id) FROM editais), 1), true);
SELECT setval('galerias_virtuais_id_seq', coalesce((SELECT max(id) FROM galerias_virtuais), 1), true);
SELECT setval('salas_chat_id_seq', coalesce((SELECT max(id) FROM salas_chat), 1), true);

COMMIT;
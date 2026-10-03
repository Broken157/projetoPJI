CREATE OR REPLACE FUNCTION fn_trg_derivar_tipo_midia_embed()
RETURNS trigger AS $$
BEGIN
    -- Mapeamento estrito e infalível por plataforma
    IF NEW.plataforma IN ('YOUTUBE', 'VIMEO') THEN
        NEW.tipo_midia := 'VIDEO'::tipo_midia_enum;
    ELSIF NEW.plataforma IN ('SPOTIFY', 'SOUNDCLOUD') THEN
        NEW.tipo_midia := 'AUDIO'::tipo_midia_enum;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- O gatilho é criado e configurado aqui fora:
DROP TRIGGER IF EXISTS trg_derivar_tipo_midia_embed ON embeds_externos;

CREATE TRIGGER trg_derivar_tipo_midia_embed
BEFORE INSERT OR UPDATE ON embeds_externos
FOR EACH ROW EXECUTE FUNCTION fn_trg_derivar_tipo_midia_embed();
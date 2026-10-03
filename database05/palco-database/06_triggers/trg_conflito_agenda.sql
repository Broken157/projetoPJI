CREATE OR REPLACE FUNCTION fn_verificar_conflito_agenda()
RETURNS trigger AS $$
BEGIN
    -- RF33: data_hora_fim deve ser posterior a data_hora_inicio
    IF NEW.data_hora_fim <= NEW.data_hora_inicio THEN
        RAISE EXCEPTION 'A data/hora de término deve ser posterior à data/hora de início.'
            USING ERRCODE = '22000';
    END IF;

    -- RF33: nenhuma sobreposição de intervalo é permitida para o mesmo artista
    IF EXISTS (
        SELECT 1
        FROM agenda_artista
        WHERE artista_id = NEW.artista_id
          AND id <> COALESCE(NEW.id, -1)
          AND tsrange(data_hora_inicio, data_hora_fim, '[)') &&
              tsrange(NEW.data_hora_inicio, NEW.data_hora_fim, '[)')
    ) THEN
        -- CORREÇÃO AQUI: Adicionado o '%' logo após a palavra "artista"
        RAISE EXCEPTION 'Conflito de horário com compromisso existente do artista %', NEW.artista_id
            USING ERRCODE = '22000';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_conflito_agenda ON agenda_artista;
CREATE TRIGGER trg_conflito_agenda
BEFORE INSERT OR UPDATE ON agenda_artista
FOR EACH ROW EXECUTE FUNCTION fn_verificar_conflito_agenda();
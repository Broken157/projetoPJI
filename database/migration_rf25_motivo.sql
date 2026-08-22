ALTER TABLE log_vagas_canceladas
    ADD COLUMN IF NOT EXISTS motivo text;

package com.portifolio.model.converter;

import com.portifolio.model.enums.TipoNotificacao;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TipoNotificacaoConverter implements AttributeConverter<TipoNotificacao, String> {

    @Override
    public String convertToDatabaseColumn(TipoNotificacao attribute) {
        return attribute == null ? null : attribute.getDatabaseValue();
    }

    @Override
    public TipoNotificacao convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TipoNotificacao.fromDatabaseValue(dbData);
    }
}

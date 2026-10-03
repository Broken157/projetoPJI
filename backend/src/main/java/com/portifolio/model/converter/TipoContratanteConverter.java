package com.portifolio.model.converter;

import com.portifolio.model.enums.TipoContratante;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TipoContratanteConverter implements AttributeConverter<TipoContratante, String> {
    @Override public String convertToDatabaseColumn(TipoContratante value) {
        return value == null ? null : value.getDatabaseValue();
    }
    @Override public TipoContratante convertToEntityAttribute(String value) {
        return value == null ? null : TipoContratante.valueOf(value);
    }
}

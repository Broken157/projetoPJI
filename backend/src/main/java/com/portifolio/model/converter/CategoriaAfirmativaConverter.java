package com.portifolio.model.converter;

import com.portifolio.model.CategoriaAfirmativa;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CategoriaAfirmativaConverter implements AttributeConverter<CategoriaAfirmativa, String> {
    @Override public String convertToDatabaseColumn(CategoriaAfirmativa value) {
        return value == null ? null : value.getDatabaseValue();
    }
    @Override public CategoriaAfirmativa convertToEntityAttribute(String value) {
        return value == null ? null : CategoriaAfirmativa.valueOf(value);
    }
}

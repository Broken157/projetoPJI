package com.portifolio.model.converter;
import jakarta.persistence.AttributeConverter;
import com.portifolio.model.enums.*;
import java.util.Locale;
/** Traduções semânticas dos enums que já existem no banco instalado. */
public final class OfficialLocalConverters {
    public static class UsuarioTipo implements AttributeConverter<TipoUsuario,String> {
        public String convertToDatabaseColumn(TipoUsuario v){return v==null?null:v.name().toLowerCase(Locale.ROOT);}
        public TipoUsuario convertToEntityAttribute(String v){return v==null?null:TipoUsuario.valueOf(v.toUpperCase(Locale.ROOT));}
    }
    public static class VagaStatus implements AttributeConverter<StatusVaga,String> {
        public String convertToDatabaseColumn(StatusVaga v){return v==null?null:v.name().toLowerCase(Locale.ROOT);}
        public StatusVaga convertToEntityAttribute(String v){return v==null?null:StatusVaga.valueOf(v.toUpperCase(Locale.ROOT));}
    }
    public static class Trabalho implements AttributeConverter<ModeloTrabalho,String> {
        public String convertToDatabaseColumn(ModeloTrabalho v){return v==null?null:v.name().toLowerCase(Locale.ROOT);}
        public ModeloTrabalho convertToEntityAttribute(String v){return v==null?null:ModeloTrabalho.valueOf(v.toUpperCase(Locale.ROOT));}
    }
    public static class CandidaturaStatus implements AttributeConverter<StatusCandidatura,String> {
        public String convertToDatabaseColumn(StatusCandidatura v){if(v==null)return null;return switch(v){case ACEITA->"aprovado";case REJEITADA->"rejeitado";case EM_ANALISE->"em analise";default->v.name().toLowerCase(Locale.ROOT);};}
        public StatusCandidatura convertToEntityAttribute(String v){if(v==null)return null;return switch(v){case "aprovado"->StatusCandidatura.ACEITA;case "rejeitado"->StatusCandidatura.REJEITADA;case "em analise"->StatusCandidatura.EM_ANALISE;default->StatusCandidatura.valueOf(v.toUpperCase(Locale.ROOT));};}
    }
    public static class SalvoTipo implements AttributeConverter<TipoAlvoSalvo,String> {
        public String convertToDatabaseColumn(TipoAlvoSalvo v){return v==null?null:v==TipoAlvoSalvo.PERFIL_ARTISTA?"artista":v.name().toLowerCase(Locale.ROOT);}
        public TipoAlvoSalvo convertToEntityAttribute(String v){return v==null?null:v.equals("artista")?TipoAlvoSalvo.PERFIL_ARTISTA:TipoAlvoSalvo.valueOf(v.toUpperCase(Locale.ROOT));}
    }
    public static class NotificacaoTipo implements AttributeConverter<TipoNotificacao,String> {
        public String convertToDatabaseColumn(TipoNotificacao v){return v==null?null:v.name().toLowerCase(Locale.ROOT);}
        public TipoNotificacao convertToEntityAttribute(String v){return v==null?null:TipoNotificacao.valueOf(v.toUpperCase(Locale.ROOT));}
    }
}

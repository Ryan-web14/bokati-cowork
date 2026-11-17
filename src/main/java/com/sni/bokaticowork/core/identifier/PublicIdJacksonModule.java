package com.sni.bokaticowork.core.identifier;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Identifiants lisibles dans l'API JSON (17/11/2026), enregistre par Spring Boot sur le
 * {@code JsonMapper} HTTP.
 *
 * <ul>
 *   <li>Sortie : un {@code Long} nomme {@code id} ou terminant par {@code Id} / {@code Ids} est
 *   rendu en code public ({@link PublicId}) ; tout autre {@code Long} trop grand pour un navigateur
 *   aussi. Les autres nombres (compteurs, tailles...) restent des nombres.</li>
 *   <li>Entree : un {@code Long} accepte le code public, le nombre en texte ou le nombre JSON.</li>
 * </ul>
 */
@Component
public class PublicIdJacksonModule extends SimpleModule {

    public PublicIdJacksonModule() {
        super("bokati-public-id");
        addSerializer(Long.class, new IdSerializer(null, false));
        addSerializer(Long.TYPE, new IdSerializer(null, false));
        addDeserializer(Long.class, new IdDeserializer(Long.class));
        addDeserializer(Long.TYPE, new IdDeserializer(Long.TYPE));
    }

    static boolean idLike(String name) {
        return name != null && (name.equals("id") || name.endsWith("Id") || name.endsWith("Ids"));
    }

    static final class IdSerializer extends StdSerializer<Long> {

        private final String prefix;
        private final boolean always;

        IdSerializer(String prefix, boolean always) {
            super(Long.class);
            this.prefix = prefix;
            this.always = always;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
            if (property == null) {
                return this;
            }
            String name = property.getName();
            if ("id".equals(name)) {
                Class<?> owner = property.getMember() == null ? null : property.getMember().getDeclaringClass();
                return new IdSerializer(PublicId.prefixFor(owner == null ? null : owner.getSimpleName()), true);
            }
            return new IdSerializer(PublicId.prefixFor(name), idLike(name));
        }

        @Override
        public void serialize(Long value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
            long v = value;
            if (always || v > PublicId.MAX_SAFE_INTEGER || v < -PublicId.MAX_SAFE_INTEGER) {
                gen.writeString(PublicId.encode(prefix == null ? "ID" : prefix, v));
            } else {
                gen.writeNumber(v);
            }
        }
    }

    static final class IdDeserializer extends StdScalarDeserializer<Long> {

        IdDeserializer(Class<?> type) {
            super(type);
        }

        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            JsonToken token = p.currentToken();
            if (token == JsonToken.VALUE_NUMBER_INT) {
                return p.getLongValue();
            }
            if (token == JsonToken.VALUE_STRING) {
                String text = p.getString();
                if (text == null || text.isBlank()) {
                    return null;
                }
                try {
                    return PublicId.decode(text);
                } catch (IllegalArgumentException ex) {
                    return (Long) ctxt.handleWeirdStringValue(Long.class, text, "identifiant ou nombre attendu");
                }
            }
            if (token == JsonToken.VALUE_NUMBER_FLOAT) {
                return (long) p.getDoubleValue();
            }
            return (Long) ctxt.handleUnexpectedToken(Long.class, p);
        }
    }
}

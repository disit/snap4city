package edu.unifi.disit.datamanager.datamodel;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.data.domain.Sort;

import java.io.IOException;

@JsonComponent
public class SortJsonSerializer extends JsonSerializer<Sort> {
	
	private static final Logger logger = LogManager.getLogger();

    @Override
    public void serialize(Sort value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartArray();

        value.iterator().forEachRemaining(v -> {
            try {
                gen.writeObject(v);
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
            }
        });

        gen.writeEndArray();
    }

    @Override
    public Class<Sort> handledType() {
        return Sort.class;
    }
}
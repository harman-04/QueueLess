package com.queueless.backend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.io.IOException;

public class GeoJsonPointDeserializer extends JsonDeserializer<GeoJsonPoint> {
    @Override
    public GeoJsonPoint deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        var node = p.getCodec().readTree(p);
        var coords = node.get("coordinates");
        double x = coords.get(0).traverse().getValueAsDouble();
        double y = coords.get(1).traverse().getValueAsDouble();
        return new GeoJsonPoint(x, y);
    }
}

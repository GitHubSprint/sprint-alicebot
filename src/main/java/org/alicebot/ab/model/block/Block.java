package org.alicebot.ab.model.block;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Block(

	@JsonProperty("nodes")
	List<Node> nodes,

	@JsonProperty("edges")
	List<Edge> edges
) {
    public static Block fromJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, Block.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
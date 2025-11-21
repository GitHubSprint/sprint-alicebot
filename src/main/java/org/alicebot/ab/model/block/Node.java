package org.alicebot.ab.model.block;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Node(

	@JsonProperty("system")
	String system,

	@JsonProperty("tech_id")
	String techId,

	@JsonProperty("assistant")
	String assistant,

	@JsonProperty("name")
	String name,

	@JsonProperty("pattern")
	String pattern,

	@JsonProperty("model")
	String model,

	@JsonProperty("addparams")
	String addparams,

	@JsonProperty("tech_pos_x")
	int techPosX,

	@JsonProperty("tech_pos_y")
	int techPosY,

	@JsonProperty("first")
	boolean first
) {
}
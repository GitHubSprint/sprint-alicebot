package org.alicebot.ab.model.block;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Edge(

	@JsonProperty("target_tech_id")
	String targetTechId,

	@JsonProperty("value")
	String value,

	@JsonProperty("source_tech_id")
	String sourceTechId
) {
}
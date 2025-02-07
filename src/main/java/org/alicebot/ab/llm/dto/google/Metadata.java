package org.alicebot.ab.llm.dto.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Metadata{
	private TokenMetadata tokenMetadata;

	public TokenMetadata getTokenMetadata() {
		return tokenMetadata;
	}

	public void setTokenMetadata(TokenMetadata tokenMetadata) {
		this.tokenMetadata = tokenMetadata;
	}

	@Override
	public String toString() {
		return "Metadata{" +
				"tokenMetadata=" + tokenMetadata +
				'}';
	}
}
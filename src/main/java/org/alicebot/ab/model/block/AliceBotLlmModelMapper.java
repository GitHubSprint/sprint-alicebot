package org.alicebot.ab.model.block;

public class AliceBotLlmModelMapper {
    private Long id;
    private LlmType llmType;
    private String modelName;
    private String modelLabel;
    private String description;
    private String symbol;

    public AliceBotLlmModelMapper() {}

    public AliceBotLlmModelMapper(Long id, LlmType llmType, String modelName, String modelLabel, String description, String symbol) {
        this.id = id;
        this.llmType = llmType;
        this.modelName = modelName;
        this.modelLabel = modelLabel;
        this.description = description;
        this.symbol = symbol;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LlmType getLlmType() {
        return llmType;
    }

    public void setLlmType(LlmType llmType) {
        this.llmType = llmType;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelLabel() {
        return modelLabel;
    }

    public void setModelLabel(String modelLabel) {
        this.modelLabel = modelLabel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return "AliceBotLlmModelMapper{" +
                "id=" + id +
                ", llmType=" + llmType +
                ", modelName='" + modelName + '\'' +
                ", modelLabel='" + modelLabel + '\'' +
                ", description='" + description + '\'' +
                ", symbol='" + symbol + '\'' +
                '}';
    }
}

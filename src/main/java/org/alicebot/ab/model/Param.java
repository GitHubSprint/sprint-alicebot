package org.alicebot.ab.model;

public record Param(String name, String value) {
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Param param = (Param) o;
        return name().equals(param.name());
    }

    @Override
    public int hashCode() {
        return name().hashCode();
    }
}

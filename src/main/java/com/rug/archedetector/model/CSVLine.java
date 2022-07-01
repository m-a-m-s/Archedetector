package com.rug.archedetector.model;

public class CSVLine {
    public String key;
    public boolean existence;
    public boolean property;
    public boolean executive;
    public boolean isArch() {
        return existence || property || executive;
    }

    public String toString() {
        if (!isArch()) return key + ": no arch";
        return key + ": " +
                (existence? "existence, " : "") +
                (executive? "executive, " : "") +
                (property? "property, " : "");
    }
}

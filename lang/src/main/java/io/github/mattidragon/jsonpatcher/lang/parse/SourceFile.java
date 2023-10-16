package io.github.mattidragon.jsonpatcher.lang.parse;

public record SourceFile(String name, String code) {
    public int findRow(int row) {
        var index = 0;
        var currentRow = 1;
        var size = code.length();
        while (currentRow < row) {
            var c = code.charAt(index);
            if (c == '\n') currentRow++;
            index++;
            if (index >= size) return -1;
        }

        return index;
    }

    @Override
    public String toString() {
        return "SourceFile[%s]".formatted(name);
    }
}

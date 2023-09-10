package io.github.mattidragon.jsonpatch.lang;

import io.github.mattidragon.jsonpatch.lang.parse.SourceSpan;
import org.jetbrains.annotations.Nullable;

public abstract class PositionedException extends RuntimeException {
    public PositionedException(String message) {
        super(message);
    }

    protected abstract String getBaseMessage();
    @Nullable
    protected abstract SourceSpan getPos();

    @Override
    public final String getMessage() {
        var message = new StringBuilder("\n| ");
        message.append(getBaseMessage());
        message.append(":\n|   ");

        var pos = getPos();
        message.append(super.getMessage()).append("\n| \n| ");
        if (pos == null) {
            message.append("Location unavailable: no location specified");
            return message.toString();
        }

        var from = pos.from();
        var to = pos.to();
        if (from.file() != to.file()){
            message.append("Location unavailable: inconsistent file\n| (from: %s, to: %s)".formatted(from, to));
            return message.toString();
        }

        var file = from.file();

        if (from.row() > to.row() || from.row() == to.row() && from.column() > to.column()){
            message.append("Location unavailable: unexpected position order\n| (from: %s, to: %s)".formatted(from, to));
            return message.toString();
        }

        if (from.row() == to.row()) {
            var row = from.row();
            var rowBegin = file.findRow(row);
            var rowEnd = file.findRow(row + 1);
            if (rowEnd == -1) rowEnd = file.code().length() - 1;

            if (from.column() == to.column()) {
                message.append("Location: in %s, line %s, column %s\n| ".formatted(file.name(), row, from.column()));
            } else {
                message.append("Location: in %s, line %s, column %s - %s\n| ".formatted(file.name(), row, from.column(), to.column()));
            }
            message.append(file.code().substring(rowBegin, rowEnd + 1).replace("\t", "    ")).append("\n| ");
            message.append(" ".repeat(from.column() - 1));
            message.append("^".repeat(to.column() - from.column() + 1));
            message.append(" here");
            return message.toString();
        }

        message.append("Location unavailable: multiline location\n(from: %s, to: %s)".formatted(from, to));
        return message.toString();
    }
}

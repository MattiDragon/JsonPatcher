package io.github.mattidragon.jsonpatcher.lang;

import io.github.mattidragon.jsonpatcher.config.Config;
import io.github.mattidragon.jsonpatcher.lang.parse.SourceSpan;
import org.jetbrains.annotations.Nullable;

public abstract class PositionedException extends RuntimeException {
    protected PositionedException(String message) {
        super(message);
    }

    protected PositionedException(String message, Throwable cause) {
        super(message, cause);
    }

    protected abstract String getBaseMessage();
    @Nullable
    protected abstract SourceSpan getPos();

    @Override
    public synchronized Throwable fillInStackTrace() {
        if (Config.MANAGER.get().useJavaStacktrace()) return super.fillInStackTrace();
        return this;
    }

    @Override
    public synchronized Throwable getCause() {
        if (Config.MANAGER.get().useJavaStacktrace()) return super.getCause();

        var original = super.getCause();
        return original instanceof PositionedException ? null : original;
    }

    @Override
    public final String getMessage() {
        var message = new StringBuilder("\n| ");
        message.append(getBaseMessage());
        message.append("\n| ");

        fillInError(message);

        if (!Config.MANAGER.get().useJavaStacktrace()
            && super.getCause() instanceof PositionedException cause) {
            message.append("\n| \n| Caused by:\n| ");
            cause.fillInError(message);
        }

        return message.toString();
    }

    private void fillInError(StringBuilder message) {
        message.append("  ").append(super.getMessage()).append("\n| ");

        var pos = getPos();
        if (pos == null) {
            message.append("Location unavailable: no location specified");
            return;
        }

        var from = pos.from();
        var to = pos.to();
        if (from.file() != to.file()){
            message.append("Location unavailable: inconsistent file\n| (from: %s, to: %s)".formatted(from, to));
            return;
        }

        var file = from.file();

        if (from.row() > to.row() || from.row() == to.row() && from.column() > to.column()){
            message.append("Location unavailable: unexpected position order\n| (from: %s, to: %s)".formatted(from, to));
            return;
        }

        if (from.column() - 1 < 0 || to.column() - from.column() + 1 < 0) {
            message.append("Location unavailable: broken position\n| (from: %s, to: %s)".formatted(from, to));
            return;
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
            message.append(file.code()
                            .substring(rowBegin, rowEnd)
                            .replace("\t", "    ")
                            .replace("\n", "")
                            .replace("\r", ""))
                    .append("\n| ");
            message.append(" ".repeat(from.column() - 1));
            message.append("^".repeat(to.column() - from.column() + 1));
            message.append(" here");
            return;
        }

        message.append("Location unavailable: multiline location\n(from: %s, to: %s)".formatted(from, to));
    }
}

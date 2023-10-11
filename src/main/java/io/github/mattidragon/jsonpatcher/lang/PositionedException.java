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

        if (super.getCause() instanceof PositionedException cause) {
            if (Config.MANAGER.get().useShortStacktrace()) {
                message.append("\n| Caused by: ");
                cause.fillInShortError(message);
            } else if (!Config.MANAGER.get().useJavaStacktrace()) {
                message.append("\n| \n| Caused by:\n| ");
                cause.fillInError(message);
            }
        }

        return message.toString();
    }

    private void fillInShortError(StringBuilder message) {
        message.append(super.getMessage())
                .append(" at ");

        message.append(formatLocation(getPos()).location);
    }

    private void fillInError(StringBuilder message) {
        message.append("  ").append(super.getMessage()).append("\n| ");

        var pos = getPos();
        var location = formatLocation(pos);
        message.append("Location: ")
                .append(location.location)
                .append("\n| ");
        if (pos == null || !location.wellBehaved) return;

        var from = pos.from();
        var to = pos.to();
        var file = from.file();

        if (from.row() == to.row()) {
            var row = from.row();
            var rowBegin = file.findRow(row);
            var rowEnd = file.findRow(row + 1);
            if (rowEnd == -1) rowEnd = file.code().length() - 1;

            message.append(file.code()
                            .substring(rowBegin, rowEnd)
                            .replace("\t", "    ")
                            .replace("\n", "")
                            .replace("\r", ""))
                    .append("\n| ");
            message.append(" ".repeat(from.column() - 1));
            message.append("^".repeat(to.column() - from.column() + 1));
            message.append(" here");
        }
    }

    private static FormattedLocation formatLocation(SourceSpan pos) {
        if (pos == null) {
            return new FormattedLocation("unknown", false);
        }

        var from = pos.from();
        var to = pos.to();
        var file = from.file();

        if (from.file() != to.file()){
            return new FormattedLocation("error: inconsistent file (from: %s, to: %s)".formatted(from, to), false);
        }

        if (from.row() > to.row() || from.row() == to.row() && from.column() > to.column()){
            return new FormattedLocation("error: unexpected position order (from: %s, to: %s)".formatted(from, to), false);
        }

        if (from.column() - 1 < 0 || to.column() - from.column() + 1 < 0) {
            return new FormattedLocation("error: broken position (from: %s, to: %s)".formatted(from, to), false);
        }

        if (from.row() != to.row()) {
            return new FormattedLocation("%s %s:%s - %s:%s".formatted(file.name(), from.row(), from.column(), to.row(), to.column()), false);
        }

        var row = from.row();
        if (from.column() == to.column()) {
            return new FormattedLocation("%s %s:%s".formatted(file.name(), row, from.column()), true);
        } else {
            return new FormattedLocation("%s %s:%s-%s".formatted(file.name(), row, from.column(), to.column()), true);
        }
    }

    private record FormattedLocation(String location, boolean wellBehaved) {

    }
}

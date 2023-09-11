package com.github.manolo8.darkbot.config.actions;

import com.github.manolo8.darkbot.config.actions.parser.Values;
import com.github.manolo8.darkbot.gui.utils.highlight.Locatable;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.NotNull;

import javax.swing.text.Position;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SyntaxException extends RuntimeException implements Locatable {

    private static final String[] EMPTY = new String[]{};

    @Delegate
    private final Locatable loc;
    private final String[] chars;
    private final List<Values.Meta<?>> metadatas;
    @Getter
    private boolean singleMeta = false;

    public SyntaxException(String message, Locatable loc, String... chars) {
        super(message);
        this.loc = loc;
        this.chars = chars == null ? EMPTY : chars;
        this.metadatas = Collections.emptyList();
    }

    public SyntaxException(String message, Locatable loc, Values.Meta<?> meta, String... chars) {
        this(message, loc, meta == null ? null : Collections.singletonList(meta), chars);
        singleMeta = true;
    }

    public SyntaxException(String message, Locatable loc, List<Values.Meta<?>> metas, String... chars) {
        super(message);
        this.loc = loc;
        this.chars = chars == null ? EMPTY : chars;
        this.metadatas = metas == null ? Collections.emptyList() : metas;
    }

    public <E extends Enum<E>> SyntaxException(String message, Locatable loc, Class<E> metadatas) {
        this(message, loc, (List<Values.Meta<?>>) null, Arrays.stream(metadatas.getEnumConstants())
                .map(Objects::toString).toArray(String[]::new));
    }

    public SyntaxException(String message, String at) {
        this(message, at, (List<Values.Meta<?>>) null, (String[]) null);
    }

    public SyntaxException(String message, int atIdx, String... chars) {
        super(message);
        this.loc = new Locatable() {
            @Override
            public Position getStart() {
                return () -> atIdx;
            }

            @Override
            public Position getEnd() {
                return null;
            }
        };
        this.chars = chars == null ? EMPTY : chars;
        this.metadatas = Collections.emptyList();
    }

    public <E extends Enum<E>> SyntaxException(String message, String at, Class<E> metadatas) {
        this(message, at, (List<Values.Meta<?>>) null, Arrays.stream(metadatas.getEnumConstants())
                .map(Objects::toString).toArray(String[]::new));
    }

    public SyntaxException(String message, String at, Values.Meta<?> meta, String... chars) {
        this(message, at, meta == null ? null : Collections.singletonList(meta), chars);
        singleMeta = true;
    }

    public SyntaxException(String message, String at, List<Values.Meta<?>> metas, String... chars) {
        super(message);
        this.loc = null;
        this.chars = chars == null ? EMPTY : chars;
        this.metadatas = metas == null ? Collections.emptyList() : metas;
    }

    public int getAt() {
        return loc.getStart().getOffset();
    }

    public @NotNull String[] getExpected() {
        return chars;
    }

    public @NotNull List<Values.Meta<?>> getMetadata() {
        return metadatas;
    }

}

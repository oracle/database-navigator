package com.dbn.vector.model.chunk;

import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Json;
import com.dbn.vector.model.VectorEmbeddingConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jspecify.annotations.NonNull;

import java.util.Map;

import static com.dbn.common.options.setting.Settings.integerAttribute;
import static com.dbn.common.options.setting.Settings.setIntegerAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
@NonNls
public class ChunkConfig extends VectorEmbeddingConfig implements Cloneable<ChunkConfig> {
    private String chunkBy = "WORDS";
    private String splitBy = "NEWLINE";
    private int maxSize = 100;
    private int overlap = 10;

    public ChunkConfig() {
    }

    public ChunkConfig(String chunkBy, int max, String splitBy, int overlap) {
        this.chunkBy = chunkBy;
        this.maxSize = max;
        this.splitBy = splitBy;
        this.overlap = overlap;
    }

    public String getConfigJson() {
        return Json.writeAsString(getConfigMap());
    }

    @NonNull
    public Map<String, ?> getConfigMap() {
        return Map.of(
                "chunkBy", chunkBy,
                "splitBy", splitBy,
                "max", maxSize,
                "overlap", overlap);
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        super.readState(element);
        chunkBy = stringAttribute(element, "chunk-by", chunkBy);
        splitBy = stringAttribute(element, "split-by", splitBy);
        maxSize = integerAttribute(element, "max-size", maxSize);
        overlap = integerAttribute(element, "overlap", overlap);
    }

    @Override
    public void writeState(Element element) {
        super.writeState(element);
        setStringAttribute(element, "chunk-by", chunkBy);
        setStringAttribute(element, "split-by", splitBy);
        setIntegerAttribute(element, "max-size", maxSize);
        setIntegerAttribute(element, "overlap", overlap);
    }

    @Override
    @SneakyThrows
    public ChunkConfig clone() {
        return cast(super.clone());
    }
}

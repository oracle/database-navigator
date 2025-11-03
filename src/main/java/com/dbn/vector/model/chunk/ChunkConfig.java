package com.dbn.vector.model.chunk;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Json;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import java.util.Map;

import static com.dbn.common.options.setting.Settings.integerAttribute;
import static com.dbn.common.options.setting.Settings.setIntegerAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
@NonNls
public class ChunkConfig implements PersistentStateElement, Cloneable<ChunkConfig> {
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
        return Json.writeAsString(Map.of(
                "chunkBy", chunkBy,
                "splitBy", splitBy,
                "max", maxSize,
                "overlap", overlap));
    }

    @Override
    public void readState(Element element) {
        chunkBy = stringAttribute(element, "chunk-by", chunkBy);
        splitBy = stringAttribute(element, "split-by", splitBy);
        maxSize = integerAttribute(element, "max-size", maxSize);
        overlap = integerAttribute(element, "overlap", overlap);
    }

    @Override
    public void writeState(Element element) {
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

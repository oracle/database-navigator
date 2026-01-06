package com.dbn.vector.model.source;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.vector.model.sourceconfig.DbTableSource;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
public class DBTableSourceConfig implements PersistentStateElement {
  private List<DbTableSource>  dbTableSources = new ArrayList<>();
  private boolean autoSync;

  @Override
  public void readState(Element element) {
    for (Element child : element.getChildren()) {
      DbTableSource source = new DbTableSource();
      source.readState(child);
      dbTableSources.add(source);
    }
  }

  @Override
  public void writeState(Element element) {
    for (DbTableSource source : dbTableSources) {
      Element childElement = new Element(String.valueOf(element), "table-source");
      source.writeState(childElement);
    }
  }

}

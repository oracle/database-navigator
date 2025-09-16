package com.dbn.mcp.models;

import com.dbn.mcp.McpServerInputForm;
import com.dbn.mcp.ui.ToolDefinitionCreateForm;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbn.mcp.ui.ToolDefinitionCreateForm.*;

@Data
public class ToolDefinitionModel {
  String name ;
  String description ;
  String sql;
  String rewritingSql;
  String paramOrderCsv;
  String jsonSchema;
  ToolDefinitionCreateForm.ParamTableModel paramsModel;

  public ToolDefinitionModel(ToolDefinitionCreateForm.ParamTableModel paramsModel) {
    this.paramsModel = paramsModel;
  }


  public String getRewritingSql() {
    return rewriteToJdbc(sql).rewrittenSql;
  }
  public String getParamOrderCsv() {

    Rewritten r = rewriteToJdbc(sql);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < r.paramOrder.size(); i++) {
      if (i > 0) sb.append(',');
      sb.append(r.paramOrder.get(i));
    }
    return sb.toString();
  }

  public String getToolJsonSchema() {

    List<String> occ = parseOccurrences(sql);
    List<String> uniq = uniqueInOrder(occ);

    Map<String, McpServerInputForm.ParamRow> byName = new LinkedHashMap<>();
    for (McpServerInputForm.ParamRow r : paramsModel.getRows()) byName.put(stripColon(r.name), r);

    StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append("\"type\":\"object\",");
    sb.append("\"properties\":{");
    for (int i = 0; i < uniq.size(); i++) {
      String n = uniq.get(i);
      if (i > 0) sb.append(',');
      sb.append("\"").append(escape(n)).append("\":{");
      McpServerInputForm.ParamRow r = byName.get(n);
      McpServerInputForm.ParamType t = r == null ? McpServerInputForm.ParamType.String : r.type;
      sb.append("\"type\":\"").append(jsonType(t)).append("\"");
      if (t == McpServerInputForm.ParamType.Date) sb.append(',').append("\"format\":\"date\"");
      if (r != null && r.defaultValue != null && !r.defaultValue.isEmpty()) {
        sb.append(',').append("\"default\":");
        switch (t) {
          case Boolean:
            sb.append(r.defaultValue.equalsIgnoreCase("true") || r.defaultValue.equals("1"));
            break;
          case Integer:
          case Float:
            sb.append(r.defaultValue);
            break;
          default:
            sb.append("\"").append(escape(r.defaultValue)).append("\"");
        }
      }
      sb.append('}');
    }
    sb.append("},");
    sb.append("\"required\":[");
    for (int i = 0; i < uniq.size(); i++) {
      if (i > 0) sb.append(',');
      sb.append("\"").append(escape(uniq.get(i))).append("\"");
    }
    sb.append(']');
    sb.append('}');
    return sb.toString();
  }




  // helpers

  private static String jsonType(McpServerInputForm.ParamType t) {
    switch (t) {
      case Integer:
        return "integer";
      case Float:
        return "number";
      case Boolean:
        return "boolean";
      case Date:
        return "string";
      default:
        return "string";
    }
  }

  private String escape(String s) {
    return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
  private static Rewritten rewriteToJdbc(String sql) {
    if (sql == null) return new Rewritten("", List.of());
    StringBuffer out = new StringBuffer();
    List<String> order = new ArrayList<>();
    Matcher m = Pattern.compile(":(\\w+)").matcher(sql);
    while (m.find()) {
      order.add(m.group(1));
      m.appendReplacement(out, "?");
    }
    m.appendTail(out);
    return new Rewritten(out.toString(), order);
  }


  private static class Rewritten {
    final String rewrittenSql; // with '?'
    final List<String> paramOrder; // names without colon, repeats preserved

    Rewritten(String s, List<String> o) {
      this.rewrittenSql = s;
      this.paramOrder = o;
    }
  }
}

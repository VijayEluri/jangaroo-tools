package net.jangaroo.jooc;

import com.google.common.collect.Multimap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class DependencyGraphFile {
  public static void writeDependencyFile(Multimap<String, String> edges, Set<String> nodes, Set<String> marked, File outFile) throws IOException {
    File parentFile = outFile.getParentFile();
    if (parentFile != null) {
      parentFile.mkdirs();
    }
    PrintWriter writer = new PrintWriter(new FileWriter(outFile));
    try {
      writeGraph(writer, edges, nodes, marked);
    } finally {
      writer.close();
    }
  }

  private static void writeGraph(PrintWriter writer, Multimap<String, String> edges, Collection<String> nodes, Collection<String> marked) {
    writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    writer.println("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" xmlns:java=\"http://www.yworks.com/xml/yfiles-common/1.0/java\" xmlns:sys=\"http://www.yworks.com/xml/yfiles-common/markup/primitives/2.0\" xmlns:x=\"http://www.yworks.com/xml/yfiles-common/markup/2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:y=\"http://www.yworks.com/xml/graphml\" xmlns:yed=\"http://www.yworks.com/xml/yed/3\" xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://www.yworks.com/xml/schema/graphml/1.1/ygraphml.xsd\">");
    writer.println("<key for=\"node\" id=\"d6\" yfiles.type=\"nodegraphics\"/>");
    for (String nodeId : nodes) {
      writer.println("    <node id=\"" + nodeId + "\">");
      writer.println("      <data key=\"d6\">");
      writer.println("        <y:ShapeNode>");
      if (marked.contains(nodeId)) {
        writer.println("        <y:Fill color=\"#FFA0A0\" transparent=\"false\"/>");
      } else {
        writer.println("        <y:Fill color=\"#A0FFA0\" transparent=\"false\"/>");
      }
      writer.println("          <y:Geometry height=\"20.0\" width=\"" + 8 * nodeId.length() + "\"/>\n");
      writer.println("          <y:NodeLabel>" + nodeId + "</y:NodeLabel>");
      writer.println("        </y:ShapeNode>");
      writer.println("      </data>");
      writer.println("    </node>");
    }
    for (Map.Entry<String, String> entry : edges.entries()) {
      if (nodes.contains(entry.getKey()) && nodes.contains(entry.getValue())) {
        writer.println("    <edge source=\"" + entry.getKey() + "\" target=\"" + entry.getValue() + "\"/>");
      }
    }
    writer.println("</graphml>");
  }
}
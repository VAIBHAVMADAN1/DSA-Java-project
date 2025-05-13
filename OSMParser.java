import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
import java.util.*;

public class OSMParser {

    public static void main(String[] args) throws Exception {
        File inputFile = new File("map3.osm");
        File outputFile = new File("locations.txt");

        // Extract and write all named locations
        extractAndSaveLocations(inputFile, outputFile);
    }

    // Step 1: Extract nodes and buildings and save them
    public static void extractAndSaveLocations(File input, File output) throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = db.parse(input);
        doc.getDocumentElement().normalize();

        // Write results to file
        BufferedWriter writer = new BufferedWriter(new FileWriter(output));
        Map<String, double[]> nodeMap = new HashMap<>();

        NodeList nodeList = doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            String id = node.getAttribute("id");
            double lat = Double.parseDouble(node.getAttribute("lat"));
            double lon = Double.parseDouble(node.getAttribute("lon"));
            nodeMap.put(id, new double[] { lat, lon });

            NodeList tags = node.getElementsByTagName("tag");
            for (int j = 0; j < tags.getLength(); j++) {
                Element tag = (Element) tags.item(j);
                if ("name".equals(tag.getAttribute("k"))) {
                    String name = tag.getAttribute("v");
                    writer.write(name + "\t" + lat + "\t" + lon + "\n");
                    break;
                }
            }
        }

        // Buildings as ways
        NodeList wayList = doc.getElementsByTagName("way");
        for (int i = 0; i < wayList.getLength(); i++) {
            Element way = (Element) wayList.item(i);
            NodeList tags = way.getElementsByTagName("tag");

            String name = null, building = null;
            for (int j = 0; j < tags.getLength(); j++) {
                Element tag = (Element) tags.item(j);
                if ("name".equals(tag.getAttribute("k")))
                    name = tag.getAttribute("v");
                if ("building".equals(tag.getAttribute("k")))
                    building = tag.getAttribute("v");
            }

            if (name != null && building != null) {
                NodeList nds = way.getElementsByTagName("nd");
                List<double[]> coords = new ArrayList<>();
                for (int j = 0; j < nds.getLength(); j++) {
                    String ref = ((Element) nds.item(j)).getAttribute("ref");
                    if (nodeMap.containsKey(ref)) {
                        coords.add(nodeMap.get(ref));
                    }
                }

                if (!coords.isEmpty()) {
                    double lat = coords.stream().mapToDouble(p -> p[0]).average().orElse(0);
                    double lon = coords.stream().mapToDouble(p -> p[1]).average().orElse(0);
                    writer.write(name + "\t" + lat + "\t" + lon + "\n");
                }
            }
        }

        writer.close();
        System.out.println("Location data saved to: " + output.getAbsolutePath());
    }
}

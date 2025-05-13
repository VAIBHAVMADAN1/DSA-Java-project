import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class OSMRouteCalculator {

    static class Coordinate {
        double lat;
        double lon;

        public Coordinate(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public String toString() {
            return String.format("(%.6f, %.6f)", lat, lon);
        }
    }

    static class RoadNetwork {
        Map<Long, Coordinate> nodes = new HashMap<>();
        Map<Long, List<Long>> adjacency = new HashMap<>();
        Map<String, Double> edgeLengths = new HashMap<>();

        public void addNode(long id, double lat, double lon) {
            nodes.put(id, new Coordinate(lat, lon));
        }

        public void addEdge(long from, long to) {
            if (!nodes.containsKey(from) || !nodes.containsKey(to)) {
                return; // Skip if nodes don't exist
            }

            adjacency.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
            adjacency.computeIfAbsent(to, k -> new ArrayList<>()).add(from);

            double length = calculateDistance(nodes.get(from), nodes.get(to));
            edgeLengths.put(from + "-" + to, length);
            edgeLengths.put(to + "-" + from, length);
        }

        public double getEdgeLength(long from, long to) {
            return edgeLengths.getOrDefault(from + "-" + to, Double.POSITIVE_INFINITY);
        }
    }

    static class NamedPlace {
        String name;
        Coordinate coord;

        public NamedPlace(String name, Coordinate coord) {
            this.name = name;
            this.coord = coord;
        }

        @Override
        public String toString() {
            return name + " " + coord;
        }
    }

    public static void main(String[] args) {
        try {
            String osmFile = "map3.osm";
            System.out.println("Loading OSM data from: " + osmFile);

            // 1. Parse OSM file and build road network
            RoadNetwork network = parseOSMFile(osmFile);
            System.out.println("Network loaded with " + network.nodes.size() + " nodes and " +
                    (network.edgeLengths.size() / 2) + " edges");

            // 2. Extract named places
            List<NamedPlace> places = extractNamedPlaces(osmFile, network);
            System.out.println("Found " + places.size() + " named locations");

            if (places.isEmpty()) {
                System.out.println("No named locations found.");
                return;
            }

            // 3. Display locations
            System.out.println("\nAvailable locations:");
            for (int i = 0; i < places.size(); i++) {
                System.out.println(i + ": " + places.get(i));
            }

            // 4. Get user input
            Scanner scanner = new Scanner(System.in);
            System.out.print("\nEnter source location number: ");
            int sourceIdx = scanner.nextInt();
            System.out.print("Enter destination location number: ");
            int destIdx = scanner.nextInt();

            NamedPlace source = places.get(sourceIdx);
            NamedPlace dest = places.get(destIdx);

            System.out.println("\nSelected locations:");
            System.out.println("Source: " + source);
            System.out.println("Destination: " + dest);

            // 5. Calculate direct distance
            double directDistance = calculateDistance(source.coord, dest.coord);
            System.out.printf("\nDirect distance: %.2f meters\n", directDistance);

            // 6. Find nearest nodes
            System.out.println("\nFinding nearest road network nodes...");
            long sourceNode = findNearestNode(network, source.coord);
            long destNode = findNearestNode(network, dest.coord);

            System.out.println("Nearest road nodes:");
            System.out.println("Source node: " + sourceNode + " at " + network.nodes.get(sourceNode));
            System.out.println("Destination node: " + destNode + " at " + network.nodes.get(destNode));

            // Debug network connectivity
            debugNetworkConnectivity(network, sourceNode, destNode);

            // 7. Calculate shortest path
            System.out.println("\nCalculating shortest path...");
            try {
                double roadDistance = calculateShortestPath(network, sourceNode, destNode);
                System.out.printf("\nShortest road path: %.2f meters\n", roadDistance);
                System.out.printf("Road distance is %.1f%% longer than direct distance\n",
                        ((roadDistance / directDistance) - 1) * 100);
            } catch (Exception e) {
                System.out.println("\nERROR: " + e.getMessage());
                System.out.println("No road path found between '" + source.name + "' and '" + dest.name + "'");
                System.out.println("Possible reasons:");
                System.out.println("- Locations are on disconnected road segments");
                System.out.println("- Missing connections in OSM data");
                System.out.println("- The area might be marked as private or inaccessible");

                // Try to find why - check if nodes are isolated
                if (!network.adjacency.containsKey(sourceNode)) {
                    System.out.println("- Source node has no connections");
                }
                if (!network.adjacency.containsKey(destNode)) {
                    System.out.println("- Destination node has no connections");
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static RoadNetwork parseOSMFile(String filename) throws Exception {
        RoadNetwork network = new RoadNetwork();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(filename));

        // Parse nodes
        NodeList nodeList = doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            long id = Long.parseLong(node.getAttribute("id"));
            double lat = Double.parseDouble(node.getAttribute("lat"));
            double lon = Double.parseDouble(node.getAttribute("lon"));
            network.addNode(id, lat, lon);
        }

        // Parse ways (roads)
        NodeList wayList = doc.getElementsByTagName("way");
        for (int i = 0; i < wayList.getLength(); i++) {
            Element way = (Element) wayList.item(i);

            // Check if it's a road (has highway tag)
            boolean isRoad = false;
            String highwayType = null;
            NodeList tags = way.getElementsByTagName("tag");
            for (int j = 0; j < tags.getLength(); j++) {
                Element tag = (Element) tags.item(j);
                if (tag.getAttribute("k").equals("highway")) {
                    isRoad = true;
                    highwayType = tag.getAttribute("v");
                    break;
                }
            }

            if (isRoad) {
                List<Long> nodeRefs = new ArrayList<>();
                NodeList nds = way.getElementsByTagName("nd");
                for (int j = 0; j < nds.getLength(); j++) {
                    Element nd = (Element) nds.item(j);
                    nodeRefs.add(Long.parseLong(nd.getAttribute("ref")));
                }

                // Add edges between consecutive nodes
                for (int j = 1; j < nodeRefs.size(); j++) {
                    network.addEdge(nodeRefs.get(j - 1), nodeRefs.get(j));
                }
            }
        }

        return network;
    }

    private static List<NamedPlace> extractNamedPlaces(String filename, RoadNetwork network) throws Exception {
        List<NamedPlace> places = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(filename));

        // From nodes
        NodeList nodeList = doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            String name = null;

            NodeList tags = node.getElementsByTagName("tag");
            for (int j = 0; j < tags.getLength(); j++) {
                Element tag = (Element) tags.item(j);
                if (tag.getAttribute("k").equals("name")) {
                    name = tag.getAttribute("v");
                    break;
                }
            }

            if (name != null) {
                long id = Long.parseLong(node.getAttribute("id"));
                if (network.nodes.containsKey(id)) {
                    places.add(new NamedPlace(name, network.nodes.get(id)));
                }
            }
        }

        // From ways
        NodeList wayList = doc.getElementsByTagName("way");
        for (int i = 0; i < wayList.getLength(); i++) {
            Element way = (Element) wayList.item(i);
            String name = null;

            NodeList tags = way.getElementsByTagName("tag");
            for (int j = 0; j < tags.getLength(); j++) {
                Element tag = (Element) tags.item(j);
                if (tag.getAttribute("k").equals("name")) {
                    name = tag.getAttribute("v");
                    break;
                }
            }

            if (name != null) {
                NodeList nds = way.getElementsByTagName("nd");
                for (int j = 0; j < nds.getLength(); j++) {
                    Element nd = (Element) nds.item(j);
                    long ref = Long.parseLong(nd.getAttribute("ref"));
                    if (network.nodes.containsKey(ref)) {
                        places.add(new NamedPlace(name, network.nodes.get(ref)));
                        break;
                    }
                }
            }
        }

        // Remove duplicates
        return places.stream()
                .collect(Collectors.toMap(
                        p -> p.name + p.coord.lat + p.coord.lon,
                        p -> p,
                        (p1, p2) -> p1))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    private static long findNearestNode(RoadNetwork network, Coordinate coord) {
        long nearestNode = -1;
        double minDistance = Double.POSITIVE_INFINITY;

        // Only consider nodes that are actually connected
        for (Long nodeId : network.adjacency.keySet()) {
            Coordinate nodeCoord = network.nodes.get(nodeId);
            double dist = calculateDistance(coord, nodeCoord);
            if (dist < minDistance) {
                minDistance = dist;
                nearestNode = nodeId;
            }
        }

        return nearestNode;
    }

    private static double calculateShortestPath(RoadNetwork network, long source, long target) {
        if (!network.nodes.containsKey(source) || !network.nodes.containsKey(target)) {
            throw new RuntimeException("Source or target node not found in network");
        }

        Map<Long, Double> distances = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        PriorityQueue<Long> queue = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

        // Initialize distances
        for (Long node : network.nodes.keySet()) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }
        distances.put(source, 0.0);
        queue.add(source);

        while (!queue.isEmpty()) {
            long current = queue.poll();

            if (current == target) {
                // Reconstruct path for debugging
                List<Long> path = new ArrayList<>();
                for (Long node = target; node != null; node = previous.get(node)) {
                    path.add(node);
                }
                Collections.reverse(path);
                System.out.println("Path found with " + path.size() + " nodes");
                return distances.get(current);
            }

            if (distances.get(current) == Double.POSITIVE_INFINITY) {
                break; // All remaining nodes are inaccessible
            }

            for (Long neighbor : network.adjacency.getOrDefault(current, Collections.emptyList())) {
                double edgeLength = network.getEdgeLength(current, neighbor);
                double newDist = distances.get(current) + edgeLength;

                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    previous.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        throw new RuntimeException("No path exists between the nodes");
    }

    private static void debugNetworkConnectivity(RoadNetwork network, long node1, long node2) {
        System.out.println("\n--- Network Connectivity Debug ---");

        // Check if nodes exist
        if (!network.nodes.containsKey(node1)) {
            System.out.println("Source node " + node1 + " not found in network!");
        } else {
            System.out.println("Source node " + node1 + " has " +
                    network.adjacency.getOrDefault(node1, Collections.emptyList()).size() + " connections");
        }

        if (!network.nodes.containsKey(node2)) {
            System.out.println("Destination node " + node2 + " not found in network!");
        } else {
            System.out.println("Destination node " + node2 + " has " +
                    network.adjacency.getOrDefault(node2, Collections.emptyList()).size() + " connections");
        }

        // Check if nodes are in the same connected component
        if (network.nodes.containsKey(node1) && network.nodes.containsKey(node2)) {
            Set<Long> visited = new HashSet<>();
            Queue<Long> queue = new LinkedList<>();
            queue.add(node1);
            visited.add(node1);

            boolean found = false;
            while (!queue.isEmpty()) {
                long current = queue.poll();
                if (current == node2) {
                    found = true;
                    break;
                }
                for (Long neighbor : network.adjacency.getOrDefault(current, Collections.emptyList())) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }

            if (found) {
                System.out.println("Nodes are in the same connected component");
            } else {
                System.out.println("Nodes are in DIFFERENT connected components - no path exists");
            }
        }
    }

    // Haversine formula for distance between two coordinates
    private static double calculateDistance(Coordinate c1, Coordinate c2) {
        final int R = 6371; // Earth radius in km

        double latDistance = Math.toRadians(c2.lat - c1.lat);
        double lonDistance = Math.toRadians(c2.lon - c1.lon);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(c1.lat)) * Math.cos(Math.toRadians(c2.lat))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c * 1000; // convert to meters
    }
}
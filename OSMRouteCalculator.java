
// Required imports for XML parsing and basic utilities
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class OSMRouteCalculator {

    // Represents a geographic coordinate (latitude and longitude)
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

    // Represents a road network using nodes (points) and edges (connections between
    // points)
    static class RoadNetwork {
        Map<Long, Coordinate> nodes = new HashMap<>(); // Node ID → Coordinates
        Map<Long, List<Long>> adjacency = new HashMap<>(); // Node ID → List of connected node IDs
        Map<String, Double> edgeLengths = new HashMap<>(); // "from-to" string → distance

        // Adds a node to the network
        public void addNode(long id, double lat, double lon) {
            nodes.put(id, new Coordinate(lat, lon));
        }

        // Adds a bidirectional edge (connection) between two nodes
        public void addEdge(long from, long to) {
            if (!nodes.containsKey(from) || !nodes.containsKey(to)) {
                return; // Skip if either node doesn't exist
            }

            // Add the edge to both directions (undirected graph)
            adjacency.computeIfAbsent(from, _ -> new ArrayList<>()).add(to);
            adjacency.computeIfAbsent(to, _ -> new ArrayList<>()).add(from);

            // Calculate the physical distance between the nodes
            double length = calculateDistance(nodes.get(from), nodes.get(to));
            edgeLengths.put(from + "-" + to, length);
            edgeLengths.put(to + "-" + from, length);
        }

        // Retrieves the edge length between two nodes
        public double getEdgeLength(long from, long to) {
            return edgeLengths.getOrDefault(from + "-" + to, Double.POSITIVE_INFINITY);
        }
    }

    // Represents a place with a human-readable name and coordinate
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

            // Parse data once at startup
            RoadNetwork network = parseOSMFile(osmFile);
            List<NamedPlace> places = extractNamedPlaces(osmFile, network);

            if (places.isEmpty()) {
                System.out.println("No named locations found.");
                return;
            }

            Scanner scanner = new Scanner(System.in);
            boolean running = true;

            while (running) {
                System.out.println("\n=== OSM Route Calculator ===");
                System.out.println("1. Calculate route between locations");
                System.out.println("2. Clear console");
                System.out.println("3. Exit");
                System.out.print("Enter your choice (1-3): ");

                int choice = scanner.nextInt();

                switch (choice) {
                    case 1:
                        // Display available locations
                        System.out.println("\nAvailable locations:");
                        for (int i = 0; i < places.size(); i++) {
                            System.out.println(i + ": " + places.get(i));
                        }

                        // Get source and destination
                        System.out.print("\nEnter source location number: ");
                        int sourceIdx = scanner.nextInt();
                        System.out.print("Enter destination location number: ");
                        int destIdx = scanner.nextInt();

                        if (sourceIdx >= 0 && sourceIdx < places.size() &&
                                destIdx >= 0 && destIdx < places.size()) {

                            calculateRoute(network, places.get(sourceIdx), places.get(destIdx));
                        } else {
                            System.out.println("Invalid location numbers!");
                        }
                        break;

                    case 2:
                        // Clear console - works for both Windows and Unix-like systems
                        clearConsole();
                        break;

                    case 3:
                        running = false;
                        System.out.println("Goodbye!");
                        break;

                    default:
                        System.out.println("Invalid choice! Please enter 1-3.");
                }
            }
            scanner.close();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add this new helper method to handle route calculation
    private static void calculateRoute(RoadNetwork network, NamedPlace source, NamedPlace dest) {
        System.out.println("\nSelected locations:");
        System.out.println("Source: " + source);
        System.out.println("Destination: " + dest);

        // Calculate straight-line distance
        double directDistance = calculateDistance(source.coord, dest.coord);
        System.out.printf("\nDirect distance: %.2f meters\n", directDistance);

        // Find nearest nodes
        System.out.println("\nFinding nearest road network nodes...");
        long sourceNode = findNearestNode(network, source.coord);
        long destNode = findNearestNode(network, dest.coord);

        System.out.println("Nearest road nodes:");
        System.out.println("Source node: " + sourceNode + " at " + network.nodes.get(sourceNode));
        System.out.println("Destination node: " + destNode + " at " + network.nodes.get(destNode));

        // Calculate shortest path
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

            if (!network.adjacency.containsKey(sourceNode)) {
                System.out.println("- Source node has no connections");
            }
            if (!network.adjacency.containsKey(destNode)) {
                System.out.println("- Destination node has no connections");
            }
        }
    }
    
    private static void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            System.out.println("Could not clear console.");
        }
    }

    // Parses an OSM XML file and constructs the road network graph
    private static RoadNetwork parseOSMFile(String filename) throws Exception {
        RoadNetwork network = new RoadNetwork();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(filename));

        // Parse all <node> elements from XML
        NodeList nodeList = doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            long id = Long.parseLong(node.getAttribute("id"));
            double lat = Double.parseDouble(node.getAttribute("lat"));
            double lon = Double.parseDouble(node.getAttribute("lon"));
            network.addNode(id, lat, lon);
        }

        // Parse all <way> elements to connect nodes (roads)
        NodeList wayList = doc.getElementsByTagName("way");
        for (int i = 0; i < wayList.getLength(); i++) {
            Element way = (Element) wayList.item(i);

            boolean isRoad = false;
            NodeList tags = way.getElementsByTagName("tag");
            for (int j = 0; j < tags.getLength(); j++) {
                Element tag = (Element) tags.item(j);
                if (tag.getAttribute("k").equals("highway")) {
                    isRoad = true;
                    break;
                }
            }

            // If it's a road, connect consecutive nodes
            if (isRoad) {
                List<Long> nodeRefs = new ArrayList<>();
                NodeList nds = way.getElementsByTagName("nd");
                for (int j = 0; j < nds.getLength(); j++) {
                    Element nd = (Element) nds.item(j);
                    nodeRefs.add(Long.parseLong(nd.getAttribute("ref")));
                }

                // Create edges between adjacent nodes
                for (int j = 1; j < nodeRefs.size(); j++) {
                    network.addEdge(nodeRefs.get(j - 1), nodeRefs.get(j));
                }
            }
        }

        return network;
    }

    // Extracts human-readable place names (from nodes and ways) from the OSM file
    private static List<NamedPlace> extractNamedPlaces(String filename, RoadNetwork network) throws Exception {
        List<NamedPlace> places = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(filename));

        // From <node> tags
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

        // From <way> tags
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

            // Use first valid node of the way as coordinate
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

        // Remove duplicates based on name and coordinate
        return places.stream()
                .collect(Collectors.toMap(
                        p -> p.name + p.coord.lat + p.coord.lon,
                        p -> p,
                        (p1, p2) -> p1)) // If duplicates, keep first
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    // Finds the closest connected road node to a given coordinate
    private static long findNearestNode(RoadNetwork network, Coordinate coord) {
        long nearestNode = -1;
        double minDistance = Double.POSITIVE_INFINITY;

        // Only search among nodes with connections
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

    // Calculates the shortest path between two nodes in a road network using
    // Dijkstra's algorithm
    private static double calculateShortestPath(RoadNetwork network, long source, long target) {
        // A map to store the current shortest known distance from the source to each
        // node
        Map<Long, Double> distances = new HashMap<>();

        // A map to remember the previous node in the shortest path (used for path
        // reconstruction)
        Map<Long, Long> previous = new HashMap<>();

        // Priority queue to always process the next node with the smallest known
        // distance.
        // The comparator uses the current values from the 'distances' map to determine
        // priority.
        PriorityQueue<Long> queue = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

        // Initialize distances for all nodes to infinity, indicating they are
        // unreachable initially
        for (Long node : network.nodes.keySet()) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }

        // Distance to the source node is 0 (starting point)
        distances.put(source, 0.0);

        // Add the source node to the priority queue to begin the algorithm
        queue.add(source);

        // Main loop: continues until there are no more nodes to process
        while (!queue.isEmpty()) {
            // Extract the node with the currently known shortest distance
            long current = queue.poll();

            // If we've reached the target node, we can stop early and return the shortest
            // distance
            if (current == target) {
                // Optional: Trace back the path from target to source for debugging or
                // visualization
                List<Long> path = new ArrayList<>();
                for (Long node = target; node != null; node = previous.get(node)) {
                    path.add(node); // Add each node in the path
                }
                Collections.reverse(path); // Reverse the path to go from source to target
                System.out.println("Path found with " + path.size() + " nodes");
                return distances.get(current); // Return the shortest distance to the target node
            }

            // If the current node's distance is still infinity, then the remaining nodes
            // are unreachable
            if (distances.get(current) == Double.POSITIVE_INFINITY) {
                break;
            }

            // Iterate over all neighbors of the current node
            for (Long neighbor : network.adjacency.getOrDefault(current, Collections.emptyList())) {
                // Get the distance (or weight) of the edge between the current node and this
                // neighbor
                double edgeLength = network.getEdgeLength(current, neighbor);

                // Calculate the total distance to the neighbor via the current node
                double newDist = distances.get(current) + edgeLength;

                // If the newly calculated distance is shorter than the previously recorded
                // distance
                if (newDist < distances.get(neighbor)) {
                    // Update the shortest known distance to this neighbor
                    distances.put(neighbor, newDist);

                    // Record the current node as the one leading to this neighbor in the shortest
                    // path
                    previous.put(neighbor, current);

                    // Add the neighbor to the queue to potentially process its neighbors later
                    queue.add(neighbor);
                }
            }
        }

        // If we exit the loop without finding the target node, then no path exists
        throw new RuntimeException("No path exists between the nodes");
    }

    // Calculates great-circle distance (Haversine formula) between two coordinates
    private static double calculateDistance(Coordinate c1, Coordinate c2) {
        final int R = 6371; // Earth radius in kilometers

        double latDistance = Math.toRadians(c2.lat - c1.lat);
        double lonDistance = Math.toRadians(c2.lon - c1.lon);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(c1.lat)) * Math.cos(Math.toRadians(c2.lat))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c * 1000; // Convert to meters
    }
}

import java.io.*;
import java.util.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

class State {
    String name;
    double latitude;
    double longitude;
    Map<String, Integer> demands;
    List<Edge> adjacentStates;

    public State(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.demands = new HashMap<>();
        this.adjacentStates = new ArrayList<>();
    }
}

class Edge {
    State destination;
    int distance;

    public Edge(State destination, int distance) {
        this.destination = destination;
        this.distance = distance;
    }
}

class FoodItem {
    String name;
    double pricePerUnit;
    int growingTime;
    double productionCost;
    double weight;
    double transportCostPerMile;

    public FoodItem(String name, double pricePerUnit, int growingTime,
                    double productionCost, double weight, double transportCostPerMile) {
        this.name = name;
        this.pricePerUnit = pricePerUnit;
        this.growingTime = growingTime;
        this.productionCost = productionCost;
        this.weight = weight;
        this.transportCostPerMile = transportCostPerMile;
    }
}

class StateGraph {
    private Map<String, State> states;
    private Map<String, Map<String, Integer>> routes;
    private String currentState;
    private static final String STATES_MARKER = "// states.csv";
    private static final String ROUTES_MARKER = "// routes.csv";
    private static final String FOOD_MARKER = "// food_items.csv";
    private static final String DEMANDS_MARKER = "// state_demands.csv";

    public StateGraph() {
        states = new HashMap<>();
        routes = new HashMap<>();
    }

    public void setCurrentState(String state) {
        if (states.containsKey(state)) {
            this.currentState = state;
        } else {
            throw new IllegalArgumentException("Invalid state: " + state);
        }
    }

    public String getCurrentState() {
        return currentState;
    }

    public void loadFromTextFile(String filename, ProfitOptimizer optimizer) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            String currentSection = "";
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                if (line.startsWith("//")) {
                    currentSection = line;
                    isFirstLine = true;
                    continue;
                }

                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header lines
                }

                switch (currentSection) {
                    case STATES_MARKER:
                        processStateLine(line);
                        break;
                    case ROUTES_MARKER:
                        processRouteLine(line);
                        break;
                    case FOOD_MARKER:
                        optimizer.loadFoodItems(line);
                        break;
                    case DEMANDS_MARKER:
                        optimizer.loadStateDemand(line);
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processStateLine(String line) {
        String[] data = line.split(",");
        String stateName = data[0];
        double latitude = Double.parseDouble(data[1]);
        double longitude = Double.parseDouble(data[2]);
        states.put(stateName, new State(stateName, latitude, longitude));
    }

    private void processRouteLine(String line) {
        String[] data = line.split(",");
        String fromState = data[0];
        String toState = data[1];
        int distance = Integer.parseInt(data[2]);
        addRoute(fromState, toState, distance);
    }

    public void addRoute(String fromState, String toState, int distance) {
        routes.putIfAbsent(fromState, new HashMap<>());
        routes.putIfAbsent(toState, new HashMap<>());

        routes.get(fromState).put(toState, distance);
        routes.get(toState).put(fromState, distance);

        State from = states.get(fromState);
        State to = states.get(toState);

        from.adjacentStates.add(new Edge(to, distance));
        to.adjacentStates.add(new Edge(from, distance));
    }

    public void addUserDefinedRoute(Scanner scanner) {
        System.out.println("\nAvailable states:");
        states.keySet().forEach(state -> System.out.println("- " + state));

        System.out.print("\nEnter first state name: ");
        String fromState = scanner.nextLine();

        System.out.print("Enter second state name: ");
        String toState = scanner.nextLine();

        System.out.print("Enter distance between states (in km): ");
        int distance = Integer.parseInt(scanner.nextLine());

        if (!states.containsKey(fromState) || !states.containsKey(toState)) {
            System.out.println("One or both states not found!");
            return;
        }

        addRoute(fromState, toState, distance);
        System.out.println("Route added successfully!");
    }

    public void printRoutes() {
        System.out.println("\nCurrent routes in the graph:");
        for (Map.Entry<String, Map<String, Integer>> fromEntry : routes.entrySet()) {
            String fromState = fromEntry.getKey();
            for (Map.Entry<String, Integer> toEntry : fromEntry.getValue().entrySet()) {
                String toState = toEntry.getKey();
                int distance = toEntry.getValue();
                if (fromState.compareTo(toState) < 0) {
                    System.out.printf("%s -> %s (%d km)\n", fromState, toState, distance);
                }
            }
        }
    }

    public int getShortestPathDistance(String start, String end) {
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previousStates = new HashMap<>();
        PriorityQueue<String> queue = new PriorityQueue<>(
                (a, b) -> distances.getOrDefault(a, Integer.MAX_VALUE) -
                        distances.getOrDefault(b, Integer.MAX_VALUE));

        // Initialize distances
        for (String state : states.keySet()) {
            distances.put(state, Integer.MAX_VALUE);
        }
        distances.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            if (current.equals(end)) break;

            if (!routes.containsKey(current)) continue;

            for (Map.Entry<String, Integer> neighbor : routes.get(current).entrySet()) {
                String nextState = neighbor.getKey();
                int newDist = distances.get(current) + neighbor.getValue();

                if (newDist < distances.get(nextState)) {
                    distances.put(nextState, newDist);
                    previousStates.put(nextState, current);
                    queue.add(nextState);
                }
            }
        }

        return distances.get(end);
    }

    public void printShortestPath(String start, String end) {
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previousStates = new HashMap<>();
        PriorityQueue<String> queue = new PriorityQueue<>(
                (a, b) -> distances.getOrDefault(a, Integer.MAX_VALUE) -
                        distances.getOrDefault(b, Integer.MAX_VALUE));

        // Initialize distances
        for (String state : states.keySet()) {
            distances.put(state, Integer.MAX_VALUE);
        }
        distances.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            if (current.equals(end)) break;

            if (!routes.containsKey(current)) continue;

            for (Map.Entry<String, Integer> neighbor : routes.get(current).entrySet()) {
                String nextState = neighbor.getKey();
                int newDist = distances.get(current) + neighbor.getValue();

                if (newDist < distances.get(nextState)) {
                    distances.put(nextState, newDist);
                    previousStates.put(nextState, current);
                    queue.add(nextState);
                }
            }
        }

        // Reconstruct path
        List<String> path = new ArrayList<>();
        String current = end;
        while (current != null) {
            path.add(0, current);
            current = previousStates.get(current);
        }

        System.out.println("Route: " + String.join(" -> ", path));
        System.out.println("Total distance: " + distances.get(end) + " km");
    }
}

class ProfitOptimizer {
    private static final double DAILY_DISTANCE_LIMIT = 900.0; // km per day
    private static final int MAX_DELIVERY_DAYS = 7;
    private Map<String, FoodItem> foodItems;
    private Map<String, Map<String, Integer>> stateDemands;
    private Map<String, TransportPlan> optimalPlans;

    public ProfitOptimizer() {
        foodItems = new HashMap<>();
        stateDemands = new HashMap<>();
        optimalPlans = new HashMap<>();
    }

    public void printOptimalSolution(StateGraph graph, FileWriter writer) {
        if (optimalPlans.isEmpty()) {
            System.out.println("No optimal plans found.");
            return;
        }

        try {
            System.out.println("\nOptimal Transport Plans:");
            writer.write("\nOptimal Transport Plans:\n");

            for (Map.Entry<String, TransportPlan> entry : optimalPlans.entrySet()) {
                TransportPlan plan = entry.getValue();
                System.out.println("Destination: " + plan.destination);
                writer.write("Destination: " + plan.destination + "\n");

                System.out.println("  Total Profit: $" + String.format("%.2f", plan.totalProfit));
                writer.write("  Total Profit: $" + String.format("%.2f", plan.totalProfit) + "\n");

                System.out.println("  Estimated Delivery Days: " + plan.estimatedDays);
                writer.write("  Estimated Delivery Days: " + plan.estimatedDays + "\n");

                System.out.println("  Total Weight: " + String.format("%.2f", plan.totalWeight) + " kg");
                writer.write("  Total Weight: " + String.format("%.2f", plan.totalWeight) + " kg\n");

                System.out.println("  Items to Transport:");
                writer.write("  Items to Transport:\n");

                for (Map.Entry<String, Integer> itemEntry : plan.itemQuantities.entrySet()) {
                    System.out.println("    " + itemEntry.getKey() + ": " + itemEntry.getValue() + " units");
                    writer.write("    " + itemEntry.getKey() + ": " + itemEntry.getValue() + " units\n");
                }

                System.out.println();
                writer.write("\n");
            }
        } catch (IOException e) {
            System.err.println("Error writing to output file: " + e.getMessage());
        }
    }

    static class TransportPlan {
        String destination;
        Map<String, Integer> itemQuantities;
        double totalProfit;
        int estimatedDays;
        double totalWeight;

        public TransportPlan() {
            this.itemQuantities = new HashMap<>();
            this.totalProfit = 0.0;
            this.estimatedDays = 0;
            this.totalWeight = 0.0;
        }
    }

    static class KnapsackItem {
        String foodName;
        String destination;
        int quantity;
        double weight;
        double profit;
        int deliveryDays;

        public KnapsackItem(String foodName, String destination, int quantity,
                            double weight, double profit, int deliveryDays) {
            this.foodName = foodName;
            this.destination = destination;
            this.quantity = quantity;
            this.weight = weight;
            this.profit = profit;
            this.deliveryDays = deliveryDays;
        }
    }

    public void optimizeDistribution(String farmerState, int vehicleCapacity, StateGraph graph) {
        if (farmerState == null || vehicleCapacity <= 0 || graph == null) {
            System.err.println("Invalid input parameters for optimization");
            return;
        }

        optimalPlans.clear();
        List<KnapsackItem> allPossibleItems = generatePossibleItems(farmerState, vehicleCapacity, graph);

        if (allPossibleItems.isEmpty()) {
            System.out.println("No valid items found for optimization");
            return;
        }

        try {
            solveKnapsack(allPossibleItems, vehicleCapacity);
        } catch (Exception e) {
            System.err.println("Error during knapsack optimization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<KnapsackItem> generatePossibleItems(String farmerState, int vehicleCapacity, StateGraph graph) {
        List<KnapsackItem> items = new ArrayList<>();

        for (String destState : stateDemands.keySet()) {
            if (destState.equals(farmerState)) continue;

            int distance = graph.getShortestPathDistance(farmerState, destState);
            if (distance == Integer.MAX_VALUE) continue;

            int deliveryDays = calculateDeliveryDays(distance);
            if (deliveryDays > MAX_DELIVERY_DAYS) continue;

            Map<String, Integer> demands = stateDemands.get(destState);
            if (demands == null) continue;

            for (Map.Entry<String, Integer> demand : demands.entrySet()) {
                String foodName = demand.getKey();
                FoodItem food = foodItems.get(foodName);

                if (food == null) continue;

                int demandQuantity = demand.getValue();
                if (food.weight <= 0) continue;

                int maxQuantity = Math.min(demandQuantity, (int) Math.floor(vehicleCapacity / food.weight));
                if (maxQuantity <= 0) continue;

                double baseProfit = calculateBaseProfit(foodName, farmerState, destState, maxQuantity, graph);
                if (baseProfit <= 0) continue;

                double timeAdjustedProfit = calculateTimeDependentProfit(baseProfit, deliveryDays, food);

                KnapsackItem item = new KnapsackItem(foodName, destState, maxQuantity, food.weight * maxQuantity,
                        timeAdjustedProfit, deliveryDays);
                items.add(item);
            }
        }

        return items;
    }

    private void solveKnapsack(List<KnapsackItem> items, int capacity) {
        int n = items.size();
        double[][] dp = new double[n + 1][capacity + 1];
        boolean[][] selected = new boolean[n + 1][capacity + 1];

        for (int i = 1; i <= n; i++) {
            KnapsackItem item = items.get(i - 1);

            for (int w = 0; w <= capacity; w++) {
                dp[i][w] = dp[i - 1][w];

                if (item.weight <= w && item.weight > 0) {
                    int remainingCapacity = (int) (w - item.weight);
                    if (remainingCapacity >= 0) {
                        double includeItem = dp[i - 1][remainingCapacity] + item.profit;
                        if (includeItem > dp[i - 1][w]) {
                            dp[i][w] = includeItem;
                            selected[i][w] = true;
                        }
                    }
                }
            }
        }

        List<KnapsackItem> selectedItems = new ArrayList<>();
        int w = capacity;

        for (int i = n; i > 0 && w > 0; i--) {
            if (selected[i][w]) {
                KnapsackItem item = items.get(i - 1);
                if (item.weight <= w) {
                    selectedItems.add(item);
                    w -= item.weight;
                }
            }
        }

        createTransportPlans(selectedItems);
    }

    private void createTransportPlans(List<KnapsackItem> selectedItems) {
        Map<String, TransportPlan> plans = new HashMap<>();

        for (KnapsackItem item : selectedItems) {
            if (item == null) continue;

            plans.putIfAbsent(item.destination, new TransportPlan());
            TransportPlan plan = plans.get(item.destination);

            if (plan == null) continue;

            plan.destination = item.destination;
            plan.itemQuantities.put(item.foodName, item.quantity);
            plan.totalProfit += item.profit;
            plan.estimatedDays = Math.max(plan.estimatedDays, item.deliveryDays);
            plan.totalWeight += item.weight;
        }

        optimalPlans = plans;
    }

    private double calculateBaseProfit(String foodName, String fromState, String toState, int quantity, StateGraph graph) {
        FoodItem food = foodItems.get(foodName);
        if (food == null) return 0;

        int distance = graph.getShortestPathDistance(fromState, toState);
        if (distance == Integer.MAX_VALUE || distance <= 0) return 0;

        try {
            double productionCost = food.productionCost * quantity;
            double transportCost = food.transportCostPerMile * distance * quantity * food.weight;
            double revenue = food.pricePerUnit * quantity;

            return revenue - productionCost - transportCost;
        } catch (Exception e) {
            System.err.println("Error calculating profit: " + e.getMessage());
            return 0;
        }
    }

    private double calculateTimeDependentProfit(double baseProfit, int deliveryDays, FoodItem item) {
        if (baseProfit <= 0 || deliveryDays <= 0 || item == null) return 0;

        try {
            double freshnessDecay = Math.max(0, 1.0 - (deliveryDays * 0.1));
            double growingTimeFactor = Math.max(0, 1.0 - (item.growingTime / 100.0));
            return baseProfit * Math.max(0.5, freshnessDecay * growingTimeFactor);
        } catch (Exception e) {
            System.err.println("Error calculating time-dependent profit: " + e.getMessage());
            return 0;
        }
    }

    private int calculateDeliveryDays(double distance) {
        if (distance <= 0) return Integer.MAX_VALUE;
        return (int) Math.ceil(distance / DAILY_DISTANCE_LIMIT);
    }

    public void loadFoodItems(String line) {
        try {
            String[] data = line.split(",");
            if (data.length < 6) return;

            String name = data[0];
            double price = Double.parseDouble(data[1]);
            int growingTime = Integer.parseInt(data[2]);
            double prodCost = Double.parseDouble(data[3]);
            double weight = Double.parseDouble(data[4]);
            double transportCost = Double.parseDouble(data[5]);

            if (weight <= 0) {
                System.err.println("Invalid weight for food item: " + name);
                return;
            }

            foodItems.put(name, new FoodItem(name, price, growingTime, prodCost, weight, transportCost));
        } catch (Exception e) {
            System.err.println("Error loading food item: " + e.getMessage());
        }
    }

    public void loadStateDemand(String line) {
        try {
            String[] data = line.split(",");
            if (data.length < 3) return;

            String state = data[0];
            String food = data[1];
            int demand = Integer.parseInt(data[2]);

            if (demand <= 0) {
                System.err.println("Invalid demand for state " + state + ": " + demand);
                return;
            }

            stateDemands.putIfAbsent(state, new HashMap<>());
            stateDemands.get(state).put(food, demand);
        } catch (Exception e) {
            System.err.println("Error loading state demand: " + e.getMessage());
        }
    }
}

public class Main {
    public static void main(String[] args) {
        StateGraph graph = new StateGraph();
        ProfitOptimizer optimizer = new ProfitOptimizer();
        Scanner scanner = new Scanner(System.in);


        // Create JFileChooser for file selection
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Data File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));


        String filePath = null;
        while (filePath == null) {
            System.out.println("\nWelcome to Farmer's Profit Optimization System!");
            System.out.println("===========================================");
            System.out.println("\nPlease select your data file using the file chooser dialog.");

            int result = fileChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                filePath = selectedFile.getAbsolutePath();
                System.out.println("Selected file: " + filePath);
            } else {
                System.out.println("No file selected. Using default 'data.txt'");
                filePath = "data.txt";
                File defaultFile = new File(filePath);
                if (!defaultFile.exists()) {
                    System.out.println("Default file not found: " + defaultFile.getAbsolutePath());
                    System.out.println("Please select a valid file.");
                    filePath = null;
                    continue;
                }
            }
        }

        // Load data from selected file
        graph.loadFromTextFile(filePath, optimizer);

        System.out.println("\nWelcome to Farmer's Profit Optimization System!");
        System.out.println("===========================================");

        System.out.print("\nEnter farmer's current state: ");
        String farmerState = scanner.nextLine();
        graph.setCurrentState(farmerState);

        System.out.print("Enter vehicle capacity (in kg): ");
        int vehicleCapacity = scanner.nextInt();
        scanner.nextLine(); // consume newline

        // Initial optimization
        optimizer.optimizeDistribution(farmerState, vehicleCapacity, graph);

        while (true) {
            System.out.println("\nOptions:");
            System.out.println("1. Change farmer's location");
            System.out.println("2. Change vehicle capacity");
            System.out.println("3. View current routes");
            System.out.println("4. Add new route");
            System.out.println("5. Recalculate optimal distribution");
            System.out.println("6. Exit");
            System.out.print("Enter choice: ");

            JFileChooser outputFileChooser = new JFileChooser();
            outputFileChooser.setDialogTitle("Select Output File Location");
            outputFileChooser.setSelectedFile(new File("output.txt"));

            String outputFilePath = null;
            while (outputFilePath == null) {
                int outputResult = outputFileChooser.showSaveDialog(null);

                if (outputResult == JFileChooser.APPROVE_OPTION) {
                    File selectedOutputFile = outputFileChooser.getSelectedFile();
                    outputFilePath = selectedOutputFile.getAbsolutePath();
                    System.out.println("Output file selected: " + outputFilePath);
                    break;
                } else {
                    System.out.println("No output file selected. Exiting the program.");
                    scanner.close();
                    System.exit(0);
                }
            }

            // Use the selected output file path
            try (FileWriter writer = new FileWriter(new File(outputFilePath))) {
                optimizer.optimizeDistribution(farmerState, vehicleCapacity, graph);
                optimizer.printOptimalSolution(graph, writer);
                System.out.println("\nOutput written to: " + outputFilePath);
            } catch (IOException e) {
                System.err.println("Error writing to output file: " + e.getMessage());
            }

            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            switch (choice) {
                case 1:
                    System.out.print("Enter new state: ");
                    farmerState = scanner.nextLine();
                    graph.setCurrentState(farmerState);
                    optimizer.optimizeDistribution(farmerState, vehicleCapacity, graph);
                case 2:
                    System.out.print("Enter new vehicle capacity (in kg): ");
                    vehicleCapacity = scanner.nextInt();
                    scanner.nextLine(); // consume newline
                    optimizer.optimizeDistribution(farmerState, vehicleCapacity, graph);



                case 3:
                    graph.printRoutes();
                    break;

                case 4:
                    graph.addUserDefinedRoute(scanner);
                    optimizer.optimizeDistribution(farmerState, vehicleCapacity, graph);


                case 5:
                    optimizer.optimizeDistribution(farmerState, vehicleCapacity, graph);

                case 6:
                    System.out.println("\nThank you for using Farmer's Profit Optimization System!");
                    try (FileWriter writer = new FileWriter(new File(outputFilePath))) {
                        optimizer.printOptimalSolution(graph, writer);
                        System.out.println("\nOutput written to: " + outputFilePath);
                    } catch (IOException e) {
                        System.err.println("Error writing to output file: " + e.getMessage());
                    }
                    scanner.close();
                    System.exit(0);
                    break;

                default:
                    System.out.println("Invalid choice! Please try again.");
            }
        }
    }
}
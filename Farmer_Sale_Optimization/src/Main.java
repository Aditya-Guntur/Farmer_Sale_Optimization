import java.io.*;
import java.util.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;


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

    public void printShortestPath(String start, String end, PrintWriter outputWriter) {
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

        outputWriter.println("Route: " + String.join(" -> ", path));
        outputWriter.println("Total distance: " + distances.get(end) + " km");
    }

}

class ProfitOptimizer {
    private static final double DAILY_DISTANCE_LIMIT = 900.0; // km per day
    private static final int MAX_DELIVERY_DAYS = 7;
    private Map<String, FoodItem> foodItems;
    private Map<String, Map<String, Integer>> stateDemands;
    private Map<String, TransportPlan> optimalPlans;

    static class TransportPlan {
        String destination;
        Map<String, Integer> itemQuantities; // food item -> quantity
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
    public ProfitOptimizer() {
        foodItems = new HashMap<>();
        stateDemands = new HashMap<>();
        optimalPlans = new HashMap<>();
    }

    public void loadFoodItems(String line) {
        try {
            String[] data = line.split(",");
            if (data.length < 6) {
                System.err.println("Insufficient data for food item: " + line);
                return;
            }

            String name = data[0];
            double weight = Double.parseDouble(data[4]);
            if (weight <= 0) {
                System.err.println("Invalid weight for food item: " + name);
                return;
            }

            // Other parsing remains the same
            foodItems.put(name, new FoodItem(name, Double.parseDouble(data[1]), Integer.parseInt(data[2]),
                    Double.parseDouble(data[3]), weight, Double.parseDouble(data[5])));
        } catch (NumberFormatException e) {
            System.err.println("Error parsing weight or other fields: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error loading food item: " + e.getMessage());
        }
    }


    public void loadStateDemand(String line) {
        String[] data = line.split(",");
        String state = data[0];
        String food = data[1];
        int demand = Integer.parseInt(data[2]);

        stateDemands.putIfAbsent(state, new HashMap<>());
        stateDemands.get(state).put(food, demand);
    }

    private int calculateDeliveryDays(double distance) {
        return (int) Math.ceil(distance / DAILY_DISTANCE_LIMIT);
    }

    private double calculateTimeDependentProfit(double baseProfit, int deliveryDays, FoodItem item) {
        // Apply freshness decay based on delivery time and growing time
        double freshnessDecay = 1.0 - (deliveryDays * 0.1); // 10% decay per day
        // Items with longer growing times are more resilient to transport time
        freshnessDecay *= (1.0 - (item.growingTime / 100.0));
        return baseProfit * Math.max(0.5, freshnessDecay);
    }

    public void optimizeDistribution(String farmerState, int vehicleCapacity, StateGraph graph) {
        optimalPlans.clear();
        List<KnapsackItem> allPossibleItems = new ArrayList<>();

        // Generate all possible transport combinations
        for (String destState : stateDemands.keySet()) {
            if (destState.equals(farmerState)) continue;

            int distance = graph.getShortestPathDistance(farmerState, destState);
            int deliveryDays = calculateDeliveryDays(distance);

            // Skip if delivery would take too long
            if (deliveryDays > MAX_DELIVERY_DAYS) continue;

            for (Map.Entry<String, Integer> demand : stateDemands.get(destState).entrySet()) {
                String foodName = demand.getKey();
                int demandQuantity = demand.getValue();
                FoodItem food = foodItems.get(foodName);

                // Calculate maximum quantity based on vehicle capacity
                int maxQuantity = Math.min(demandQuantity,
                        (int)(vehicleCapacity / food.weight));

                if (maxQuantity > 0) {
                    double baseProfit = calculateBaseProfit(foodName, farmerState,
                            destState, maxQuantity, graph);
                    double timeAdjustedProfit = calculateTimeDependentProfit(baseProfit,
                            deliveryDays, food);

                    // Create knapsack item
                    KnapsackItem item = new KnapsackItem(
                            foodName,
                            destState,
                            maxQuantity,
                            food.weight * maxQuantity,
                            timeAdjustedProfit,
                            deliveryDays
                    );

                    allPossibleItems.add(item);
                }
            }
        }

        // Solve knapsack problem
        solveKnapsack(allPossibleItems, vehicleCapacity);
    }

    private double calculateBaseProfit(String foodName, String fromState,
                                       String toState, int quantity, StateGraph graph) {
        FoodItem food = foodItems.get(foodName);
        if (food == null) return 0;

        int distance = graph.getShortestPathDistance(fromState, toState);
        if (distance == Integer.MAX_VALUE) return 0;

        double productionCost = food.productionCost * quantity;
        double transportCost = food.transportCostPerMile * distance * quantity * food.weight;
        double revenue = food.pricePerUnit * quantity;

        return revenue - productionCost - transportCost;
    }

    private void solveKnapsack(List<KnapsackItem> items, int capacity) {
        int n = items.size();
        double[][] dp = new double[n + 1][capacity + 1];
        boolean[][] selected = new boolean[n + 1][capacity + 1];

        // Fill the dp table
        for (int i = 1; i <= n; i++) {
            KnapsackItem item = items.get(i - 1);
            for (int w = 0; w <= capacity; w++) {
                if (item.weight <= w) {
                    double includeItem = dp[i - 1][(int)(w - item.weight)] + item.profit;
                    if (includeItem > dp[i - 1][w]) {
                        dp[i][w] = includeItem;
                        selected[i][w] = true;
                    } else {
                        dp[i][w] = dp[i - 1][w];
                    }
                } else {
                    dp[i][w] = dp[i - 1][w];
                }
            }
        }

        // Reconstruct solution
        List<KnapsackItem> selectedItems = new ArrayList<>();
        int w = capacity;
        for (int i = n; i > 0; i--) {
            if (selected[i][w]) {
                selectedItems.add(items.get(i - 1));
                w -= items.get(i - 1).weight;
            }
        }

        // Create transport plans from selected items
        createTransportPlans(selectedItems);
    }

    private void createTransportPlans(List<KnapsackItem> selectedItems) {
        Map<String, TransportPlan> plans = new HashMap<>();

        for (KnapsackItem item : selectedItems) {
            plans.putIfAbsent(item.destination, new TransportPlan());
            TransportPlan plan = plans.get(item.destination);

            plan.destination = item.destination;
            plan.itemQuantities.put(item.foodName, item.quantity);
            plan.totalProfit += item.profit;
            plan.estimatedDays = Math.max(plan.estimatedDays, item.deliveryDays);
            plan.totalWeight += item.weight;
        }

        optimalPlans = plans;
    }

    public void printOptimalSolution(StateGraph graph,PrintWriter outputWriter) {
        outputWriter.println("\nOptimal Distribution Plan with Time Constraints:");
        outputWriter.println("=============================================");

        double totalProfit = 0;

        for (TransportPlan plan : optimalPlans.values()) {
            outputWriter.printf("\nDestination: %s\n", plan.destination);
            outputWriter.printf("Estimated Delivery Time: %d days\n", plan.estimatedDays);
            outputWriter.printf("Total Load Weight: %.2f kg\n", plan.totalWeight);
            outputWriter.println("\nItems to Transport:");

            for (Map.Entry<String, Integer> entry : plan.itemQuantities.entrySet()) {
                String foodName = entry.getKey();
                int quantity = entry.getValue();
                FoodItem food = foodItems.get(foodName);

                outputWriter.printf("- %s: %d units (%.2f kg)\n",
                        foodName, quantity, quantity * food.weight);
            }

            outputWriter.printf("Expected Profit: ₹%.2f\n", plan.totalProfit);
            outputWriter.println("\nRecommended Route:");
            graph.printShortestPath(graph.getCurrentState(), plan.destination, outputWriter);

            totalProfit += plan.totalProfit;
        }

        outputWriter.printf("\nTotal Expected Profit: ₹%.2f\n", totalProfit);
        outputWriter.println("\nNote: All deliveries are scheduled within the 7-day freshness window");
        outputWriter.println("Daily distance limit of 400km has been considered for delivery time calculations");
        outputWriter.flush();
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
        scanner.nextLine();



        //String currentDir = System.getProperty("user.dir");
        String outputFilePath = "output.txt";

        try (PrintWriter outputWriter = new PrintWriter(outputFilePath)) {
            // Initial optimization
            optimizer.optimizeDistribution(farmerState, vehicleCapacity, graph);
            optimizer.printOptimalSolution(graph, outputWriter);

            while (true) {
                System.out.println("\nOptions:");
                System.out.println("1. Change farmer's location");
                System.out.println("2. Change vehicle capacity");
                System.out.println("3. View current routes");
                System.out.println("4. Add new route");
                System.out.println("5. Recalculate optimal distribution");
                System.out.println("6. Exit");
                System.out.print("Enter choice: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); // consume newline

                switch (choice) {
                    case 1:
                        System.out.print("Enter new state: ");
                        farmerState = scanner.nextLine();
                        graph.setCurrentState(farmerState);
                        optimizer.optimizeDistribution(farmerState, vehicleCapacity, graph);
                        optimizer.printOptimalSolution(graph,outputWriter);
                        break;

                    case 2:
                        System.out.print("Enter new vehicle capacity (in kg): ");
                        vehicleCapacity = scanner.nextInt();
                        scanner.nextLine(); // consume newline
                        optimizer.optimizeDistribution(farmerState, vehicleCapacity, graph);
                        optimizer.printOptimalSolution(graph,outputWriter);
                        break;

                    case 3:
                        graph.printRoutes();
                        break;

                    case 4:
                        graph.addUserDefinedRoute(scanner);
                        optimizer.optimizeDistribution(farmerState, vehicleCapacity, graph);
                        optimizer.printOptimalSolution(graph,outputWriter);
                        break;

                    case 5:
                        optimizer.optimizeDistribution(farmerState, vehicleCapacity, graph);
                        optimizer.printOptimalSolution(graph,outputWriter);
                        break;

                    case 6:
                        System.out.println("\nThank you for using Farmer's Profit Optimization System!");
                        scanner.close();
                        System.exit(0);
                        break;

                    default:
                        System.out.println("Invalid choice! Please try again.");
                }
            }
        }catch (IOException e) {
            // Handle the exception, e.g., log the error or provide a user-friendly message
            System.out.println("Error creating output file: " + e.getMessage());
        }
    } }

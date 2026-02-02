package VTUF3D.ConfigMaker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory class for creating and managing tree configurations.
 * This class simplifies the creation of multiple tree configurations
 * similar to the example directory structure (dirs 1, 2, 3, 4, 5, etc.).
 *
 * Example usage:
 * <pre>
 *   TreeConfigurationFactory factory = new TreeConfigurationFactory(5.0);
 *
 *   // Create configs similar to example directories
 *   List&lt;TreeParametersConfig&gt; configs = factory.createStandardConfigurations();
 *
 *   // Or create custom configurations
 *   factory.addConfiguration("SmallOlive",
 *       TreeParametersConfig.createOliveConfig(5.0, 5.0));
 * </pre>
 */
public class TreeConfigurationFactory {

    private double gridSizeInMeters;
    private Map<String, TreeParametersConfig> configurations;
    private List<String> configurationOrder;

    /**
     * Constructor with grid size
     * @param gridSizeInMeters Grid size in meters (typically 5.0)
     */
    public TreeConfigurationFactory(double gridSizeInMeters) {
        this.gridSizeInMeters = gridSizeInMeters;
        this.configurations = new HashMap<>();
        this.configurationOrder = new ArrayList<>();
    }

    /**
     * Create standard configurations matching the example directory structure:
     * Config 1: Grass (0.2m height)
     * Config 2: Small Olive tree (5m height)
     * Config 3: Small Brushbox tree (5m height)
     * Config 4: Large Olive tree (10m height)
     * Config 5: Large Brushbox tree (10m height)
     *
     * @return List of tree parameter configurations
     */
    public List<TreeParametersConfig> createStandardConfigurations() {
        List<TreeParametersConfig> configs = new ArrayList<>();

        // Configuration 1: Grass layer
        TreeParametersConfig config1 = TreeParametersConfig.createGrassConfig(gridSizeInMeters);
        addConfiguration("Grass", config1);
        configs.add(config1);

        // Configuration 2: Small Olive tree (5m total height)
        TreeParametersConfig config2 = TreeParametersConfig.createOliveConfig(5.0, gridSizeInMeters);
        config2.setTitle("Single olive tree IN 1x1 metre plot.");
        addConfiguration("SmallOlive", config2);
        configs.add(config2);

        // Configuration 3: Small Brushbox tree (5m total height)
        TreeParametersConfig config3 = TreeParametersConfig.createBrushboxConfig(5.0, gridSizeInMeters);
        addConfiguration("SmallBrushbox", config3);
        configs.add(config3);

        // Configuration 4: Large Olive tree (10m total height)
        TreeParametersConfig config4 = TreeParametersConfig.createOliveConfig(10.0, gridSizeInMeters);
        addConfiguration("LargeOlive", config4);
        configs.add(config4);

        // Configuration 5: Large Brushbox tree (10m total height)
        TreeParametersConfig config5 = TreeParametersConfig.createBrushboxConfig(10.0, gridSizeInMeters);
        addConfiguration("LargeBrushbox", config5);
        configs.add(config5);

        return configs;
    }

    /**
     * Create configurations matching the PrestonBaseSmall example (2 configs)
     * Config 1: Small Olive tree
     * Config 2: Small Olive tree (duplicate for testing different shading)
     *
     * @return List of tree parameter configurations
     */
    public List<TreeParametersConfig> createPrestonBaseSmallConfigurations() {
        List<TreeParametersConfig> configs = new ArrayList<>();

        // Configuration 1 & 2: Same small olive trees with different shading
        TreeParametersConfig config1 = TreeParametersConfig.createOliveConfig(5.0, gridSizeInMeters);
        config1.setTitle("Single olive tree IN 1x1 metre plot.");
        addConfiguration("OliveTree", config1);
        configs.add(config1);

        return configs;
    }

    /**
     * Add a custom tree configuration
     * @param name Configuration name (for reference)
     * @param config Tree parameters configuration
     */
    public void addConfiguration(String name, TreeParametersConfig config) {
        configurations.put(name, config);
        if (!configurationOrder.contains(name)) {
            configurationOrder.add(name);
        }
    }

    /**
     * Get configuration by name
     * @param name Configuration name
     * @return Tree parameters configuration or null if not found
     */
    public TreeParametersConfig getConfiguration(String name) {
        return configurations.get(name);
    }

    /**
     * Get all configurations in the order they were added
     * @return List of all configurations
     */
    public List<TreeParametersConfig> getAllConfigurations() {
        List<TreeParametersConfig> configs = new ArrayList<>();
        for (String name : configurationOrder) {
            configs.add(configurations.get(name));
        }
        return configs;
    }

    /**
     * Get configuration names in order
     * @return List of configuration names
     */
    public List<String> getConfigurationNames() {
        return new ArrayList<>(configurationOrder);
    }

    /**
     * Get number of configurations
     * @return Number of configurations
     */
    public int getConfigurationCount() {
        return configurations.size();
    }

    /**
     * Create a set of tree configurations with varying heights
     * @param baseType Tree type ("olive", "brushbox", "grass")
     * @param heights Array of heights in meters
     * @return List of tree parameter configurations
     */
    public List<TreeParametersConfig> createHeightVariations(String baseType, double[] heights) {
        List<TreeParametersConfig> configs = new ArrayList<>();

        for (int i = 0; i < heights.length; i++) {
            TreeParametersConfig config;
            String name = baseType + "_" + heights[i] + "m";

            switch (baseType.toLowerCase()) {
                case "olive":
                    config = TreeParametersConfig.createOliveConfig(heights[i], gridSizeInMeters);
                    break;
                case "brushbox":
                    config = TreeParametersConfig.createBrushboxConfig(heights[i], gridSizeInMeters);
                    break;
                case "grass":
                    config = TreeParametersConfig.createGrassConfig(gridSizeInMeters);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown tree type: " + baseType);
            }

            addConfiguration(name, config);
            configs.add(config);
        }

        return configs;
    }

    /**
     * Print summary of all configurations
     */
    public void printConfigurationSummary() {
        System.out.println("=== Tree Configuration Summary ===");
        System.out.println("Grid size: " + gridSizeInMeters + " meters");
        System.out.println("Total configurations: " + configurations.size());
        System.out.println();

        int index = 1;
        for (String name : configurationOrder) {
            TreeParametersConfig config = configurations.get(name);
            System.out.println("Configuration " + index + " [" + name + "]:");
            System.out.println("  " + config);
            index++;
        }
    }

    /**
     * Create tree configurations similar to PrestonBase8 example structure
     * @return Map of configuration number to TreeParametersConfig
     */
    public Map<Integer, TreeParametersConfig> createPrestonBase8Configurations() {
        Map<Integer, TreeParametersConfig> configMap = new HashMap<>();

        // Config 1: Grass (0.2m)
        configMap.put(1, TreeParametersConfig.createGrassConfig(gridSizeInMeters));

        // Config 2: Small Olive (5m)
        TreeParametersConfig olive5m = TreeParametersConfig.createOliveConfig(5.0, gridSizeInMeters);
        olive5m.setTitle("Single olive tree IN 1x1 metre plot.");
        configMap.put(2, olive5m);

        // Config 3: Small Brushbox (5m)
        configMap.put(3, TreeParametersConfig.createBrushboxConfig(5.0, gridSizeInMeters));

        // Config 4: Large Olive (10m)
        configMap.put(4, TreeParametersConfig.createOliveConfig(10.0, gridSizeInMeters));

        // Config 5: Large Brushbox (10m)
        configMap.put(5, TreeParametersConfig.createBrushboxConfig(10.0, gridSizeInMeters));

        return configMap;
    }

    /**
     * Apply a tree configuration to MaespaConfigConfileDat
     * This method sets all the tree parameters in the MAESPA configuration
     *
     * @param maespaConfig The MAESPA configuration object to update
     * @param treeConfig The tree parameters to apply
     * @param numberOfTrees Number of trees (typically 1)
     */
    public void applyConfigurationToMaespa(MaespaConfigConfileDat maespaConfig,
                                           TreeParametersConfig treeConfig,
                                           int numberOfTrees) {

        // Set plot dimensions
        maespaConfig.setConfigTreex(0);
        maespaConfig.setConfigTreey(0);
        maespaConfig.setConfigTreexmax((int)treeConfig.getPlotXMax());
        maespaConfig.setConfigTreeymax((int)treeConfig.getPlotYMax());
        maespaConfig.setConfigTreexslope(0);
        maespaConfig.setConfigTreeyslope(0);
        maespaConfig.setConfigTreebearing(0);
        maespaConfig.setConfigTreenotrees(numberOfTrees);

        // Set aerodynamic parameters
        maespaConfig.setConfigTreezht(treeConfig.getZht());
        maespaConfig.setConfigTreezpd(treeConfig.getZpd());
        maespaConfig.setConfigTreez0ht(treeConfig.getZ0ht());

        // Set tree species
        maespaConfig.setConfigTreeispecies("1");

        // Set tree position
        maespaConfig.setConfigTreexycoords(
            treeConfig.getTreeXPosition() + " " + treeConfig.getTreeYPosition());

        // Set tree title/description
        maespaConfig.setConfigTreeTitle(treeConfig.getTitle());

        // Set crown dimensions - X radius
        maespaConfig.setConfigTreezAllradxNodates(1);
        maespaConfig.setConfigTreezAllradxDates("");
        maespaConfig.setConfigTreezAllradxValues(treeConfig.getCrownRadiusX());

        // Set crown dimensions - Y radius
        maespaConfig.setConfigTreezAllradyNodates(1);
        maespaConfig.setConfigTreezAllradyDates("");
        maespaConfig.setConfigTreezAllradyValues(treeConfig.getCrownRadiusY());

        // Set crown height
        maespaConfig.setConfigTreezAllhtcrownNodates(1);
        maespaConfig.setConfigTreezAllhtcrownDates("");
        maespaConfig.setConfigTreezAllhtcrownValues(treeConfig.getCrownHeight());
        maespaConfig.setConfigTreezAllhtcrownValuesComment(" ");

        // Set stem diameter
        maespaConfig.setConfigTreezAlldiamNodates(1);
        maespaConfig.setConfigTreezAlldiamDates("");
        maespaConfig.setConfigTreezAlldiamValues(treeConfig.getStemDiameter());
        maespaConfig.setConfigTreezAlldiamValuesComment(" ");

        // Set trunk height
        maespaConfig.setConfigTreezAllhttrunkNodates(1);
        maespaConfig.setConfigTreezAllhttrunkDates("");
        maespaConfig.setConfigTreezAllhttrunkValues(treeConfig.getTrunkHeight());
        maespaConfig.setConfigTreezAllhttrunkValuesComment(" ");

        // Set leaf area
        maespaConfig.setConfigTreezAlllareaNodates(1);
        maespaConfig.setConfigTreezAlllareaDates("");
        maespaConfig.setConfigTreezAlllareaValues(treeConfig.getTotalLeafArea());

        // Create informative comment for leaf area
        String leafAreaComment = String.format(
            "!Total leaf area then is = LAI * tree area (radius^2*pi). LAI=%.2f; %.2f^2*pi=%.2f; => Total LA=%.2f",
            treeConfig.getLeafAreaIndex(),
            treeConfig.getCrownRadiusX(),
            treeConfig.getCanopyArea(),
            treeConfig.getTotalLeafArea()
        );
        maespaConfig.setConfigTreezAlllareaValuesComment(leafAreaComment);
    }

    /**
     * Example usage and demonstration
     */
    public static void main(String[] args) {
        System.out.println("=== Tree Configuration Factory Demo ===\n");

        // Create factory with 5-meter grid
        TreeConfigurationFactory factory = new TreeConfigurationFactory(5.0);

        // Create standard configurations (matching PrestonBase8 structure)
        System.out.println("Creating standard configurations...\n");
        List<TreeParametersConfig> configs = factory.createStandardConfigurations();

        // Print summary
        factory.printConfigurationSummary();

        System.out.println("\n=== Height Variations Example ===");
        // Create olive trees with different heights
        TreeConfigurationFactory heightFactory = new TreeConfigurationFactory(5.0);
        double[] heights = {3.0, 5.0, 7.0, 10.0, 15.0};
        List<TreeParametersConfig> oliveVariations =
            heightFactory.createHeightVariations("olive", heights);

        System.out.println("Created " + oliveVariations.size() + " olive tree variations:");
        for (int i = 0; i < oliveVariations.size(); i++) {
            System.out.println("  " + (i+1) + ". " + oliveVariations.get(i));
        }

        System.out.println("\n=== PrestonBase8 Configuration Map ===");
        Map<Integer, TreeParametersConfig> prestonConfigs =
            factory.createPrestonBase8Configurations();

        for (Map.Entry<Integer, TreeParametersConfig> entry : prestonConfigs.entrySet()) {
            System.out.println("Directory " + entry.getKey() + ": " +
                entry.getValue().getTreeTypeName() +
                " (height=" + entry.getValue().getTotalHeight() + "m)");
        }
    }

    // Getters
    public double getGridSizeInMeters() {
        return gridSizeInMeters;
    }

    public void setGridSizeInMeters(double gridSizeInMeters) {
        this.gridSizeInMeters = gridSizeInMeters;
    }
}

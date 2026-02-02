package VTUF3D.ConfigMaker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Tree Parameters Configuration class for flexible tree parametrization.
 * This class allows defining tree characteristics for different tree types,
 * similar to the example directories structure (dirs 1, 2, 3, etc.).
 *
 * Each tree configuration includes:
 * - Physical dimensions (crown height, trunk height, crown radii)
 * - Aerodynamic parameters (zpd, z0ht, zht)
 * - Physiological parameters (leaf area index, stomatal conductance, etc.)
 * - Species information
 */
public class TreeParametersConfig {

    private String treeTypeName;
    private String speciesName;
    private String title;

    // Physical dimensions
    private double crownHeight;           // Height of tree crown (meters)
    private double trunkHeight;           // Height of trunk (meters)
    private double crownRadiusX;          // Crown radius in X direction (meters)
    private double crownRadiusY;          // Crown radius in Y direction (meters)
    private double stemDiameter;          // Stem diameter (meters)

    // Plot dimensions
    private double plotXMax;              // Plot maximum X dimension (meters)
    private double plotYMax;              // Plot maximum Y dimension (meters)
    private double treeXPosition;         // Tree X coordinate in plot
    private double treeYPosition;         // Tree Y coordinate in plot

    // Aerodynamic parameters
    private double zht;                   // Height of wind speed measurements (meters)
    private double zpd;                   // Zero plane displacement (meters)
    private double z0ht;                  // Roughness length (meters)

    // Physiological parameters
    private double leafAreaIndex;         // Leaf Area Index (LAI)
    private double totalLeafArea;         // Total leaf area per tree (m²)

    // Physiological parameters for photosynthesis
    private double g0;                    // Minimum stomatal conductance
    private double g1;                    // Stomatal slope parameter
    private double jmax;                  // Maximum electron transport rate
    private double vcmax;                 // Maximum carboxylation rate
    private double specificLeafArea;      // Specific leaf area (m²/kg)

    // Reflectance and transmittance (PAR, NIR, Thermal)
    private String rhoSol;                // Soil reflectance (PAR, NIR, Thermal)
    private String aTau;                  // Leaf transmittance (adaxial)
    private String aRho;                  // Leaf reflectance (adaxial)

    // Water parameters
    private double minLeafWaterPotential; // Minimum leaf water potential (MPa)
    private double minRootWaterPotential; // Minimum root water potential (MPa)

    // Tree type identifier
    private int configTypeId;

    // Calculation method for derived parameters
    private boolean useAutomaticCalculation = true;

    /**
     * Default constructor with standard olive tree parameters
     */
    public TreeParametersConfig() {
        this("Olive Tree", "O.europaea", 1);
    }

    /**
     * Constructor with tree type name, species, and config type ID
     */
    public TreeParametersConfig(String treeTypeName, String speciesName, int configTypeId) {
        this.treeTypeName = treeTypeName;
        this.speciesName = speciesName;
        this.configTypeId = configTypeId;
        this.title = "Single " + treeTypeName + " in 5x5 metre plot.";

        // Set default values for olive tree
        initializeDefaults();
    }

    /**
     * Initialize default parameters (based on olive tree)
     */
    private void initializeDefaults() {
        // Plot dimensions
        this.plotXMax = 5.0;
        this.plotYMax = 5.0;
        this.treeXPosition = 2.5;
        this.treeYPosition = 2.5;

        // Default tree dimensions
        this.crownHeight = 3.75;
        this.trunkHeight = 1.25;
        this.crownRadiusX = 2.5;
        this.crownRadiusY = 2.5;
        this.stemDiameter = 0.05;

        // Aerodynamic parameters (will be auto-calculated if needed)
        this.zht = 40.0;
        this.zpd = crownHeight * 2.0 / 3.0;     // 2/3 rule
        this.z0ht = crownHeight / 10.0;          // 1/10 rule

        // Physiological parameters
        this.leafAreaIndex = 2.48;
        calculateTotalLeafArea();

        // Stomatal conductance (from Smith St. data for olive)
        this.g0 = 0.03;
        this.g1 = 2.615;

        // Reflectance (olive: Baldini et al 1997)
        this.rhoSol = "0.10\t0.05\t0.05";
        this.aTau = "0.01\t0.28\t0.01";
        this.aRho = "0.08\t0.42\t0.05";

        // Water parameters
        this.minLeafWaterPotential = -10.0;
        this.minRootWaterPotential = -3.0;
    }

    /**
     * Calculate total leaf area based on crown dimensions and LAI
     */
    private void calculateTotalLeafArea() {
        double canopyArea = crownRadiusX * crownRadiusY * Math.PI;
        this.totalLeafArea = leafAreaIndex * canopyArea;
    }

    /**
     * Calculate aerodynamic parameters from crown height
     */
    private void calculateAerodynamicParameters() {
        if (useAutomaticCalculation) {
            this.zpd = crownHeight * 2.0 / 3.0;   // 2/3 of crown height
            this.z0ht = crownHeight / 10.0;        // 1/10 of crown height

            // Ensure they are not zero (would cause crashes)
            if (this.zpd == 0) this.zpd = 0.066;
            if (this.z0ht == 0) this.z0ht = 0.02;
        }
    }

    /**
     * Calculate stem diameter based on tree height using allometric relationships
     * From: http://www.academicjournals.org/journal/AJB/article-abstract/96974C726212
     * and 10.1093/treephys/tps127
     */
    private void calculateStemDiameter() {
        if (useAutomaticCalculation) {
            double totalHeight = trunkHeight + crownHeight;
            if (totalHeight > 7) {
                this.stemDiameter = (totalHeight - 6.74) / 14.4;
            } else {
                this.stemDiameter = 0.05;
            }
        }
    }

    /**
     * Set tree dimensions from total height and grid size
     * Automatically calculates trunk height (25%) and crown height (75%)
     */
    public void setTreeDimensionsFromHeight(double totalHeight, double gridSizeInMeters) {
        this.trunkHeight = totalHeight * 0.25;
        this.crownHeight = totalHeight * 0.75;
        this.crownRadiusX = gridSizeInMeters * 0.5;
        this.crownRadiusY = gridSizeInMeters * 0.5;

        // Recalculate derived parameters
        calculateAerodynamicParameters();
        calculateStemDiameter();
        calculateTotalLeafArea();
    }

    /**
     * Create a grass configuration
     */
    public static TreeParametersConfig createGrassConfig(double gridSizeInMeters) {
        TreeParametersConfig config = new TreeParametersConfig("Grass", "Turf grass",
                TUFBldVegHeights.GRASS_CONFIG_TYPE);

        config.title = "Grass layer as a box tree on the ground covering the plot area.";
        config.crownHeight = 0.2;              // Blade length (Simmonds et al 2011)
        config.trunkHeight = 0.0;              // No trunk for grass
        config.crownRadiusX = gridSizeInMeters * 0.5;
        config.crownRadiusY = gridSizeInMeters * 0.5;
        config.stemDiameter = 0.2;             // N/A for grass

        config.zht = 4.0;
        config.zpd = 0.066;
        config.z0ht = 0.02;

        config.leafAreaIndex = 2.0;
        config.calculateTotalLeafArea();

        return config;
    }

    /**
     * Create an olive tree configuration
     */
    public static TreeParametersConfig createOliveConfig(double totalHeight, double gridSizeInMeters) {
        TreeParametersConfig config = new TreeParametersConfig("Olive Tree", "O.europaea",
                TUFBldVegHeights.OLIVE_CONFIG_TYPE);

        config.setTreeDimensionsFromHeight(totalHeight, gridSizeInMeters);
        config.title = "Single olive tree in " + (int)gridSizeInMeters + "x" +
                       (int)gridSizeInMeters + " metre plot.";

        return config;
    }

    /**
     * Create a brushbox (Lophostemon confertus) tree configuration
     */
    public static TreeParametersConfig createBrushboxConfig(double totalHeight, double gridSizeInMeters) {
        TreeParametersConfig config = new TreeParametersConfig("Lophostemon Confertus", "L.confertus",
                TUFBldVegHeights.BRUSHBOX_CONFIG_TYPE);

        config.setTreeDimensionsFromHeight(totalHeight, gridSizeInMeters);
        config.title = "single Lophostemon Confertuse in " + (int)gridSizeInMeters + "x" +
                       (int)gridSizeInMeters + " m plot. Scaled to cemetery tree";

        // Brushbox-specific parameters (slightly different from olive)
        config.leafAreaIndex = 2.0;  // Different LAI for brushbox
        config.calculateTotalLeafArea();

        return config;
    }

    /**
     * Load tree parameters from a configuration file
     * Format: key=value pairs
     */
    public static TreeParametersConfig loadFromFile(String filename) throws IOException {
        TreeParametersConfig config = new TreeParametersConfig();
        Map<String, String> params = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    params.put(parts[0].trim(), parts[1].trim());
                }
            }
        }

        // Apply parameters from file
        config.applyParametersFromMap(params);

        return config;
    }

    /**
     * Apply parameters from a map
     */
    private void applyParametersFromMap(Map<String, String> params) {
        if (params.containsKey("treeTypeName")) this.treeTypeName = params.get("treeTypeName");
        if (params.containsKey("speciesName")) this.speciesName = params.get("speciesName");
        if (params.containsKey("title")) this.title = params.get("title");

        if (params.containsKey("crownHeight")) this.crownHeight = Double.parseDouble(params.get("crownHeight"));
        if (params.containsKey("trunkHeight")) this.trunkHeight = Double.parseDouble(params.get("trunkHeight"));
        if (params.containsKey("crownRadiusX")) this.crownRadiusX = Double.parseDouble(params.get("crownRadiusX"));
        if (params.containsKey("crownRadiusY")) this.crownRadiusY = Double.parseDouble(params.get("crownRadiusY"));
        if (params.containsKey("stemDiameter")) this.stemDiameter = Double.parseDouble(params.get("stemDiameter"));

        if (params.containsKey("zht")) this.zht = Double.parseDouble(params.get("zht"));
        if (params.containsKey("zpd")) this.zpd = Double.parseDouble(params.get("zpd"));
        if (params.containsKey("z0ht")) this.z0ht = Double.parseDouble(params.get("z0ht"));

        if (params.containsKey("leafAreaIndex")) {
            this.leafAreaIndex = Double.parseDouble(params.get("leafAreaIndex"));
            calculateTotalLeafArea();
        }

        if (params.containsKey("g0")) this.g0 = Double.parseDouble(params.get("g0"));
        if (params.containsKey("g1")) this.g1 = Double.parseDouble(params.get("g1"));

        if (params.containsKey("configTypeId")) this.configTypeId = Integer.parseInt(params.get("configTypeId"));
    }

    // Getters and setters

    public String getTreeTypeName() { return treeTypeName; }
    public void setTreeTypeName(String treeTypeName) { this.treeTypeName = treeTypeName; }

    public String getSpeciesName() { return speciesName; }
    public void setSpeciesName(String speciesName) { this.speciesName = speciesName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getCrownHeight() { return crownHeight; }
    public void setCrownHeight(double crownHeight) {
        this.crownHeight = crownHeight;
        calculateAerodynamicParameters();
        calculateTotalLeafArea();
    }

    public double getTrunkHeight() { return trunkHeight; }
    public void setTrunkHeight(double trunkHeight) { this.trunkHeight = trunkHeight; }

    public double getCrownRadiusX() { return crownRadiusX; }
    public void setCrownRadiusX(double crownRadiusX) {
        this.crownRadiusX = crownRadiusX;
        calculateTotalLeafArea();
    }

    public double getCrownRadiusY() { return crownRadiusY; }
    public void setCrownRadiusY(double crownRadiusY) {
        this.crownRadiusY = crownRadiusY;
        calculateTotalLeafArea();
    }

    public double getStemDiameter() { return stemDiameter; }
    public void setStemDiameter(double stemDiameter) { this.stemDiameter = stemDiameter; }

    public double getPlotXMax() { return plotXMax; }
    public void setPlotXMax(double plotXMax) { this.plotXMax = plotXMax; }

    public double getPlotYMax() { return plotYMax; }
    public void setPlotYMax(double plotYMax) { this.plotYMax = plotYMax; }

    public double getTreeXPosition() { return treeXPosition; }
    public void setTreeXPosition(double treeXPosition) { this.treeXPosition = treeXPosition; }

    public double getTreeYPosition() { return treeYPosition; }
    public void setTreeYPosition(double treeYPosition) { this.treeYPosition = treeYPosition; }

    public double getZht() { return zht; }
    public void setZht(double zht) { this.zht = zht; }

    public double getZpd() { return zpd; }
    public void setZpd(double zpd) { this.zpd = zpd; }

    public double getZ0ht() { return z0ht; }
    public void setZ0ht(double z0ht) { this.z0ht = z0ht; }

    public double getLeafAreaIndex() { return leafAreaIndex; }
    public void setLeafAreaIndex(double leafAreaIndex) {
        this.leafAreaIndex = leafAreaIndex;
        calculateTotalLeafArea();
    }

    public double getTotalLeafArea() { return totalLeafArea; }

    public double getG0() { return g0; }
    public void setG0(double g0) { this.g0 = g0; }

    public double getG1() { return g1; }
    public void setG1(double g1) { this.g1 = g1; }

    public String getRhoSol() { return rhoSol; }
    public void setRhoSol(String rhoSol) { this.rhoSol = rhoSol; }

    public String getATau() { return aTau; }
    public void setATau(String aTau) { this.aTau = aTau; }

    public String getARho() { return aRho; }
    public void setARho(String aRho) { this.aRho = aRho; }

    public int getConfigTypeId() { return configTypeId; }
    public void setConfigTypeId(int configTypeId) { this.configTypeId = configTypeId; }

    public boolean isUseAutomaticCalculation() { return useAutomaticCalculation; }
    public void setUseAutomaticCalculation(boolean useAutomaticCalculation) {
        this.useAutomaticCalculation = useAutomaticCalculation;
    }

    public double getMinLeafWaterPotential() { return minLeafWaterPotential; }
    public void setMinLeafWaterPotential(double minLeafWaterPotential) {
        this.minLeafWaterPotential = minLeafWaterPotential;
    }

    public double getMinRootWaterPotential() { return minRootWaterPotential; }
    public void setMinRootWaterPotential(double minRootWaterPotential) {
        this.minRootWaterPotential = minRootWaterPotential;
    }

    /**
     * Get total tree height (trunk + crown)
     */
    public double getTotalHeight() {
        return trunkHeight + crownHeight;
    }

    /**
     * Get canopy area (crown radius X * crown radius Y * PI)
     */
    public double getCanopyArea() {
        return crownRadiusX * crownRadiusY * Math.PI;
    }

    /**
     * Generate a summary string of this configuration
     */
    @Override
    public String toString() {
        return String.format("TreeParametersConfig[%s (%s): height=%.2fm (trunk=%.2f, crown=%.2f), " +
                "radii=(%.2f,%.2f)m, LAI=%.2f, zpd=%.2f, z0ht=%.3f]",
                treeTypeName, speciesName, getTotalHeight(), trunkHeight, crownHeight,
                crownRadiusX, crownRadiusY, leafAreaIndex, zpd, z0ht);
    }
}

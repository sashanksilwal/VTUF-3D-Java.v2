# Tree Configuration Files

This directory contains example tree configuration files for the VTUF-3D-Java parametrization system.

## Overview

The tree parametrization system allows you to define tree characteristics in configuration files, making it easy to create and manage multiple tree types without modifying Java code. This matches the structure seen in the example directories (1, 2, 3, etc.).

## Configuration Structure

The example directory structure typically follows this pattern:
```
example/
  PrestonBase8/
    1/              # Configuration 1: Grass layer
      1/            # Shading type 1: Diffuse only
        trees.dat
        str1.dat
        phy1.dat
        ...
      2/            # Shading type 2: 100% direct
        trees.dat
        ...
    2/              # Configuration 2: Small olive tree
      1/
      2/
    3/              # Configuration 3: Small brushbox tree
      1/
      2/
    4/              # Configuration 4: Large olive tree
      1/
      2/
    5/              # Configuration 5: Large brushbox tree
      1/
      2/
```

## Configuration Files

This directory includes the following example configurations:

1. **grass.properties** - Grass layer configuration (0.2m height)
   - Models grass as a box tree on the ground
   - Low crown height (blade length)
   - Appropriate aerodynamic parameters for grass

2. **olive_small.properties** - Small olive tree (5m total height)
   - Mediterranean olive tree (Olea europaea)
   - Crown height: 3.75m, trunk: 1.25m
   - LAI: 2.48

3. **brushbox_large.properties** - Large brushbox tree (10m total height)
   - Lophostemon confertus (Brisbane box)
   - Crown height: 7.5m, trunk: 2.5m
   - LAI: 2.0

## Configuration Parameters

Each configuration file supports the following parameters:

### Tree Identification
- `treeTypeName` - Descriptive name for the tree type
- `speciesName` - Scientific or common species name
- `title` - Title for the trees.dat file
- `configTypeId` - Configuration type ID (1=Olive, 2=Grass, 3=Brushbox, etc.)

### Physical Dimensions (meters)
- `crownHeight` - Height of the tree crown
- `trunkHeight` - Height of the trunk
- `crownRadiusX` - Crown radius in X direction
- `crownRadiusY` - Crown radius in Y direction
- `stemDiameter` - Stem/trunk diameter

### Plot Dimensions (meters)
- `plotXMax` - Maximum X dimension of the plot
- `plotYMax` - Maximum Y dimension of the plot
- `treeXPosition` - X coordinate of tree in plot
- `treeYPosition` - Y coordinate of tree in plot

### Aerodynamic Parameters (meters)
- `zht` - Height of wind speed measurements
- `zpd` - Zero plane displacement (typically 2/3 of crown height)
- `z0ht` - Roughness length (typically 1/10 of crown height)

### Physiological Parameters
- `leafAreaIndex` - Leaf Area Index (LAI)
- `g0` - Minimum stomatal conductance
- `g1` - Stomatal slope parameter

## Usage

### Using TreeConfigurationFactory (Recommended)

```java
import VTUF3D.ConfigMaker.TreeConfigurationFactory;
import VTUF3D.ConfigMaker.TreeParametersConfig;
import java.util.List;

// Create factory with grid size
TreeConfigurationFactory factory = new TreeConfigurationFactory(5.0);

// Option 1: Create standard configurations
List<TreeParametersConfig> configs = factory.createStandardConfigurations();

// Option 2: Create PrestonBase8-style configurations
Map<Integer, TreeParametersConfig> prestonConfigs =
    factory.createPrestonBase8Configurations();

// Option 3: Create height variations
double[] heights = {3.0, 5.0, 7.0, 10.0, 15.0};
List<TreeParametersConfig> olives =
    factory.createHeightVariations("olive", heights);

// Print summary
factory.printConfigurationSummary();

// Apply configuration to MAESPA config
MaespaConfigConfileDat maespaConfig = new MaespaConfigConfileDat(...);
TreeParametersConfig treeConfig = configs.get(0);
factory.applyConfigurationToMaespa(maespaConfig, treeConfig, 1);
```

### Loading from Configuration File

```java
import VTUF3D.ConfigMaker.TreeParametersConfig;

try {
    // Load configuration from file
    TreeParametersConfig config =
        TreeParametersConfig.loadFromFile("tree_configs/olive_small.properties");

    System.out.println(config);

    // Use the configuration...

} catch (IOException e) {
    e.printStackTrace();
}
```

### Creating Configurations Programmatically

```java
import VTUF3D.ConfigMaker.TreeParametersConfig;

// Option 1: Using factory methods
TreeParametersConfig olive =
    TreeParametersConfig.createOliveConfig(5.0, 5.0);

TreeParametersConfig brushbox =
    TreeParametersConfig.createBrushboxConfig(10.0, 5.0);

TreeParametersConfig grass =
    TreeParametersConfig.createGrassConfig(5.0);

// Option 2: Custom configuration
TreeParametersConfig custom = new TreeParametersConfig("My Tree", "Species X", 10);
custom.setCrownHeight(6.0);
custom.setTrunkHeight(2.0);
custom.setCrownRadiusX(3.0);
custom.setCrownRadiusY(3.0);
custom.setLeafAreaIndex(3.5);
// ... set other parameters
```

## Creating New Tree Types

To create a new tree type:

1. **Create a properties file** in this directory:
   ```properties
   # tree_configs/my_custom_tree.properties
   treeTypeName=My Custom Tree
   speciesName=Customus treeus
   configTypeId=6
   crownHeight=8.0
   trunkHeight=2.0
   # ... other parameters
   ```

2. **Load and use it**:
   ```java
   TreeParametersConfig config =
       TreeParametersConfig.loadFromFile("tree_configs/my_custom_tree.properties");
   ```

## Parameter Calculation

The system automatically calculates some derived parameters:

1. **Aerodynamic parameters** (if not explicitly set):
   - `zpd = crownHeight * 2/3` (zero plane displacement)
   - `z0ht = crownHeight / 10` (roughness length)

2. **Stem diameter** (from allometric relationships):
   - If `totalHeight > 7m`: `stemDiameter = (totalHeight - 6.74) / 14.4`
   - Otherwise: `stemDiameter = 0.05`

3. **Total leaf area**:
   - `totalLeafArea = LAI * (crownRadiusX * crownRadiusY * π)`

## References

- **Allometric relationships**:
  - http://www.academicjournals.org/journal/AJB/article-abstract/96974C726212
  - DOI: 10.1093/treephys/tps127

- **Aerodynamic parameters**:
  - zpd (zero plane displacement): Typically 2/3 of canopy height
  - z0ht (roughness length): Typically 1/10 of canopy height

- **Grass parameters**: Blade length from Simmonds et al 2011

## Integration with ConfigurationMaker

The new parametrization system integrates with the existing `ConfigurationMaker` class:

```java
// In ConfigurationMaker
TreeConfigurationFactory factory = new TreeConfigurationFactory(gridSizeInMeters);
List<TreeParametersConfig> treeConfigs = factory.createStandardConfigurations();

// For each configuration
for (int i = 0; i < treeConfigs.size(); i++) {
    TreeParametersConfig treeConfig = treeConfigs.get(i);
    String subdirectory = (i + 1) + "";

    // For each shading type (1 = diffuse, 2 = direct)
    for (int shadingType = 1; shadingType < 3; shadingType++) {
        String configDir = runDirectory + "/" + subdirectory + "/" + shadingType;

        // Apply tree configuration to MAESPA config
        factory.applyConfigurationToMaespa(maespaConfig, treeConfig, 1);

        // Write configuration files
        maespaConfig.writeTreeConfigFile(configDir);
        // ... write other config files
    }
}
```

## Directory Correspondence

The numbered directories in the example structure correspond to:

| Directory | Tree Type | Height | LAI | Species |
|-----------|-----------|--------|-----|---------|
| 1 | Grass | 0.2m | 2.0 | Turf grass |
| 2 | Olive (small) | 5.0m | 2.48 | O.europaea |
| 3 | Brushbox (small) | 5.0m | 2.0 | L.confertus |
| 4 | Olive (large) | 10.0m | 2.48 | O.europaea |
| 5 | Brushbox (large) | 10.0m | 2.0 | L.confertus |

Each directory contains subdirectories 1 and 2 for different shading configurations:
- Subdirectory 1: Diffuse shading only
- Subdirectory 2: 100% direct shading

## Notes

- All measurements are in meters unless otherwise specified
- The `configTypeId` should match constants in `TUFBldVegHeights`:
  - 1 = OLIVE_CONFIG_TYPE
  - 2 = GRASS_CONFIG_TYPE
  - 3 = BRUSHBOX_CONFIG_TYPE
  - 4 = OLD_GRASS_CONFIG_TYPE
  - 5 = IRRIGATED_GRASS_CONFIG_TYPE

- When `useAutomaticCalculation=true` (default), aerodynamic parameters are automatically calculated from crown height
- Set individual parameters to override automatic calculations

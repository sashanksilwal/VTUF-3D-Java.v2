# Tree Parametrization System Guide

## Overview

This guide describes the new tree parametrization system for VTUF-3D-Java v2. The system provides a flexible way to define and manage tree configurations, similar to the structure seen in the example directories (1, 2, 3, etc.).

## Table of Contents

1. [Introduction](#introduction)
2. [System Architecture](#system-architecture)
3. [Quick Start](#quick-start)
4. [Configuration Structure](#configuration-structure)
5. [Usage Examples](#usage-examples)
6. [API Reference](#api-reference)
7. [Integration Guide](#integration-guide)

## Introduction

The tree parametrization system allows you to:
- Define tree characteristics in configuration files or programmatically
- Create multiple tree configurations easily (grass, olive, brushbox, custom)
- Automatically calculate derived parameters (aerodynamic parameters, stem diameter, leaf area)
- Match the example directory structure (dirs 1, 2, 3, 4, 5, etc.)

### Example Directory Structure

```
example/PrestonBase8/
  1/              # Grass layer (0.2m)
    1/            # Diffuse shading
    2/            # Direct shading
  2/              # Small olive (5m)
    1/
    2/
  3/              # Small brushbox (5m)
    1/
    2/
  4/              # Large olive (10m)
    1/
    2/
  5/              # Large brushbox (10m)
    1/
    2/
```

## System Architecture

### Core Classes

1. **TreeParametersConfig** - Stores all tree parameters
   - Physical dimensions (crown height, trunk height, radii)
   - Aerodynamic parameters (zpd, z0ht, zht)
   - Physiological parameters (LAI, stomatal conductance)
   - Automatic calculation of derived parameters

2. **TreeConfigurationFactory** - Creates and manages tree configurations
   - Factory methods for standard tree types
   - Batch creation of configurations
   - Integration with MAESPA configuration

3. **TUFBldVegHeights** - Existing class with tree type constants
   - OLIVE_CONFIG_TYPE = 1
   - GRASS_CONFIG_TYPE = 2
   - BRUSHBOX_CONFIG_TYPE = 3
   - OLD_GRASS_CONFIG_TYPE = 4
   - IRRIGATED_GRASS_CONFIG_TYPE = 5

### Key Features

- **Automatic Parameter Calculation**: Aerodynamic parameters and stem diameter calculated from tree height
- **Factory Methods**: Quick creation of standard tree types (olive, brushbox, grass)
- **Configuration Files**: Load tree parameters from external files
- **Height Variations**: Easily create multiple configurations with different heights
- **MAESPA Integration**: Direct integration with existing MAESPA configuration system

## Quick Start

### 1. Run the Demo

```bash
cd src
javac -cp ".:../opencsv-2.3.jar" VTUF3D/ConfigMaker/Common.java \
     VTUF3D/ConfigMaker/TUFBldVegHeights.java \
     VTUF3D/ConfigMaker/TreeParametersConfig.java \
     VTUF3D/ConfigMaker/TreeConfigurationFactory.java

java -cp ".:../opencsv-2.3.jar" VTUF3D.ConfigMaker.TreeConfigurationFactory
```

### 2. Create Standard Configurations

```java
import VTUF3D.ConfigMaker.TreeConfigurationFactory;
import VTUF3D.ConfigMaker.TreeParametersConfig;

// Create factory with 5-meter grid
TreeConfigurationFactory factory = new TreeConfigurationFactory(5.0);

// Create standard configurations (matching PrestonBase8)
List<TreeParametersConfig> configs = factory.createStandardConfigurations();

// Print summary
factory.printConfigurationSummary();
```

Output:
```
Configuration 1 [Grass]: height=0.20m, LAI=2.00
Configuration 2 [SmallOlive]: height=5.00m, LAI=2.48
Configuration 3 [SmallBrushbox]: height=5.00m, LAI=2.00
Configuration 4 [LargeOlive]: height=10.00m, LAI=2.48
Configuration 5 [LargeBrushbox]: height=10.00m, LAI=2.00
```

### 3. Create Custom Configuration

```java
// Option 1: Use factory method
TreeParametersConfig olive =
    TreeParametersConfig.createOliveConfig(8.0, 5.0);

// Option 2: Load from file
TreeParametersConfig custom =
    TreeParametersConfig.loadFromFile("tree_configs/olive_small.properties");

// Option 3: Create manually
TreeParametersConfig manual =
    new TreeParametersConfig("My Tree", "M.treeus", 10);
manual.setCrownHeight(6.0);
manual.setTrunkHeight(2.0);
manual.setLeafAreaIndex(3.0);
```

## Configuration Structure

### Tree Parameters

| Parameter | Description | Units | Example |
|-----------|-------------|-------|---------|
| **Physical Dimensions** ||||
| crownHeight | Height of tree crown | meters | 3.75 |
| trunkHeight | Height of trunk | meters | 1.25 |
| crownRadiusX | Crown radius (X direction) | meters | 2.5 |
| crownRadiusY | Crown radius (Y direction) | meters | 2.5 |
| stemDiameter | Stem/trunk diameter | meters | 0.05 |
| **Aerodynamic** ||||
| zht | Wind measurement height | meters | 40.0 |
| zpd | Zero plane displacement | meters | 2.5 |
| z0ht | Roughness length | meters | 0.375 |
| **Physiological** ||||
| leafAreaIndex | Leaf Area Index (LAI) | - | 2.48 |
| g0 | Min stomatal conductance | mol/m²/s | 0.03 |
| g1 | Stomatal slope parameter | - | 2.615 |

### Automatic Calculations

The system automatically calculates:

1. **Zero Plane Displacement (zpd)**
   ```
   zpd = crownHeight × 2/3
   ```

2. **Roughness Length (z0ht)**
   ```
   z0ht = crownHeight / 10
   ```

3. **Stem Diameter** (allometric relationship)
   ```java
   if (totalHeight > 7m) {
       stemDiameter = (totalHeight - 6.74) / 14.4
   } else {
       stemDiameter = 0.05
   }
   ```

4. **Total Leaf Area**
   ```
   totalLeafArea = LAI × (crownRadiusX × crownRadiusY × π)
   ```

## Usage Examples

### Example 1: Create PrestonBase8 Configurations

```java
TreeConfigurationFactory factory = new TreeConfigurationFactory(5.0);
Map<Integer, TreeParametersConfig> configs =
    factory.createPrestonBase8Configurations();

// Directory 1: Grass
TreeParametersConfig grass = configs.get(1);
System.out.println("Config 1: " + grass.getTreeTypeName() +
                   ", height: " + grass.getTotalHeight() + "m");

// Directory 2: Small olive
TreeParametersConfig smallOlive = configs.get(2);
System.out.println("Config 2: " + smallOlive.getTreeTypeName() +
                   ", height: " + smallOlive.getTotalHeight() + "m");

// ... etc for configs 3, 4, 5
```

### Example 2: Create Height Variations

```java
TreeConfigurationFactory factory = new TreeConfigurationFactory(5.0);

// Create olive trees at different heights
double[] heights = {3.0, 5.0, 7.0, 10.0, 15.0};
List<TreeParametersConfig> olives =
    factory.createHeightVariations("olive", heights);

// Result: 5 olive configurations at different heights
for (TreeParametersConfig config : olives) {
    System.out.println(config);
}
```

Output:
```
olive_3.0m: height=3.00m (trunk=0.75, crown=2.25), zpd=1.50, z0ht=0.225
olive_5.0m: height=5.00m (trunk=1.25, crown=3.75), zpd=2.50, z0ht=0.375
olive_7.0m: height=7.00m (trunk=1.75, crown=5.25), zpd=3.50, z0ht=0.525
olive_10.0m: height=10.00m (trunk=2.50, crown=7.50), zpd=5.00, z0ht=0.750
olive_15.0m: height=15.00m (trunk=3.75, crown=11.25), zpd=7.50, z0ht=1.125
```

### Example 3: Load from Configuration File

```java
// Load pre-defined tree configuration
TreeParametersConfig config =
    TreeParametersConfig.loadFromFile("tree_configs/olive_small.properties");

System.out.println("Loaded: " + config.getTreeTypeName());
System.out.println("Species: " + config.getSpeciesName());
System.out.println("Height: " + config.getTotalHeight() + "m");
System.out.println("LAI: " + config.getLeafAreaIndex());
```

### Example 4: Apply to MAESPA Configuration

```java
// Create tree configuration
TreeParametersConfig treeConfig =
    TreeParametersConfig.createOliveConfig(5.0, 5.0);

// Create MAESPA configuration
MaespaConfigConfileDat maespaConfig =
    new MaespaConfigConfileDat(runDirectory, year, configNumber, day, numDays);

// Apply tree parameters to MAESPA config
TreeConfigurationFactory factory = new TreeConfigurationFactory(5.0);
factory.applyConfigurationToMaespa(maespaConfig, treeConfig, 1);

// Write configuration files
maespaConfig.writeTreeConfigFile(configDirectory);
```

### Example 5: Custom Tree Type

```java
// Create custom tree type
TreeParametersConfig custom =
    new TreeParametersConfig("Urban Pine", "P.urbanensis", 6);

// Set physical dimensions
custom.setTreeDimensionsFromHeight(12.0, 5.0);

// Override automatic LAI
custom.setLeafAreaIndex(3.5);

// Override aerodynamic parameters if needed
custom.setZpd(8.0);
custom.setZ0ht(1.2);

// Disable automatic calculation for manual control
custom.setUseAutomaticCalculation(false);
```

## API Reference

### TreeParametersConfig

#### Factory Methods

```java
// Create grass configuration
static TreeParametersConfig createGrassConfig(double gridSizeInMeters)

// Create olive configuration
static TreeParametersConfig createOliveConfig(double totalHeight,
                                               double gridSizeInMeters)

// Create brushbox configuration
static TreeParametersConfig createBrushboxConfig(double totalHeight,
                                                  double gridSizeInMeters)

// Load from file
static TreeParametersConfig loadFromFile(String filename)
```

#### Key Methods

```java
// Set tree dimensions from total height
void setTreeDimensionsFromHeight(double totalHeight, double gridSizeInMeters)

// Get calculated values
double getTotalHeight()           // trunk + crown height
double getCanopyArea()            // crown area (πr²)
double getTotalLeafArea()         // LAI × canopy area

// Getters and setters for all parameters
double getCrownHeight()
void setCrownHeight(double height)
// ... (see class for full list)
```

### TreeConfigurationFactory

#### Factory Methods

```java
// Create standard 5 configurations (grass + 2 tree types × 2 heights)
List<TreeParametersConfig> createStandardConfigurations()

// Create PrestonBase8-style configurations (returns map)
Map<Integer, TreeParametersConfig> createPrestonBase8Configurations()

// Create PrestonBaseSmall-style configurations
List<TreeParametersConfig> createPrestonBaseSmallConfigurations()

// Create height variations for a tree type
List<TreeParametersConfig> createHeightVariations(String baseType,
                                                   double[] heights)
```

#### Configuration Management

```java
// Add custom configuration
void addConfiguration(String name, TreeParametersConfig config)

// Get configuration by name
TreeParametersConfig getConfiguration(String name)

// Get all configurations
List<TreeParametersConfig> getAllConfigurations()

// Print summary
void printConfigurationSummary()
```

#### MAESPA Integration

```java
// Apply tree configuration to MAESPA config object
void applyConfigurationToMaespa(MaespaConfigConfileDat maespaConfig,
                                TreeParametersConfig treeConfig,
                                int numberOfTrees)
```

## Integration Guide

### Integrating with ConfigurationMaker

To integrate the new parametrization system with the existing `ConfigurationMaker` class:

```java
public void processWithTreeConfigs(/* ... existing parameters ... */) {

    // Create tree configuration factory
    TreeConfigurationFactory treeFactory =
        new TreeConfigurationFactory(gridSizeInMeters);

    // Create standard configurations (or custom ones)
    List<TreeParametersConfig> treeConfigs =
        treeFactory.createStandardConfigurations();

    // Iterate through tree configurations
    for (int configIndex = 0; configIndex < treeConfigs.size(); configIndex++) {
        TreeParametersConfig treeConfig = treeConfigs.get(configIndex);

        String newSubdirectory = (configIndex + 1) + "";  // Directory 1, 2, 3, etc.

        // For each shading type (1 = diffuse, 2 = direct)
        for (int diffShadingValue = 1; diffShadingValue < 3; diffShadingValue++) {

            String configDirectory =
                runDirectory + "/" + newSubdirectory + "/" + diffShadingValue;

            // Create MAESPA configuration
            MaespaConfigConfileDat maespaConfig =
                new MaespaConfigConfileDat(runDirectory, year, configNumber,
                                          day, numDays);

            // Apply tree parameters to MAESPA config
            treeFactory.applyConfigurationToMaespa(maespaConfig, treeConfig, 1);

            // Generate and write configuration files
            maespaConfig.generateFile();
            maespaConfig.writeConfigFile(configDirectory);
            maespaConfig.writeTreeConfigFile(configDirectory);

            // Write other config files (str, phy, watpars, etc.)
            // ... existing code ...
        }
    }
}
```

### Using with Existing ConfigFileVariations

The new system can work alongside the existing `configFileVariations` approach:

```java
// Option 1: Replace configFileVariations with TreeConfigurationFactory
TreeConfigurationFactory factory = new TreeConfigurationFactory(gridSizeInMeters);
List<TreeParametersConfig> configs = factory.createStandardConfigurations();

// Option 2: Convert existing configFileVariations to TreeParametersConfig
TreeMap<String, Integer> configFileVariations = vegHeights.getConfigFileVariations();

for (String keyValue : configFileVariations.keySet()) {
    String[] splitKeyValues = keyValue.split("_");
    int configType = Integer.parseInt(splitKeyValues[1]);
    int heightInGrids = Integer.parseInt(splitKeyValues[0]);
    double heightInMeters = heightInGrids * gridSizeInMeters;

    // Create tree config based on type
    TreeParametersConfig treeConfig;
    switch (configType) {
        case TUFBldVegHeights.OLIVE_CONFIG_TYPE:
            treeConfig = TreeParametersConfig.createOliveConfig(
                heightInMeters, gridSizeInMeters);
            break;
        case TUFBldVegHeights.GRASS_CONFIG_TYPE:
            treeConfig = TreeParametersConfig.createGrassConfig(gridSizeInMeters);
            break;
        case TUFBldVegHeights.BRUSHBOX_CONFIG_TYPE:
            treeConfig = TreeParametersConfig.createBrushboxConfig(
                heightInMeters, gridSizeInMeters);
            break;
        default:
            // Handle other types
            treeConfig = new TreeParametersConfig();
    }

    // Use treeConfig...
}
```

## Configuration File Format

### Properties File Format

```properties
# Tree identification
treeTypeName=Olive Tree
speciesName=O.europaea
title=Single olive tree in 5x5 metre plot.
configTypeId=1

# Physical dimensions (meters)
crownHeight=3.75
trunkHeight=1.25
crownRadiusX=2.5
crownRadiusY=2.5
stemDiameter=0.05

# Plot dimensions (meters)
plotXMax=5.0
plotYMax=5.0
treeXPosition=2.5
treeYPosition=2.5

# Aerodynamic parameters (meters)
zht=40.0
zpd=2.5
z0ht=0.375

# Physiological parameters
leafAreaIndex=2.48
g0=0.03
g1=2.615
```

See `tree_configs/` directory for complete examples.

## Best Practices

1. **Use Factory Methods**: Prefer factory methods for standard tree types
   ```java
   TreeParametersConfig.createOliveConfig(5.0, 5.0)  // Good
   ```

2. **Automatic Calculation**: Let the system calculate derived parameters unless you have specific values
   ```java
   config.setTreeDimensionsFromHeight(10.0, 5.0);  // Auto-calculates zpd, z0ht, etc.
   ```

3. **Configuration Files**: Use configuration files for custom tree types
   ```java
   TreeParametersConfig.loadFromFile("tree_configs/my_custom_tree.properties")
   ```

4. **Height Variations**: Use `createHeightVariations()` for sensitivity analysis
   ```java
   double[] heights = {5.0, 10.0, 15.0, 20.0};
   factory.createHeightVariations("olive", heights);
   ```

5. **Validation**: Always check configuration validity before use
   ```java
   if (config.getCrownHeight() <= 0) {
       // Handle invalid configuration
   }
   ```

## References

### Allometric Relationships
- http://www.academicjournals.org/journal/AJB/article-abstract/96974C726212
- DOI: 10.1093/treephys/tps127

### Aerodynamic Parameters
- Monin-Obukhov similarity theory
- zpd (zero plane displacement): Typically 2/3 of canopy height
- z0ht (roughness length): Typically 1/10 of canopy height

### Tree-Specific Parameters
- **Olive (O.europaea)**: Baldini et al 1997 (reflectance/transmittance)
- **Brushbox (L.confertus)**: Cemetery tree scaling
- **Grass**: Blade length from Simmonds et al 2011

## Troubleshooting

### Common Issues

1. **Compilation errors**: Make sure to compile dependencies first
   ```bash
   javac Common.java TUFBldVegHeights.java TreeParametersConfig.java TreeConfigurationFactory.java
   ```

2. **Zero aerodynamic parameters**: Crown height cannot be zero
   ```java
   // System automatically sets minimum values
   if (crownHeight == 0) {
       zpd = 0.066;
       z0ht = 0.02;
   }
   ```

3. **File not found**: Check file path for configuration files
   ```java
   String configPath = "tree_configs/olive_small.properties";
   File f = new File(configPath);
   if (!f.exists()) {
       System.err.println("Config file not found: " + configPath);
   }
   ```

## Summary

The tree parametrization system provides:
- ✅ Flexible tree configuration (programmatic or file-based)
- ✅ Automatic parameter calculation
- ✅ Standard tree types (olive, brushbox, grass)
- ✅ Height variation generation
- ✅ MAESPA integration
- ✅ Matches example directory structure

For more information, see:
- `tree_configs/README.md` - Configuration file documentation
- `TreeParametersConfig.java` - API documentation
- `TreeConfigurationFactory.java` - Factory methods and examples

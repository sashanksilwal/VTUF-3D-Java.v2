package VTUF3D;

public class MaespaConfigTreeMapState
{
        public int numberTreePlots;
        public String rootDirectory;
        public int[] xLocation;
        public int[] yLocation;
        public int[] phyfileNumber;
        public int[] strfileNumber;
        public int[] treesfileNumber;
        public int[] treesHeight;
        public int[] trees;
        public int numberBuildingPlots;
        public int[] xBuildingLocation;
        public int[] yBuildingLocation;
        public int[] buildingsHeight;
        public int configPartitioningMethod;
        public int usingDiffShading;
        public int width;
        public int length;
        public int configTreeMapCentralArrayLength;
        public int configTreeMapCentralWidth;
        public int configTreeMapCentralLength;
        public int configTreeMapX;
        public int configTreeMapY;
        public int configTreeMapX1;
        public int configTreeMapX2;
        public int configTreeMapY1;
        public int configTreeMapY2;
        public int configTreeMapNumsfcab;
        public double configTreeMapGridSize;
        public int configTreeMapHighestBuildingHeight;

        // Per-cell surface properties for spatial variation
        // These are 2D arrays stored as 1D: index = x * length + y
        public boolean hasCellProperties = false;
        public double[] cellAlbedo;      // Per-cell ground surface albedo
        public double[] cellEmissivity;  // Per-cell ground surface emissivity

}

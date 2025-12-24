package com.example.resourcegenerator.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Configuration class for generator types.
 * Defines the recipe, output, generation rate, and appearance of a generator type.
 */
public class GeneratorConfig {
    
    private final String name;
    private final Material blockType;
    private final ItemStack[] recipe; // 3x3 array (9 elements)
    private final ItemStack output;
    private final int generationTimeSeconds;
    private final int storageCapacity;

    @JsonCreator
    public GeneratorConfig(
            @JsonProperty("name") String name,
            @JsonProperty("blockType") Material blockType,
            @JsonProperty("recipe") ItemStack[] recipe,
            @JsonProperty("output") ItemStack output,
            @JsonProperty("generationTimeSeconds") int generationTimeSeconds,
            @JsonProperty("storageCapacity") int storageCapacity) {
        
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Generator name cannot be null or empty");
        }
        if (blockType == null) {
            throw new IllegalArgumentException("Block type cannot be null");
        }
        if (recipe == null || recipe.length != 9) {
            throw new IllegalArgumentException("Recipe must be a 3x3 grid (9 elements)");
        }
        if (output == null) {
            throw new IllegalArgumentException("Output cannot be null");
        }
        if (generationTimeSeconds <= 0) {
            throw new IllegalArgumentException("Generation time must be positive");
        }
        if (storageCapacity <= 0) {
            throw new IllegalArgumentException("Storage capacity must be positive");
        }

        this.name = name.trim();
        this.blockType = blockType;
        this.recipe = recipe.clone(); // Defensive copy
        this.output = output.clone();
        this.generationTimeSeconds = generationTimeSeconds;
        this.storageCapacity = storageCapacity;
    }

    /**
     * Gets the name of this generator type.
     * @return The generator name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the block type that represents this generator in the world.
     * @return The block material
     */
    public Material getBlockType() {
        return blockType;
    }

    /**
     * Gets the crafting recipe for this generator.
     * The array represents a 3x3 crafting grid in row-major order.
     * @return A copy of the recipe array
     */
    public ItemStack[] getRecipe() {
        return recipe.clone(); // Defensive copy
    }

    /**
     * Gets the item at a specific position in the recipe grid.
     * @param row The row (0-2)
     * @param col The column (0-2)
     * @return The item at that position, or null if empty
     */
    public ItemStack getRecipeItem(int row, int col) {
        if (row < 0 || row > 2 || col < 0 || col > 2) {
            throw new IllegalArgumentException("Recipe coordinates must be 0-2");
        }
        return recipe[row * 3 + col];
    }

    /**
     * Gets the output item produced by this generator.
     * @return A copy of the output item
     */
    public ItemStack getOutput() {
        return output.clone();
    }

    /**
     * Gets the time in seconds required to generate one output item.
     * @return The generation time in seconds
     */
    public int getGenerationTimeSeconds() {
        return generationTimeSeconds;
    }

    /**
     * Gets the maximum storage capacity for this generator type.
     * @return The storage capacity in item stacks
     */
    public int getStorageCapacity() {
        return storageCapacity;
    }

    /**
     * Checks if the given 3x3 crafting grid matches this generator's recipe.
     * @param craftingGrid The crafting grid to check (9 elements)
     * @return True if the recipe matches
     */
    public boolean matchesRecipe(ItemStack[] craftingGrid) {
        if (craftingGrid == null || craftingGrid.length != 9) {
            return false;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack recipeItem = recipe[i];
            ItemStack craftingItem = craftingGrid[i];

            // Both null - matches
            if (recipeItem == null && craftingItem == null) {
                continue;
            }

            // One null, one not - doesn't match
            if (recipeItem == null || craftingItem == null) {
                return false;
            }

            // Special handling for beds - accept any bed type
            if (isBedType(recipeItem.getType()) && isBedType(craftingItem.getType())) {
                if (recipeItem.getAmount() > craftingItem.getAmount()) {
                    return false;
                }
                continue;
            }

            // Check material and amount for non-bed items
            if (recipeItem.getType() != craftingItem.getType() ||
                recipeItem.getAmount() > craftingItem.getAmount()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a material is a bed type.
     * @param material The material to check
     * @return True if the material is any type of bed
     */
    private boolean isBedType(Material material) {
        return material.name().endsWith("_BED");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeneratorConfig that = (GeneratorConfig) o;
        return generationTimeSeconds == that.generationTimeSeconds &&
               storageCapacity == that.storageCapacity &&
               Objects.equals(name, that.name) &&
               blockType == that.blockType &&
               Objects.deepEquals(recipe, that.recipe) &&
               Objects.equals(output, that.output);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, blockType, Objects.hashCode(recipe), output, 
                           generationTimeSeconds, storageCapacity);
    }

    @Override
    public String toString() {
        return "GeneratorConfig{" +
               "name='" + name + '\'' +
               ", blockType=" + blockType +
               ", output=" + output +
               ", generationTimeSeconds=" + generationTimeSeconds +
               ", storageCapacity=" + storageCapacity +
               '}';
    }
}
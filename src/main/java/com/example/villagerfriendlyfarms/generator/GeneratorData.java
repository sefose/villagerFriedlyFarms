package com.example.villagerfriendlyfarms.generator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Represents an active generator instance in the world.
 * Contains all runtime data for a specific generator including location,
 * ownership, timing, and stored items.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratorData {
    
    private final UUID id;
    private final Location location;
    private final String generatorType;
    private final UUID owner;
    private long lastAccessedTime;
    private long lastGenerationTime; // Track when items were last generated
    private final List<ItemStack> storedItems;

    @JsonCreator
    public GeneratorData(
            @JsonProperty("id") UUID id,
            @JsonProperty("location") Location location,
            @JsonProperty("generatorType") String generatorType,
            @JsonProperty("owner") UUID owner,
            @JsonProperty("lastAccessedTime") long lastAccessedTime,
            @JsonProperty("lastGenerationTime") Long lastGenerationTime,
            @JsonProperty("storedItems") List<ItemStack> storedItems) {
        
        if (id == null) {
            throw new IllegalArgumentException("Generator ID cannot be null");
        }
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        if (generatorType == null || generatorType.trim().isEmpty()) {
            throw new IllegalArgumentException("Generator type cannot be null or empty");
        }
        if (owner == null) {
            throw new IllegalArgumentException("Owner cannot be null");
        }

        this.id = id;
        this.location = location.clone();
        this.generatorType = generatorType.trim();
        this.owner = owner;
        this.lastAccessedTime = lastAccessedTime;
        this.lastGenerationTime = lastGenerationTime != null ? lastGenerationTime : System.currentTimeMillis();
        this.storedItems = storedItems != null ? new ArrayList<>(storedItems) : new ArrayList<>();
    }

    /**
     * Creates a new generator instance with current timestamp.
     * @param location The location of the generator
     * @param generatorType The type of generator
     * @param owner The owner's UUID
     * @return A new GeneratorData instance
     */
    public static GeneratorData create(Location location, String generatorType, UUID owner) {
        long currentTime = System.currentTimeMillis();
        return new GeneratorData(
            UUID.randomUUID(),
            location,
            generatorType,
            owner,
            currentTime,
            currentTime, // Start generation timer from creation
            new ArrayList<>()
        );
    }

    /**
     * Gets the unique identifier for this generator.
     * @return The generator UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the world location of this generator.
     * @return A copy of the location
     */
    public Location getLocation() {
        return location.clone();
    }

    /**
     * Gets the type name of this generator.
     * @return The generator type
     */
    public String getGeneratorType() {
        return generatorType;
    }

    /**
     * Gets the UUID of the player who owns this generator.
     * @return The owner's UUID
     */
    public UUID getOwner() {
        return owner;
    }

    /**
     * Gets the timestamp when this generator was last accessed.
     * @return The last accessed time in milliseconds since epoch
     */
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    /**
     * Gets the timestamp when items were last generated.
     * @return The last generation time in milliseconds since epoch
     */
    public long getLastGenerationTime() {
        return lastGenerationTime;
    }

    /**
     * Updates the last accessed time to the current time.
     */
    public void updateLastAccessedTime() {
        this.lastAccessedTime = System.currentTimeMillis();
    }

    /**
     * Updates the last generation time to the current time.
     */
    public void updateLastGenerationTime() {
        this.lastGenerationTime = System.currentTimeMillis();
    }

    /**
     * Sets the last generation time to a specific timestamp.
     * @param timestamp The timestamp in milliseconds since epoch
     */
    public void setLastGenerationTime(long timestamp) {
        this.lastGenerationTime = timestamp;
    }

    /**
     * Sets the last accessed time to a specific timestamp.
     * @param timestamp The timestamp in milliseconds since epoch
     */
    public void setLastAccessedTime(long timestamp) {
        this.lastAccessedTime = timestamp;
    }

    /**
     * Gets the list of items currently stored in this generator.
     * @return A copy of the stored items list
     */
    public List<ItemStack> getStoredItems() {
        return new ArrayList<>(storedItems);
    }

    /**
     * Adds an item to the generator's storage with proper stacking.
     * @param item The item to add
     * @return True if the item was added successfully
     */
    public boolean addStoredItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        
        // Try to stack with existing items first
        for (ItemStack existingItem : storedItems) {
            if (existingItem.isSimilar(item)) {
                int maxStackSize = existingItem.getType().getMaxStackSize();
                int currentAmount = existingItem.getAmount();
                int itemAmount = item.getAmount();
                
                if (currentAmount < maxStackSize) {
                    int canAdd = Math.min(itemAmount, maxStackSize - currentAmount);
                    existingItem.setAmount(currentAmount + canAdd);
                    
                    // If we added all the items, we're done
                    if (canAdd >= itemAmount) {
                        return true;
                    }
                    
                    // Otherwise, reduce the amount we still need to add
                    item = item.clone();
                    item.setAmount(itemAmount - canAdd);
                }
            }
        }
        
        // If we couldn't stack everything, add new stacks
        while (item.getAmount() > 0) {
            int maxStackSize = item.getType().getMaxStackSize();
            int amountToAdd = Math.min(item.getAmount(), maxStackSize);
            
            ItemStack newStack = item.clone();
            newStack.setAmount(amountToAdd);
            storedItems.add(newStack);
            
            item.setAmount(item.getAmount() - amountToAdd);
        }
        
        return true;
    }

    /**
     * Adds an item to the generator's storage with capacity checking and proper stacking.
     * @param item The item to add
     * @param maxCapacity The maximum storage capacity (in individual items, not stacks)
     * @return True if the item was added successfully
     */
    public boolean addStoredItem(ItemStack item, int maxCapacity) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        
        // Check if we have space for the items
        int currentTotalItems = getTotalItemCount();
        if (currentTotalItems + item.getAmount() > maxCapacity) {
            return false; // Storage full
        }
        
        return addStoredItem(item);
    }

    /**
     * Adds multiple items to the generator's storage.
     * @param items The items to add
     */
    public void addStoredItems(Collection<ItemStack> items) {
        if (items != null) {
            for (ItemStack item : items) {
                addStoredItem(item);
            }
        }
    }

    /**
     * Removes an item from the generator's storage.
     * @param item The item to remove
     * @return True if the item was removed
     */
    public boolean removeStoredItem(ItemStack item) {
        return storedItems.remove(item);
    }

    /**
     * Clears all stored items.
     */
    public void clearStoredItems() {
        storedItems.clear();
    }

    /**
     * Gets the total number of stored item stacks.
     * @return The number of item stacks
     */
    public int getStoredItemCount() {
        return storedItems.size();
    }

    /**
     * Gets the total number of individual items (counting stack amounts).
     * @return The total number of individual items
     */
    public int getTotalItemCount() {
        int total = 0;
        for (ItemStack item : storedItems) {
            if (item != null) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /**
     * Checks if the storage is empty.
     * @return True if no items are stored
     */
    public boolean isEmpty() {
        return storedItems.isEmpty();
    }

    /**
     * Calculates the time elapsed since last generation in seconds.
     * @return The elapsed time since last generation in seconds
     */
    public long getElapsedGenerationTimeSeconds() {
        return (System.currentTimeMillis() - lastGenerationTime) / 1000;
    }

    /**
     * Calculates how many items should be generated based on elapsed time since last generation.
     * @param generationTimeSeconds The time required to generate one item
     * @param maxCapacity The maximum storage capacity (in individual items)
     * @return The number of items to generate (capped by storage capacity)
     */
    public int calculateItemsToGenerate(int generationTimeSeconds, int maxCapacity) {
        if (generationTimeSeconds <= 0) {
            return 0;
        }

        long elapsedSeconds = getElapsedGenerationTimeSeconds();
        int itemsFromTime = (int) (elapsedSeconds / generationTimeSeconds);
        int currentItemCount = getTotalItemCount(); // Use total individual items, not stacks
        int availableSpace = Math.max(0, maxCapacity - currentItemCount);
        
        return Math.min(itemsFromTime, availableSpace);
    }

    /**
     * Updates the generation time after items have been generated.
     * This advances the generation timer by the time used for the generated items.
     * @param itemsGenerated The number of items that were generated
     * @param generationTimeSeconds The time per item in seconds
     */
    public void advanceGenerationTime(int itemsGenerated, int generationTimeSeconds) {
        if (itemsGenerated > 0) {
            long timeAdvanced = (long) itemsGenerated * generationTimeSeconds * 1000; // Convert to milliseconds
            this.lastGenerationTime += timeAdvanced;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeneratorData that = (GeneratorData) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "GeneratorData{" +
               "id=" + id +
               ", location=" + location +
               ", generatorType='" + generatorType + '\'' +
               ", owner=" + owner +
               ", lastAccessedTime=" + lastAccessedTime +
               ", storedItemCount=" + storedItems.size() +
               '}';
    }
}
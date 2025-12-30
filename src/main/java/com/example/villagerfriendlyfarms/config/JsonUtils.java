package com.example.villagerfriendlyfarms.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.UUID;

/**
 * Utility class for JSON serialization/deserialization of Bukkit objects.
 */
public class JsonUtils {

    private static final ObjectMapper MAPPER = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        
        // ItemStack serialization
        module.addSerializer(ItemStack.class, new ItemStackSerializer());
        module.addDeserializer(ItemStack.class, new ItemStackDeserializer());
        
        // Location serialization
        module.addSerializer(Location.class, new LocationSerializer());
        module.addDeserializer(Location.class, new LocationDeserializer());
        
        // Material serialization
        module.addSerializer(Material.class, new MaterialSerializer());
        module.addDeserializer(Material.class, new MaterialDeserializer());
        
        mapper.registerModule(module);
        return mapper;
    }

    /**
     * Gets the configured ObjectMapper instance.
     * @return The ObjectMapper
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    // ItemStack serialization
    private static class ItemStackSerializer extends JsonSerializer<ItemStack> {
        @Override
        public void serialize(ItemStack item, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (item == null || item.getType() == null || item.getType().isAir()) {
                gen.writeNull();
                return;
            }
            
            gen.writeStartObject();
            gen.writeStringField("type", item.getType().name());
            gen.writeNumberField("amount", item.getAmount());
            // Note: For simplicity, we're not serializing meta data in this basic implementation
            // In a full implementation, you'd want to serialize item meta, enchantments, etc.
            gen.writeEndObject();
        }
    }

    private static class ItemStackDeserializer extends JsonDeserializer<ItemStack> {
        @Override
        public ItemStack deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            
            if (node.isNull()) {
                return null;
            }
            
            String typeName = node.get("type").asText();
            int amount = node.get("amount").asInt();
            
            Material material = Material.valueOf(typeName);
            return new ItemStack(material, amount);
        }
    }

    // Location serialization
    private static class LocationSerializer extends JsonSerializer<Location> {
        @Override
        public void serialize(Location loc, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (loc == null) {
                gen.writeNull();
                return;
            }
            
            World world = loc.getWorld();
            if (world == null) {
                throw new IOException("Cannot serialize Location with null world: " + loc);
            }
            
            gen.writeStartObject();
            gen.writeStringField("world", world.getName());
            gen.writeNumberField("x", loc.getX());
            gen.writeNumberField("y", loc.getY());
            gen.writeNumberField("z", loc.getZ());
            gen.writeNumberField("yaw", loc.getYaw());
            gen.writeNumberField("pitch", loc.getPitch());
            gen.writeEndObject();
        }
    }

    private static class LocationDeserializer extends JsonDeserializer<Location> {
        @Override
        public Location deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            
            if (node.isNull()) {
                return null;
            }
            
            String worldName = node.get("world").asText();
            double x = node.get("x").asDouble();
            double y = node.get("y").asDouble();
            double z = node.get("z").asDouble();
            float yaw = (float) node.get("yaw").asDouble();
            float pitch = (float) node.get("pitch").asDouble();
            
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                throw new IllegalArgumentException("World not found: " + worldName);
            }
            
            return new Location(world, x, y, z, yaw, pitch);
        }
    }

    // Material serialization
    private static class MaterialSerializer extends JsonSerializer<Material> {
        @Override
        public void serialize(Material material, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(material.name());
        }
    }

    private static class MaterialDeserializer extends JsonDeserializer<Material> {
        @Override
        public Material deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String materialName = p.getValueAsString();
            return Material.valueOf(materialName);
        }
    }
}
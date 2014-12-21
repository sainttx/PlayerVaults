package com.drtshock.playervaults;

import com.drtshock.playervaults.util.Lang;
import com.drtshock.playervaults.util.VaultHolder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Fancy JSON serialization mostly by evilmidget38.
 *
 * @author evilmidget38, gomeow
 */
public class SerializationUtil {

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> result = Maps.newHashMap();

        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            result.put(key, fromJson(object.get(key)));
        }

        return result;
    }

    private static Object fromJson(Object json) throws JSONException {
        if (json == JSONObject.NULL) {
            return null;
        } else if (json instanceof JSONObject) {
            return toMap((JSONObject) json);
        } else if (json instanceof JSONArray) {
            return toList((JSONArray) json);
        } else {
            return json;
        }
    }

    public static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> result = Lists.newArrayList();

        for (int i = 0; i < array.length(); i++) {
            result.add(fromJson(array.get(i)));
        }

        return result;
    }

    public static List<String> toString(Inventory inventory) {
        List<String> result = Lists.newArrayList();

        List<ConfigurationSerializable> items = Lists.newArrayList();
        Collections.addAll(items, inventory.getContents());
        for (ConfigurationSerializable cs : items) {
            if (cs == null) {
                result.add("null");
            } else {
                result.add(new JSONObject(serialize(cs)).toString());
            }
        }

        return result;
    }

    public static Inventory toInventory(int vaultId, int size, List<String> rawContents) {
        Inventory inventory = VaultHolder.wrapInventory(vaultId, Lang.VAULT_TITLE.format("%number", vaultId), size);

        List<ItemStack> result = Lists.newArrayList();
        for (String piece : rawContents) {
            if (piece.equalsIgnoreCase("null")) {
                result.add(null);
            } else {
                try {
                    result.add((ItemStack) deserialize(toMap(new JSONObject(piece))));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        ItemStack[] inventoryContents = new ItemStack[result.size()];
        for (int x = 0; x < result.size(); x++) {
            inventoryContents[x] = result.get(x);
        }

        inventory.setContents(inventoryContents);

        return inventory;
    }

    public static Map<String, Object> serialize(ConfigurationSerializable config) {
        Map<String, Object> result = handleSerialization(config.serialize());
        result.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(config.getClass()));

        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> handleSerialization(Map<String, Object> map) {
        Map<String, Object> serialized = recreateMap(map);

        for (Map.Entry<String, Object> entry : serialized.entrySet()) {
            if (entry.getValue() instanceof ConfigurationSerializable) {
                entry.setValue(serialize((ConfigurationSerializable) entry.getValue()));
            } else if (entry.getValue() instanceof Iterable<?>) {
                List<Object> result = Lists.newArrayList();

                for (Object object : ((Iterable) entry.getValue())) {
                    if (object instanceof ConfigurationSerializable) {
                        object = serialize((ConfigurationSerializable) object);
                    }

                    result.add(object);
                }

                entry.setValue(result);
            } else if (entry.getValue() instanceof Map<?, ?>) {
                // unchecked cast here. If you're serializing to a non-standard Map you deserve ClassCastExceptions
                entry.setValue(handleSerialization((Map<String, Object>) entry.getValue()));
            }
        }

        return serialized;
    }

    public static Map<String, Object> recreateMap(Map<String, Object> original) {
        Map<String, Object> map = Maps.newHashMap();
        map.putAll(original);

        return map;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ConfigurationSerializable deserialize(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map && ((Map) entry.getValue()).containsKey(ConfigurationSerialization.SERIALIZED_TYPE_KEY)) {
                entry.setValue(deserialize((Map) entry.getValue()));
            } else if (entry.getValue() instanceof Iterable) {
                entry.setValue(convertIterable((Iterable) entry.getValue()));
            }
        }

        return ConfigurationSerialization.deserializeObject(map);
    }

    @SuppressWarnings({"unchecked"})
    private static List<?> convertIterable(Iterable<?> iterable) {
        List<Object> result = Lists.newArrayList();

        for (Object object : iterable) {
            if (object instanceof Map) {
                object = deserialize((Map<String, Object>) object);
            } else if (object instanceof List) {
                object = convertIterable((Iterable) object);
            }

            result.add(object);
        }

        return result;
    }
}

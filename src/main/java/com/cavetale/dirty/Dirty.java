package com.cavetale.dirty;

import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.ItemStack;
import net.minecraft.server.v1_13_R2.NBTBase;
import net.minecraft.server.v1_13_R2.NBTList;
import net.minecraft.server.v1_13_R2.NBTTagByte;
import net.minecraft.server.v1_13_R2.NBTTagByteArray;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.NBTTagDouble;
import net.minecraft.server.v1_13_R2.NBTTagFloat;
import net.minecraft.server.v1_13_R2.NBTTagInt;
import net.minecraft.server.v1_13_R2.NBTTagIntArray;
import net.minecraft.server.v1_13_R2.NBTTagList;
import net.minecraft.server.v1_13_R2.NBTTagLong;
import net.minecraft.server.v1_13_R2.NBTTagLongArray;
import net.minecraft.server.v1_13_R2.NBTTagShort;
import net.minecraft.server.v1_13_R2.NBTTagString;
import net.minecraft.server.v1_13_R2.TileEntity;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;

/**
 * Utility class to get or set item, entity, or block NBT data.
 * Items may be accessed directly; for the other two one has to load,
 * modify, store.
 */
public final class Dirty {
    private static Field fieldCraftItemStackHandle = null;
    private Dirty() { }

    // --- NBT-Container converstion

    /**
     * Turn an NBT into a corresponding Container object.  Works
     * recursively on Compounds and Lists.
     *
     * @param value The NBT value.
     * @return value A raw value, such as Number, Boolean, String, Array, List, Map.
     */
    public static Object fromTag(NBTBase value) {
        if (value == null) {
            return null;
        } else if (value instanceof NBTTagCompound) {
            // Recursive
            NBTTagCompound tag = (NBTTagCompound)value;
            Map<String, Object> result = new HashMap<>();
            for (String key: tag.getKeys()) {
                result.put(key, fromTag(tag.get(key)));
            }
            return result;
        } else if (value instanceof NBTTagList) {
            // Recursive
            NBTTagList tag = (NBTTagList)value;
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < tag.size(); i += 1) {
                result.add(fromTag(tag.get(i)));
            }
            return result;
        } else if (value instanceof NBTTagString) {
            return (String)((NBTTagString)value).b_();
        } else if (value instanceof NBTTagInt) {
            return (int)((NBTTagInt)value).e();
        } else if (value instanceof NBTTagLong) {
            return (long)((NBTTagLong)value).d();
        } else if (value instanceof NBTTagShort) {
            return (short)((NBTTagShort)value).f();
        } else if (value instanceof NBTTagFloat) {
            return (float)((NBTTagFloat)value).i();
        } else if (value instanceof NBTTagDouble) {
            return (double)((NBTTagDouble)value).asDouble();
        } else if (value instanceof NBTTagByte) {
            return (byte)((NBTTagByte)value).g();
        } else if (value instanceof NBTTagByteArray) {
            byte[] l = (byte[])((NBTTagByteArray)value).c();
            List<Byte> ls = new ArrayList<>(l.length);
            for (byte b: l) ls.add(b);
            return ls;
        } else if (value instanceof NBTTagIntArray) {
            int[] l = (int[])((NBTTagIntArray)value).d();
            return Arrays.stream(l).boxed().collect(Collectors.toList());
        } else if (value instanceof NBTTagLongArray) {
            long[] l = (long[])((NBTTagLongArray)value).d();
            return Arrays.stream(l).boxed().collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException("TagWrapper.fromTag: Unsupported value type: " + value.getClass().getName());
        }
    }

    /**
     * Turn any JSON object into an NBT.  Workd recursively on
     * Maps and Lists.
     *
     * @param value The raw value, such as Number, Boolean, String, Array, List, Map.
     * @return An NBT structure.
     */
    public static NBTBase toTag(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Map) {
            // Recursive
            NBTTagCompound tag = new NBTTagCompound();
            for (Map.Entry<String, Object> e: ((Map<String, Object>)value).entrySet()) {
                tag.set(e.getKey(), toTag(e.getValue()));
            }
            return tag;
        } else if (value instanceof List) {
            // Recursive
            NBTTagList tag = new NBTTagList();
            for (Object e: (List<Object>)value) {
                tag.add(toTag(e));
            }
            return tag;
        } else if (value instanceof String) {
            return new NBTTagString((String)value);
        } else if (value instanceof Integer) {
            return new NBTTagInt((Integer)value);
        } else if (value instanceof Long) {
            return new NBTTagLong((Long)value);
        } else if (value instanceof Short) {
            return new NBTTagShort((Short)value);
        } else if (value instanceof Byte) {
            return new NBTTagByte((Byte)value);
        } else if (value instanceof Float) {
            return new NBTTagFloat((Float)value);
        } else if (value instanceof Double) {
            return new NBTTagDouble((Double)value);
        } else if (value instanceof Boolean) {
            return new NBTTagInt((Boolean)value ? 1 : 0);
        } else if (value instanceof byte[]) {
            return new NBTTagByteArray((byte[])value);
        } else if (value instanceof int[]) {
            return new NBTTagIntArray((int[])value);
        } else if (value instanceof long[]) {
            return new NBTTagLongArray((long[])value);
        } else {
            throw new IllegalArgumentException("TagWrapper.toTag: Unsupported value type: " + value.getClass().getName());
        }
    }

    private static Field getFieldCraftItemStackHandle() {
        if (fieldCraftItemStackHandle == null) {
            try {
                fieldCraftItemStackHandle = CraftItemStack.class.getDeclaredField("handle");
            } catch (NoSuchFieldException nsfe) {
                nsfe.printStackTrace();
                fieldCraftItemStackHandle = null;
            }
        }
        return fieldCraftItemStackHandle;
    }

    // --- Item tags

    /**
     * Get an item's tag in container form, that is, transformed from
     * NBT to Java language objects, ready to be saved as JSON or
     * modified.
     */
    public static Map<String, Object> getItemTag(org.bukkit.inventory.ItemStack bukkitItem) {
        try {
            if (!(bukkitItem instanceof CraftItemStack)) return null;
            CraftItemStack obcItem = (CraftItemStack)bukkitItem;
            getFieldCraftItemStackHandle().setAccessible(true);
            ItemStack nmsItem = (ItemStack)fieldCraftItemStackHandle.get(obcItem);
            if (nmsItem == null) return null;
            NBTTagCompound tag = nmsItem.getTag();
            return (Map<String, Object>)fromTag(tag);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Completely serialize an item, including `id` and `Count`.  The
     * result will be ready to be saved to JSON or deserialized, see
     * below.
     */
    public static Map<String, Object> serializeItem(org.bukkit.inventory.ItemStack bukkitItem) {
        if (bukkitItem == null) throw new NullPointerException("bukkitItem cannot be null");
        try {
            ItemStack nmsItem;
            if (bukkitItem instanceof CraftItemStack) {
                CraftItemStack obcItem = (CraftItemStack)bukkitItem;
                nmsItem = (ItemStack)fieldCraftItemStackHandle.get(obcItem);
            } else {
                nmsItem = CraftItemStack.asNMSCopy(bukkitItem);
            }
            NBTTagCompound tag = new NBTTagCompound();
            nmsItem.save(tag);
            return (Map<String, Object>)fromTag(tag);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deserialize a full item.
     * @param json The JSON structure.
     * @return The item.
     */
    public static org.bukkit.inventory.ItemStack deserializeItem(Map<String, Object> json) {
        if (json == null) throw new NullPointerException("json cannot be null");
        return ItemStack.a((NBTTagCompound)toTag(json)).asBukkitMirror();
    }

    /**
     * Deserialize a full item.
     * @param json The JSON string.
     * @return The item.
     */
    public static org.bukkit.inventory.ItemStack deserializeItem(String json) {
        if (json == null) throw new NullPointerException("json cannot be null");
        Map<String, Object> map = (Map<String, Object>)new Gson().fromJson(json, Map.class);
        return ItemStack.a((NBTTagCompound)toTag(map)).asBukkitMirror();
    }

    /**
     * This will return a new instance if `bukkitItem` is not an
     * instance of of CraftItemStack, and possibly for other reasons.
     *
     * To avoid that, create the ItemStack with one of the methods
     * below, or retrieve them from a running Minecraft server.  In
     * other words, do not just use the result of ItemStack::new.
     */
    public static org.bukkit.inventory.ItemStack setItemTag(org.bukkit.inventory.ItemStack bukkitItem, Map<String, Object> json) {
        try {
            CraftItemStack obcItem;
            ItemStack nmsItem;
            if (bukkitItem instanceof CraftItemStack) {
                obcItem = (CraftItemStack)bukkitItem;
                getFieldCraftItemStackHandle().setAccessible(true);
                nmsItem = (ItemStack)fieldCraftItemStackHandle.get(obcItem);
            } else {
                nmsItem = CraftItemStack.asNMSCopy(bukkitItem);
                obcItem = CraftItemStack.asCraftMirror(nmsItem);
            }
            // if (!nmsItem.hasTag()) {
            //     nmsItem.setTag(new NBTTagCompound());
            // }
            NBTTagCompound tag = (NBTTagCompound)toTag(json);
            nmsItem.setTag(tag);
            return obcItem;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- Block tags

    public static Map<String, Object> getBlockTag(org.bukkit.block.Block bukkitBlock) {
        CraftWorld craftWorld = (CraftWorld)bukkitBlock.getWorld();
        TileEntity tileEntity = craftWorld.getTileEntityAt(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ());
        if (tileEntity == null) return null;
        NBTTagCompound tag = new NBTTagCompound();
        tileEntity.save(tag);
        return (Map<String, Object>)fromTag(tag);
    }

    public static Map<String, Object> getBlockTag(org.bukkit.block.BlockState bukkitBlockState) {
        if (!(bukkitBlockState instanceof CraftBlockEntityState)) return null;
        CraftBlockEntityState cbes = (CraftBlockEntityState)bukkitBlockState;
        NBTTagCompound tag = cbes.getSnapshotNBT();
        return (Map<String, Object>)fromTag(tag);
    }

    public static boolean setBlockTag(org.bukkit.block.Block bukkitBlock, Map<String, Object> json) {
        CraftWorld craftWorld = (CraftWorld)bukkitBlock.getWorld();
        TileEntity tileEntity = craftWorld.getTileEntityAt(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ());
        if (tileEntity == null) return false;
        NBTTagCompound tag = (NBTTagCompound)toTag(json);
        tileEntity.load(tag);
        return true;
    }

    // --- Entity tags

    public static Map<String, Object> getEntityTag(org.bukkit.entity.Entity entity) {
        Entity nmsEntity = ((CraftEntity)entity).getHandle();
        NBTTagCompound tag = new NBTTagCompound();
        nmsEntity.save(tag);
        return (Map<String, Object>)fromTag(tag);
    }

    public static void setEntityTag(org.bukkit.entity.Entity entity, Map<String, Object> json) {
        Entity nmsEntity = ((CraftEntity)entity).getHandle();
        NBTTagCompound tag = (NBTTagCompound)toTag(json);
        nmsEntity.f(tag);
    }

    // --- Item NBT access.  For now, will only work on items.

    public static Optional<Object> accessItemNBT(org.bukkit.inventory.ItemStack bukkitItem, boolean create) {
        try {
            if (!(bukkitItem instanceof CraftItemStack)) return Optional.empty();
            CraftItemStack obcItem = (CraftItemStack)bukkitItem;
            getFieldCraftItemStackHandle().setAccessible(true);
            ItemStack nmsItem = (ItemStack)fieldCraftItemStackHandle.get(obcItem);
            if (nmsItem == null) return Optional.empty();
            NBTTagCompound tag = nmsItem.getTag();
            if (tag == null) {
                if (create) {
                    tag = new NBTTagCompound();
                    nmsItem.setTag(tag);
                } else {
                    return Optional.empty();
                }
            }
            return Optional.of(tag);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static CraftItemStack newCraftItemStack(org.bukkit.Material bukkitMaterial) {
        return CraftItemStack.asCraftCopy(new org.bukkit.inventory.ItemStack(bukkitMaterial));
    }

    public static CraftItemStack toCraftItemStack(org.bukkit.inventory.ItemStack bukkitItem) {
        if (bukkitItem instanceof CraftItemStack) return (CraftItemStack)bukkitItem;
        ItemStack nmsItem = CraftItemStack.asNMSCopy(bukkitItem);
        return CraftItemStack.asCraftMirror(nmsItem);
    }

    // --- NBT modification

    /**
     * Set a key-value pair in a compound.  This will wrap the value
     * in the approproate NBTBase subclass.
     *
     * @param opt Optional wrapping NBTTagCompound.
     * @param key The key.
     * @param value The raw value, such as Number, Boolean, String, Array, List, Map.
     * @return The created NBT value.
     * @throws IllegalArgumentException if opt does not wrap an NBTTagCompound.
     */
    public static Optional<Object> setNBT(Optional<Object> opt, String key, Object value) {
        if (!opt.isPresent()) throw new NullPointerException("Tag cannot be null");
        if (!(opt.get() instanceof NBTTagCompound)) throw new IllegalArgumentException("Expected tag compound: " + opt.get().getClass().getName());
        NBTTagCompound tag = (NBTTagCompound)opt.get();
        if (value == null) {
            tag.remove(key);
            return null;
        } else {
            NBTBase result = toTag(value);
            tag.set(key, result);
            return Optional.of(result);
        }
    }

    /**
     * Set a value at an index in an NBT list.  This will wrap the
     * value in the approproate NBTBase subclass.
     *
     * @param opt Optional wrapping NBTTagList.
     * @param index The index.
     * @param value The raw value, such as Number, Boolean, String, Array, List, Map.
     * @return The created NBT value.
     * @throws IllegalArgumentException if opt does not wrap an NBTTagList.
     */
    public static Optional<Object> setNBT(Optional<Object> opt, int index, Object value) {
        if (!opt.isPresent()) throw new NullPointerException("Tag cannot be null");
        if (!(opt.get() instanceof NBTTagList)) throw new IllegalArgumentException("Expected tag list: " + opt.get().getClass().getName());
        NBTList<NBTBase> list = (NBTList<NBTBase>)opt.get();
        NBTBase result = toTag(value);
        list.set(index, result);
        return Optional.of(result);
    }

    /**
     * Append a value to an NBT list.  This will wrap the value in the
     * approproate NBTBase subclass.
     *
     * @param opt Optional wrapping NBTTagList.
     * @param value The raw value, such as Number, Boolean, String, Array, List, Map.
     * @return The added NBT value.
     * @throws IllegalArgumentException if opt does not wrap an NBTTagList.
     */
    public static Optional<Object> addNBT(Optional<Object> opt, Object value) {
        if (!opt.isPresent()) throw new NullPointerException("Tag cannot be null");
        if (!(opt.get() instanceof NBTTagList)) throw new IllegalArgumentException("Expected tag list: " + opt.get().getClass().getName());
        NBTList<NBTBase> list = (NBTList<NBTBase>)opt.get();
        NBTBase result = toTag(value);
        list.add(result);
        return Optional.of(result);
    }

    /**
     * Retrieve the value for a key from a compound.
     *
     * @param opt Optional wrapping NBTagCompound.
     * @param key The key.
     * @throws IllegalArgumentException if opt does not wrap an NBTTagCompound.
     */
    public static Optional<Object> getNBT(Optional<Object> opt, String key) {
        if (!opt.isPresent()) throw new NullPointerException("Tag cannot be null");
        if (!(opt.get() instanceof NBTTagCompound)) throw new IllegalArgumentException("Expected tag compound: " + opt.get().getClass().getName());
        NBTTagCompound tag = (NBTTagCompound)opt.get();
        return Optional.ofNullable(tag.get(key));
    }

    /**
     * Retrieve the value at an index from a list.
     *
     * @param opt Optional wrapping NBTagCompound.
     * @param index The index.
     * @throws IllegalArgumentException if opt does not wrap an NBTList.
     */
    public static Optional<Object> getNBT(Optional<Object> opt, int index) {
        if (!opt.isPresent()) throw new NullPointerException("Tag cannot be null");
        if (opt.get() instanceof NBTTagList) {
            NBTTagList tag = (NBTTagList)opt.get();
            return Optional.ofNullable(tag.get(index));
        } else if (opt.get() instanceof NBTList) {
            NBTList tag = (NBTList)opt.get();
            return Optional.ofNullable(tag.get(index));
        } else {
            throw new IllegalArgumentException("Expected list or tag list: " + opt.get().getClass().getName());
        }
    }

    /**
     * Turn an Optional wrapping any NBTBase subclass into a Java
     * value, ready to be stored as JSON or modified.
     *
     * @param opt Optional wrapping NBTBase subclass.
     * @return The Java contained Object or null.
     */
    public static Object fromNBT(Optional<Object> opt) {
        Object o = opt.orElse(null);
        if (o instanceof NBTBase) return fromTag((NBTBase)o);
        return null;
    }
}

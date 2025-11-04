package dlindustries.vigillant.system.utils;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Set;

public class EnchantmentUtil {
    public static boolean hasEnchantment(final ItemStack itemStack, final RegistryKey<?> registryKey) {
        if (itemStack.isEmpty()) {
            return false;
        }
        final Object2IntArrayMap<?> enchantmentMap = new Object2IntArrayMap<>();
        populateEnchantmentMap(itemStack, enchantmentMap);
        return containsEnchantment(enchantmentMap, registryKey);
    }

    private static boolean containsEnchantment(final Object2IntMap<?> enchantmentMap, final RegistryKey<?> registryKey) {
        for (Object enchantment : enchantmentMap.keySet()) {
            if (((RegistryEntry) enchantment).matchesKey(registryKey)) {
                return true;
            }
        }
        return false;
    }

    public static void populateEnchantmentMap(final ItemStack itemStack, final Object2IntMap enchantmentMap) {
        enchantmentMap.clear();
        if (!itemStack.isEmpty()) {
            Set<?> enchantments;
            if (itemStack.getItem() == Items.ENCHANTED_BOOK) {
                enchantments = itemStack.get(DataComponentTypes.STORED_ENCHANTMENTS).getEnchantmentEntries();
            } else {
                enchantments = itemStack.getEnchantments().getEnchantmentEntries();
            }
            for (final Object enchantmentEntry : enchantments) {
                enchantmentMap.put(((Object2IntMap.Entry<?>) enchantmentEntry).getKey(), ((Object2IntMap.Entry<?>) enchantmentEntry).getIntValue());
            }
        }
    }
}
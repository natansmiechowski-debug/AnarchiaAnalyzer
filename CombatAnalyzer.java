package pl.anarchia.analyzer;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;

public class CombatAnalyzer {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public enum Result {
        EASY("ŁATWO", 0xFF55FF55),
        MEDIUM("ŚREDNIO", 0xFFFFFF55),
        RUN("UCIEKAJ!", 0xFFFF5555);

        public final String label;
        public final int color;

        Result(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    private static final double ADVANTAGE_RATIO = 1.25;

    public static Result compare(PlayerEntity self, PlayerEntity target) {
        double mine = score(self);
        double theirs = score(target);

        if (mine >= theirs * ADVANTAGE_RATIO) {
            return Result.EASY;
        }
        if (theirs >= mine * ADVANTAGE_RATIO) {
            return Result.RUN;
        }
        return Result.MEDIUM;
    }

    public static double score(PlayerEntity player) {
        return armorScore(player) + weaponScore(player) + totemScore(player) + healthScore(player);
    }

    private static double armorScore(PlayerEntity player) {
        double points = player.getArmor() * 2.0;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                points += enchantLevel(stack, Enchantments.PROTECTION) * 3.0;
            }
        }
        return points;
    }

    private static int enchantLevel(ItemStack stack, net.minecraft.registry.RegistryKey<Enchantment> key) {
        ItemEnchantmentsComponent enchants = stack.getEnchantments();
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchants.getEnchantmentEntries()) {
            if (entry.getKey().matchesKey(key)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    private static double weaponScore(PlayerEntity player) {
        ItemStack weapon = player.getMainHandStack();
        if (weapon.isEmpty()) {
            return 0;
        }

        double base = weaponBase(weapon.getItem());
        if (base == 0) {
            return 0;
        }
        return base + enchantLevel(weapon, Enchantments.SHARPNESS) * 1.5;
    }

    private static double weaponBase(Item item) {
        if (item == Items.NETHERITE_SWORD) return 14;
        if (item == Items.NETHERITE_AXE)   return 15;
        if (item == Items.DIAMOND_SWORD)   return 12;
        if (item == Items.DIAMOND_AXE)     return 13;
        if (item == Items.IRON_SWORD)      return 9;
        if (item == Items.IRON_AXE)        return 10;
        if (item == Items.STONE_SWORD || item == Items.STONE_AXE) return 6;
        if (item == Items.GOLDEN_SWORD || item == Items.GOLDEN_AXE
                || item == Items.WOODEN_SWORD || item == Items.WOODEN_AXE) return 4;
        return 0;
    }

    private static double totemScore(PlayerEntity player) {
        if (player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            return 15;
        }
        if (player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            return 8;
        }
        return 0;
    }

    private static double healthScore(PlayerEntity player) {
        return (player.getHealth() + player.getAbsorptionAmount()) * 1.5;
    }
}

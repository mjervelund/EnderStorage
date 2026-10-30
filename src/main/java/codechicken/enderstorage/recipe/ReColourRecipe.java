package codechicken.enderstorage.recipe;

import codechicken.enderstorage.api.Frequency;
import codechicken.enderstorage.init.EnderStorageModContent;
import codechicken.lib.colour.EnumColour;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.covers1624.quack.collection.ColUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by covers1624 on 8/07/2017.
 */
public class ReColourRecipe implements CraftingRecipe {

    public static final MapCodec<ReColourRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(e -> e.group),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(e -> e.result)
            ).apply(builder, ReColourRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ReColourRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, e -> e.group,
            ItemStackTemplate.STREAM_CODEC, e -> e.result,
            ReColourRecipe::new
    );

    protected final String group;
    protected final ItemStackTemplate result;
    protected final Ingredient ingredient;

    public ReColourRecipe(ItemStackTemplate result) {
        this("", result);
    }

    public ReColourRecipe(String group, ItemStackTemplate result) {
        this.group = group;
        this.result = result;
        ingredient = Ingredient.of(result.item().value());
    }

    @Override
    public boolean matches(CraftingInput inv, Level worldIn) {
        if (inv.isEmpty()) {
            return false;
        }
        ItemWithPos chest = findChest(inv);
        if (chest == null || chest.y == 0) {
            return false;
        }
        List<ItemWithPos> validPositions = new ArrayList<>();
        validPositions.add(chest);

        EnumColour[] colours = findDyes(inv, chest, validPositions);
        if (colours == null) return false;

        for (int x = 0; x < inv.width(); x++) {
            for (int y = 0; y < inv.height(); y++) {
                ItemStack stack = inv.getItem(x + y * inv.width());
                if (!stack.isEmpty() && !validPositions.contains(new ItemWithPos(x, y, stack))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput inv) {
        ItemWithPos chestPos = findChest(inv);
        if (chestPos == null) return result.create(); // This should not happen...

        EnumColour[] colours = findDyes(inv, chestPos, null);
        if (colours == null) return result.create(); // This should also not happen...

        return Frequency.getComponentOrEmpty(chestPos.stack)
                .withColours(colours)
                .putComponent(result.create());
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return EnderStorageModContent.RECOLOUR_RECIPE_SERIALIZER.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public String group() {
        return group;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    private @Nullable ItemWithPos findChest(CraftingInput inv) {
        ItemWithPos chest = null;
        for (int x = 0; x < inv.width(); x++) {
            for (int y = 0; y < inv.height(); y++) {
                ItemStack stack = inv.getItem(x, y);
                if (stack.isEmpty()) continue;
                if (!ingredient.test(stack)) continue;

                chest = new ItemWithPos(x, y, stack);
                break;
            }
        }
        return chest;
    }

    private EnumColour @Nullable [] findDyes(CraftingInput inv, ItemWithPos chest, @Nullable List<ItemWithPos> validPositions) {
        EnumColour[] colours = new EnumColour[] { null, null, null };
        for (int x = 0; x < inv.width(); x++) {
            for (int y = 0; y < chest.y; y++) {
                ItemStack stack = inv.getItem(x, y);
                if (stack.isEmpty()) continue;

                EnumColour colour = EnumColour.fromDyeStack(stack);
                if (colour == null) continue; // Not a dye.

                int effectiveColour = chest.x == x ? 1 : chest.x < x ? 2 : 0;

                if (colours[effectiveColour] != null) {
                    EnumColour merge = EnumColour.mix(colours[effectiveColour], colour);
                    if (merge == null || merge == colour) return null;

                    colours[effectiveColour] = merge;
                } else {
                    colours[effectiveColour] = colour;
                }
                if (validPositions != null) {
                    validPositions.add(new ItemWithPos(x, y, stack));
                }
            }
        }
        return !ColUtils.allMatch(colours, Objects::isNull) ? colours : null;
    }

    private record ItemWithPos(int x, int y, ItemStack stack) { }
}

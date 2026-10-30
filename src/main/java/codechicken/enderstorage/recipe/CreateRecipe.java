package codechicken.enderstorage.recipe;

import codechicken.enderstorage.api.Frequency;
import codechicken.enderstorage.init.EnderStorageModContent;
import codechicken.lib.colour.EnumColour;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;

/**
 * Created by covers1624 on 1/11/19.
 */
public class CreateRecipe extends ShapedRecipe {

    public static final MapCodec<CreateRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(e -> e.commonInfo),
                    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(e -> e.bookInfo),
                    ShapedRecipePattern.MAP_CODEC.forGetter(e -> e.pattern),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(e -> e.result)
            ).apply(builder, CreateRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CreateRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, e -> e.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, e -> e.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, e -> e.pattern,
            ItemStackTemplate.STREAM_CODEC, e -> e.result,
            CreateRecipe::new
    );

    // ShapedRecipe keeps its result private, so carry our own copy for the codecs.
    private final ItemStackTemplate result;

    public CreateRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern, ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
        this.result = result;
    }

    @Override
    public ItemStack assemble(CraftingInput inv) {
        EnumColour colour = EnumColour.WHITE;
        // Create recipe only has a single wool.
        // We find it and set the colour to that.
        finish:
        for (int x = 0; x < inv.width(); x++) {
            for (int y = 0; y < inv.height(); y++) {
                ItemStack stack = inv.getItem(x, y);
                if (!stack.isEmpty()) {
                    EnumColour c = EnumColour.fromWoolStack(stack);
                    if (c != null) {
                        colour = c;
                        break finish;
                    }
                }
            }
        }
        Frequency frequency = new Frequency(colour, colour, colour);
        return frequency.putComponent(super.assemble(inv));
    }

    @Override
    @SuppressWarnings ("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (Object) EnderStorageModContent.CREATE_RECIPE_SERIALIZER.get();
    }
}

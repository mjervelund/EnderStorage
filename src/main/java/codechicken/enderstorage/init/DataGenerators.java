package codechicken.enderstorage.init;

import codechicken.enderstorage.client.model.BagFrequencySelectProperty;
import codechicken.enderstorage.client.model.BagOpenModelCondition;
import codechicken.enderstorage.client.model.BagOwnedModelCondition;
import codechicken.enderstorage.client.render.item.EnderChestItemRender;
import codechicken.enderstorage.client.render.item.EnderTankItemRender;
import codechicken.enderstorage.recipe.CreateRecipe;
import codechicken.enderstorage.recipe.ReColourRecipe;
import codechicken.lib.colour.EnumColour;
import codechicken.lib.datagen.recipe.RecipeProvider;
import codechicken.lib.util.CCLTags;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static codechicken.enderstorage.EnderStorage.MOD_ID;
import static codechicken.enderstorage.init.EnderStorageModContent.*;

/**
 * Created by covers1624 on 4/25/20.
 */
public class DataGenerators {

    private static final CrashLock LOCK = new CrashLock("Already Initialized.");

    public static void init(IEventBus modBus) {
        LOCK.lock();

        if (FMLEnvironment.getDist().isClient()) {
            modBus.addListener(DataGenerators::gatherDataGenerators);
        }
    }

    private static void gatherDataGenerators(GatherDataEvent.Client event) {
        event.createProvider(Models::new);
        event.createProvider(BlockTagGen::new);
        event.createProvider(Recipes::new);
    }

    private static class Models extends ModelProvider {

        public Models(PackOutput output) {
            super(output, MOD_ID);
        }

        @Override
        protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
            blockModels.createParticleOnlyBlock(ENDER_CHEST_BLOCK.get(), Blocks.OBSIDIAN);
            blockModels.createParticleOnlyBlock(ENDER_TANK_BLOCK.get(), Blocks.OBSIDIAN);

            Identifier chestBase = ModelTemplates.CHEST_INVENTORY.create(
                    ModelLocationUtils.getModelLocation(ENDER_CHEST_ITEM.get()),
                    TextureMapping.particle(Blocks.OBSIDIAN),
                    blockModels.modelOutput
            );
            itemModels.itemModelOutput.accept(
                    ENDER_CHEST_ITEM.get(),
                    ItemModelUtils.specialModel(chestBase, new EnderChestItemRender.Unbaked())
            );

            Identifier tankBase = ModelTemplates.CHEST_INVENTORY.create(
                    ModelLocationUtils.getModelLocation(ENDER_TANK_ITEM.get()),
                    TextureMapping.particle(Blocks.OBSIDIAN),
                    blockModels.modelOutput
            );
            itemModels.itemModelOutput.accept(
                    ENDER_TANK_ITEM.get(),
                    ItemModelUtils.specialModel(tankBase, new EnderTankItemRender.Unbaked())
            );

            itemModels.itemModelOutput.accept(
                    ENDER_POUCH.get(),
                    ItemModelUtils.composite(
                            ItemModelUtils.conditional(
                                    new BagOwnedModelCondition(),
                                    ItemModelUtils.conditional(
                                            new BagOpenModelCondition(),
                                            ItemModelUtils.plainModel(customFlat(itemModels, "items/ender_pouch_owned_open", "item/pouch/owned_open")),
                                            ItemModelUtils.plainModel(customFlat(itemModels, "items/ender_pouch_owned_closed", "item/pouch/owned_closed"))
                                    ),
                                    ItemModelUtils.conditional(
                                            new BagOpenModelCondition(),
                                            ItemModelUtils.plainModel(customFlat(itemModels, "items/ender_pouch_open", "item/pouch/open")),
                                            ItemModelUtils.plainModel(customFlat(itemModels, "items/ender_pouch_closed", "item/pouch/closed"))
                                    )
                            ),
                            genSide(itemModels, BagFrequencySelectProperty.ColourLocation.LEFT),
                            genSide(itemModels, BagFrequencySelectProperty.ColourLocation.MIDDLE),
                            genSide(itemModels, BagFrequencySelectProperty.ColourLocation.RIGHT)
                    )
            );
        }

        private ItemModel.Unbaked genSide(ItemModelGenerators itemModels, BagFrequencySelectProperty.ColourLocation loc) {
            Map<EnumColour, ItemModel.Unbaked> colourModels = new HashMap<>();
            for (EnumColour colour : EnumColour.values()) {
                var model = customFlat(
                        itemModels,
                        "ender_pouch_button_" + loc.getSerializedName() + "_" + colour.getSerializedName(),
                        "item/pouch/buttons/" + loc.getSerializedName() + "/" + colour.getSerializedName()
                );
                colourModels.put(colour, ItemModelUtils.plainModel(model));
            }

            return ItemModelUtils.select(
                    new BagFrequencySelectProperty(loc),
                    colourModels.get(EnumColour.WHITE),
                    FastStream.of(EnumColour.values())
                            .map(e -> ItemModelUtils.when(e, colourModels.get(e)))
                            .toList()
            );
        }

        private Identifier customFlat(ItemModelGenerators itemModels, String path, String texture) {
            return ModelTemplates.FLAT_ITEM.create(modLocation(path), TextureMapping.layer0(new Material(modLocation(texture))), itemModels.modelOutput);
        }
    }

    private static class BlockTagGen extends BlockTagsProvider {

        public BlockTagGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider, MOD_ID);
        }

        @Override
        protected void addTags(HolderLookup.Provider pProvider) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .add(ENDER_CHEST_BLOCK.get().builtInRegistryHolder().key())
                    .add(ENDER_TANK_BLOCK.get().builtInRegistryHolder().key());
        }
    }

    private static class Recipes extends RecipeProvider {

        public Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider, MOD_ID);
        }

        @Override
        protected void registerRecipes() {
            customShaped(ENDER_POUCH, (group, category, pattern, stack, showNotification) -> new CreateRecipe(new Recipe.CommonInfo(showNotification), new CraftingRecipe.CraftingBookInfo(category, group), pattern, stack))
                    .key('P', Tags.Items.ENDER_PEARLS)
                    .key('L', Tags.Items.LEATHERS)
                    .key('B', Items.BLAZE_POWDER)
                    .key('W', CCLTags.Items.WOOLS)
                    .patternLine("BLB")
                    .patternLine("LPL")
                    .patternLine("BWB");

            customShaped((ItemLike) ENDER_CHEST_ITEM, (group, category, pattern, stack, showNotification) -> new CreateRecipe(new Recipe.CommonInfo(showNotification), new CraftingRecipe.CraftingBookInfo(category, group), pattern, stack))
                    .key('P', Tags.Items.ENDER_PEARLS)
                    .key('O', Tags.Items.OBSIDIANS)
                    .key('C', Tags.Items.CHESTS_WOODEN)
                    .key('B', Items.BLAZE_ROD)
                    .key('W', CCLTags.Items.WOOLS)
                    .patternLine("BWB")
                    .patternLine("OCO")
                    .patternLine("BPB");
            customShaped((ItemLike) ENDER_TANK_ITEM, (group, category, pattern, stack, showNotification) -> new CreateRecipe(new Recipe.CommonInfo(showNotification), new CraftingRecipe.CraftingBookInfo(category, group), pattern, stack))
                    .key('P', Tags.Items.ENDER_PEARLS)
                    .key('O', Tags.Items.OBSIDIANS)
                    .key('C', Items.CAULDRON)
                    .key('B', Items.BLAZE_ROD)
                    .key('W', CCLTags.Items.WOOLS)
                    .patternLine("BWB")
                    .patternLine("OCO")
                    .patternLine("BPB");

            special(Identifier.fromNamespaceAndPath(MOD_ID, "recolour_ender_pouch"), () -> new ReColourRecipe(new ItemStackTemplate(ENDER_POUCH.get())));
            special(Identifier.fromNamespaceAndPath(MOD_ID, "recolour_ender_chest"), () -> new ReColourRecipe(new ItemStackTemplate(ENDER_CHEST_ITEM.get())));
            special(Identifier.fromNamespaceAndPath(MOD_ID, "recolour_ender_tank"), () -> new ReColourRecipe(new ItemStackTemplate(ENDER_TANK_ITEM.get())));
        }
    }
}

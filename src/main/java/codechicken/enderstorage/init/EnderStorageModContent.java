package codechicken.enderstorage.init;

import codechicken.enderstorage.api.Frequency;
import codechicken.enderstorage.block.BlockEnderChest;
import codechicken.enderstorage.block.BlockEnderTank;
import codechicken.enderstorage.container.ContainerEnderItemStorage;
import codechicken.enderstorage.item.ItemEnderPouch;
import codechicken.enderstorage.item.ItemEnderStorage;
import codechicken.enderstorage.recipe.CreateRecipe;
import codechicken.enderstorage.recipe.ReColourRecipe;
import codechicken.enderstorage.tile.TileEnderChest;
import codechicken.enderstorage.tile.TileEnderTank;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static codechicken.enderstorage.EnderStorage.MOD_ID;

/**
 * Created by covers1624 on 29/10/19.
 */
public class EnderStorageModContent {

    private static final CrashLock LOCK = new CrashLock("Already Initialized.");
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD_ID);

    public static final DeferredBlock<BlockEnderChest> ENDER_CHEST_BLOCK = BLOCKS.registerBlock(
            "ender_chest",
            BlockEnderChest::new,
            e -> e.mapColor(MapColor.STONE)
                    .strength(20, 100)
    );
    public static final DeferredBlock<BlockEnderTank> ENDER_TANK_BLOCK = BLOCKS.registerBlock(
            "ender_tank",
            BlockEnderTank::new,
            e -> e.mapColor(MapColor.STONE)
                    .strength(20, 100)
    );

    public static final DeferredItem<ItemEnderStorage> ENDER_CHEST_ITEM = ITEMS.registerItem(
            "ender_chest",
            e -> new ItemEnderStorage(ENDER_CHEST_BLOCK.get(), e)
    );
    public static final DeferredItem<ItemEnderStorage> ENDER_TANK_ITEM = ITEMS.registerItem(
            "ender_tank",
            e -> new ItemEnderStorage(ENDER_TANK_BLOCK.get(), e)
    );

    public static final DeferredHolder<Item, ItemEnderPouch> ENDER_POUCH = ITEMS.registerItem(
            "ender_pouch",
            ItemEnderPouch::new,
            e -> e.stacksTo(1)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEnderChest>> ENDER_CHEST_TILE = BLOCK_ENTITY_TYPES.register("ender_chest", () ->
            new BlockEntityType<>(TileEnderChest::new, ENDER_CHEST_BLOCK.get())
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEnderTank>> ENDER_TANK_TILE = BLOCK_ENTITY_TYPES.register("ender_tank", () ->
            new BlockEntityType<>(TileEnderTank::new, ENDER_TANK_BLOCK.get())
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Frequency>> FREQUENCY_DATA_COMPONENT = DATA_COMPONENTS.register("frequency", () ->
            DataComponentType.<Frequency>builder()
                    .persistent(Frequency.CODEC)
                    .networkSynchronized(Frequency.STREAM_CODEC)
                    .build()
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ContainerEnderItemStorage>> ENDER_ITEM_STORAGE = MENU_TYPES.register("ender_item_storage", () ->
            IMenuTypeExtension.create(ContainerEnderItemStorage::new)
    );

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CreateRecipe>> CREATE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("create_recipe",
            () -> new RecipeSerializer<>(CreateRecipe.CODEC, CreateRecipe.STREAM_CODEC)
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ReColourRecipe>> RECOLOUR_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("recolour_recipe",
            () -> new RecipeSerializer<>(ReColourRecipe.CODEC, ReColourRecipe.STREAM_CODEC)
    );

    public static void init(IEventBus modBus) {
        LOCK.lock();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        DATA_COMPONENTS.register(modBus);
        MENU_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        modBus.addListener(EnderStorageModContent::onCreativeTabBuild);
        modBus.addListener(EnderStorageModContent::onRegisterCaps);
    }

    private static void onCreativeTabBuild(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ENDER_POUCH.get());
            event.accept(ENDER_CHEST_BLOCK.get());
            event.accept(ENDER_TANK_BLOCK.get());
        }
    }

    private static void onRegisterCaps(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, ENDER_CHEST_TILE.get(), (object, context) -> object.getItemHandler());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ENDER_TANK_TILE.get(), (object, context) -> object.getFluidHandler());
    }
}

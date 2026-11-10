package codechicken.enderstorage.manager;

import codechicken.enderstorage.api.AbstractEnderStorage;
import codechicken.enderstorage.api.EnderStoragePlugin;
import codechicken.enderstorage.api.Frequency;
import codechicken.enderstorage.api.StorageType;
import codechicken.lib.util.ServerUtils;
import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class EnderStorageManager {

    public static class EnderStorageSaveHandler {

        @SubscribeEvent
        public void onWorldLoad(LevelEvent.Load event) {
            if (event.getLevel().isClientSide()) {
                reloadManager(true);
            }
        }

        @SubscribeEvent
        public void onWorldSave(LevelEvent.Save event) {
            if (!event.getLevel().isClientSide() && instance(false) != null) {
                instance(false).save(false);
            }
        }

        @SubscribeEvent
        public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            instance(false).sendClientInfo((ServerPlayer) event.getEntity());
        }

        @SubscribeEvent
        public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            instance(false).sendClientInfo((ServerPlayer) event.getEntity());
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(EnderStorageManager.class);

    private static @Nullable EnderStorageManager serverManager;
    private static @Nullable EnderStorageManager clientManager;
    private static Map<StorageType<?>, EnderStoragePlugin<?>> plugins = new HashMap<>();

    private Map<String, AbstractEnderStorage> storageMap;
    private Map<StorageType<?>, List<AbstractEnderStorage>> storageList;
    public final boolean client;

    private File saveDir;
    private File[] saveFiles;
    private int saveTo;
    private List<AbstractEnderStorage> dirtyStorage;
    private CompoundTag saveTag;

    public EnderStorageManager(boolean client) {
        this.client = client;

        storageMap = Collections.synchronizedMap(new HashMap<>());
        storageList = Collections.synchronizedMap(new HashMap<>());
        dirtyStorage = Collections.synchronizedList(new LinkedList<>());

        for (StorageType<?> key : plugins.keySet()) {
            storageList.put(key, new ArrayList<>());
        }

        if (!client) {
            load();
        }
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(EnderStorageManager::onServerStarted);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        EnderStorageManager.reloadManager(false);
    }

    private void sendClientInfo(ServerPlayer player) {
        for (Map.Entry<StorageType<?>, EnderStoragePlugin<?>> plugin : plugins.entrySet()) {
            plugin.getValue().sendClientInfo(player, unsafeCast(storageList.get(plugin.getKey())));
        }
    }

    @SuppressWarnings ("unchecked")
    private static <T> T unsafeCast(Object object) {
        return (T) object;
    }

    private void load() {
        saveDir = new File(ServerUtils.getSaveDirectory().toFile(), "EnderStorage");
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
        //data1 and data2 are written alternately, lock holds the index of the last one written (and is only written
        //after that data file). Every file is replaced atomically (written to a temp file, then moved over it), so a
        //crash mid-save can no longer leave a truncated file. Saves made by older versions may still hold truncated
        //files, so loading falls back to the other data file instead of starting empty or failing.
        saveFiles = new File[] { new File(saveDir, "data1.dat"), new File(saveDir, "data2.dat"), new File(saveDir, "lock.dat") };

        int current = readLock(saveFiles[2]);
        List<Integer> candidates = new ArrayList<>(2);
        if (current >= 0) {
            candidates.add(current);
            candidates.add(current ^ 1);
        } else {
            //No usable lock: try the newest data file first.
            candidates.add(0);
            candidates.add(1);
            candidates.sort(Comparator.comparingLong((Integer i) -> saveFiles[i].lastModified()).reversed());
            if (saveFiles[0].exists() || saveFiles[1].exists()) {
                LOGGER.warn("EnderStorage: {} is missing or unreadable, loading the newest readable data file instead.", saveFiles[2]);
            }
        }

        saveTag = null;
        boolean sawData = false;
        for (int idx : candidates) {
            File file = saveFiles[idx];
            if (!file.exists()) continue;
            sawData = true;
            try (FileInputStream in = new FileInputStream(file)) {
                saveTag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            } catch (Exception e) {
                File copy = keepCorruptCopy(file);
                LOGGER.error("EnderStorage: unable to read {} ({}); kept a copy as {}, trying the other data file.", file, e.toString(), copy);
                continue;
            }
            if (current >= 0 && idx != current) {
                LOGGER.warn("EnderStorage: recovered ender storage contents from the older save {}; changes made since that save are lost.", file);
            }
            saveTo = idx ^ 1; //Never overwrite the file we just loaded on the next save.
            break;
        }
        if (saveTag == null) {
            if (sawData) {
                LOGGER.error("EnderStorage: no readable data file in {}, starting with empty ender storage. The unreadable files were kept as *.corrupt-*.", saveDir);
            }
            saveTag = new CompoundTag();
            saveTo = current >= 0 ? current ^ 1 : 0;
        }
    }

    /**
     * @return The data file index stored in the lock file, or -1 if it is missing, empty or invalid.
     */
    private static int readLock(File lock) {
        if (!lock.exists()) return -1;
        try (FileInputStream in = new FileInputStream(lock)) {
            int v = in.read();
            return v == 0 || v == 1 ? v : -1;
        } catch (IOException e) {
            LOGGER.error("EnderStorage: unable to read {}.", lock, e);
            return -1;
        }
    }

    private static @Nullable File keepCorruptCopy(File file) {
        File copy = new File(file.getParentFile(), file.getName() + ".corrupt-" + System.currentTimeMillis());
        try {
            Files.copy(file.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return copy;
        } catch (IOException e) {
            LOGGER.error("EnderStorage: unable to copy {} to {}.", file, copy, e);
            return null;
        }
    }

    /**
     * Replaces {@code target} with {@code data}: written and synced to a temp file next to it, then moved over it,
     * so the target is never left truncated.
     */
    private static void writeAtomically(File target, byte[] data) throws IOException {
        Path dest = target.toPath();
        Path tmp = dest.resolveSibling(target.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp.toFile())) {
            out.write(data);
            out.getFD().sync();
        }
        try {
            Files.move(tmp, dest, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void save(boolean force) {
        if (!dirtyStorage.isEmpty() || force) {
            for (AbstractEnderStorage inv : dirtyStorage) {
                try (var reporter = new ProblemReporter.ScopedCollector(LOGGER)) {
                    var output = TagValueOutput.createWithContext(reporter, ServerLifecycleHooks.getCurrentServer().registryAccess());
                    inv.saveToTag(output);
                    saveTag.put(inv.freq + ",type=" + inv.type(), output.buildResult());
                    inv.setClean();
                }
            }

            dirtyStorage.clear();

            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                NbtIo.writeCompressed(saveTag, bytes);
                writeAtomically(saveFiles[saveTo], bytes.toByteArray());
                writeAtomically(saveFiles[2], new byte[] { (byte) saveTo });
                saveTo ^= 1;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void reloadManager(boolean client) {
        EnderStorageManager newManager = new EnderStorageManager(client);
        if (client) {
            clientManager = newManager;
        } else {
            serverManager = newManager;
        }
    }

    public File getSaveDir() {
        return saveDir;
    }

    public static EnderStorageManager instance(boolean client) {
        EnderStorageManager manager = client ? clientManager : serverManager;
        if (manager == null) {
            reloadManager(client);
            manager = client ? clientManager : serverManager;
        }
        return manager;
    }

    @SuppressWarnings ("unchecked")
    public <T extends AbstractEnderStorage> T getStorage(Frequency freq, StorageType<T> type) {
        String key = freq + ",type=" + type.name();
        AbstractEnderStorage storage = storageMap.get(key);
        if (storage == null) {
            storage = plugins.get(type).createEnderStorage(this, freq);
            if (!client && saveTag.contains(key)) {
                try (var reporter = new ProblemReporter.ScopedCollector(LOGGER)) {
                    storage.loadFromTag(
                            TagValueInput.create(
                                    reporter,
                                    ServerLifecycleHooks.getCurrentServer().registryAccess(),
                                    saveTag.getCompoundOrEmpty(key)
                            )
                    );
                }
            }
            storageMap.put(key, storage);
            storageList.get(type).add(storage);
        }
        return (T) storage;
    }

    public static void registerPlugin(EnderStoragePlugin<?> plugin) {
        plugins.put(plugin.identifier(), plugin);

        if (serverManager != null) {
            serverManager.storageList.put(plugin.identifier(), new ArrayList<>());
        }
        if (clientManager != null) {
            clientManager.storageList.put(plugin.identifier(), new ArrayList<>());
        }
    }

    public static EnderStoragePlugin<?> getPlugin(StorageType<?> identifier) {
        return plugins.get(identifier);
    }

    public static Map<StorageType<?>, EnderStoragePlugin<?>> getPlugins() {
        return ImmutableMap.copyOf(plugins);
    }

    public List<String> getValidKeys(String identifer) {
        List<String> list = new ArrayList<>();
        for (String key : saveTag.keySet()) {
            if (key.endsWith(",type=" + identifer)) {
                list.add(key.replace(",type=" + identifer, ""));
            }
        }
        return list;
    }

    public void requestSave(AbstractEnderStorage storage) {
        dirtyStorage.add(storage);
    }
}

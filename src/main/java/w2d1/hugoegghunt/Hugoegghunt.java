package w2d1.hugoegghunt;

import com.mojang.authlib.properties.Property;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Hugoegghunt implements ModInitializer {

    public static Hugoegghunt INSTANCE;
    public static final String MOD_ID = "hugoegghunt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Skulls the player already found this save (persisted in the config file). */
    public static final Set<BlockPos> FOUND_SKULLS = ConcurrentHashMap.newKeySet();

    public static final String HALLOWEEN_SKULL_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2Q4YzFmMzk5ZjlhNzRjZTE0ODMwMGNiNmZhNzlmYmMxM2JhNDRmOWE1MmMyNWNlOWU0NTJiNDlmNWI0MGExMCJ9fX0=";

    private static volatile boolean dirty = false;

    @Override
    public void onInitialize() {
        INSTANCE = this;
        LOGGER.info("HugoSMP EggHunt Mod activated");

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (hand == InteractionHand.MAIN_HAND && getTexture(level, hitResult.getBlockPos()).isPresent()) {
                var blockPos = hitResult.getBlockPos();
                if (getTexture(level, blockPos).get().equals(HALLOWEEN_SKULL_TEXTURE)) {
                    BlockPos immutable = blockPos.immutable();
                    if (FOUND_SKULLS.add(immutable)) {
                        markDirty();
                        LOGGER.info("Halloween skull found at: {}", immutable.toShortString());
                    }
                }
            }
            return InteractionResult.PASS;
        });
    }

    public static Optional<String> getTexture(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof SkullBlockEntity skull)) {
            return Optional.empty();
        }
        return Optional.ofNullable(skull.getOwnerProfile())
                .flatMap(profile -> profile.partialProfile().properties().get("textures").stream().findFirst())
                .map(Property::value);
    }

    /** Set when a new skull was found; consumed by the client scanner to force a rescan. */
    public static void markDirty() {
        dirty = true;
    }

    public static boolean consumeDirtyFlag() {
        if (dirty) {
            dirty = false;
            return true;
        }
        return false;
    }
}

package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.listeners;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureBreakEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurniturePlaceEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

import static io.th0rgal.oraxen.utils.BlockHelpers.isLoaded;
import static io.th0rgal.oraxen.utils.blocksounds.BlockSounds.*;

public class FurnitureSoundListener implements Listener {

    private final Map<Location, BukkitTask> breakerPlaySound = new HashMap<>();

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        for (Map.Entry<Location, BukkitTask> entry : breakerPlaySound.entrySet()) {
            if (entry.getKey().isWorldLoaded() || entry.getValue().isCancelled()) continue;
            entry.getValue().cancel();
            breakerPlaySound.remove(entry.getKey());
        }
    }

    // Play sound due to furniture/barrier custom sound replacing stone
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlacingStone(final BlockPlaceEvent event) {
        Block block = event.getBlock();

        if (OraxenBlocks.isOraxenStringBlock(block)) return;
        if (block.getBlockData().getSoundGroup().getPlaceSound() != Sound.BLOCK_STONE_PLACE) return;
        BlockHelpers.playCustomBlockSound(event.getBlock().getLocation(), VANILLA_STONE_PLACE, VANILLA_PLACE_VOLUME, VANILLA_PLACE_PITCH);
    }

    // Play sound due to furniture/barrier custom sound replacing stone
    @EventHandler(priority = EventPriority.HIGH)
    public void onBreakingStone(final BlockBreakEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        StringBlockMechanic mechanicBelow = OraxenBlocks.getStringMechanic(block.getRelative(BlockFace.DOWN));

        if (breakerPlaySound.containsKey(location)) {
            breakerPlaySound.get(location).cancel();
            breakerPlaySound.remove(location);
        }

        if (OraxenBlocks.isOraxenStringBlock(block) || block.getType() == Material.TRIPWIRE && mechanicBelow != null && mechanicBelow.isTall()) return;
        if (block.getBlockData().getSoundGroup().getBreakSound() != Sound.BLOCK_STONE_BREAK) return;
        if (OraxenFurniture.isFurniture(block.getLocation()) && block.getType() == Material.BARRIER || block.isEmpty()) return;

        if (!event.isCancelled() && ProtectionLib.canBreak(event.getPlayer(), location))
            BlockHelpers.playCustomBlockSound(location, VANILLA_STONE_BREAK, VANILLA_BREAK_VOLUME, VANILLA_BREAK_PITCH);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHitStone(final BlockDamageEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        SoundGroup soundGroup = block.getBlockData().getSoundGroup();

        if (event.getInstaBreak()) return;
        if (block.getType() == Material.BARRIER || soundGroup.getHitSound() != Sound.BLOCK_STONE_HIT) return;
        if (breakerPlaySound.containsKey(location)) return;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(OraxenPlugin.get(), () ->
                BlockHelpers.playCustomBlockSound(location, VANILLA_STONE_HIT, VANILLA_HIT_VOLUME, VANILLA_HIT_PITCH), 2L, 4L);
        breakerPlaySound.put(location, task);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStopHittingStone(final BlockDamageAbortEvent event) {
        Location location = event.getBlock().getLocation();
        if (breakerPlaySound.containsKey(location)) {
            breakerPlaySound.get(location).cancel();
            breakerPlaySound.remove(location);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onStepFall(final GenericGameEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) return;
        if (!isLoaded(entity.getLocation())) return;

        GameEvent gameEvent = event.getEvent();
        Block blockStandingOn = BlockHelpers.getBlockStandingOn(entity);
        EntityDamageEvent cause = entity.getLastDamageCause();

        if (blockStandingOn == null || blockStandingOn.getType().isAir()) return;
        SoundGroup soundGroup = blockStandingOn.getBlockData().getSoundGroup();

        if (soundGroup.getStepSound() != Sound.BLOCK_STONE_STEP) return;
        if (gameEvent == GameEvent.HIT_GROUND && cause != null && cause.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (blockStandingOn.getType() == Material.TRIPWIRE) return;
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(blockStandingOn.getLocation());

        String sound;
        float volume;
        float pitch;
        if (gameEvent == GameEvent.STEP) {
            boolean hasStepSound = mechanic != null && mechanic.hasBlockSounds() && mechanic.blockSounds().hasStepSound();
            sound = (hasStepSound) ? mechanic.blockSounds().stepSound() : VANILLA_STONE_STEP;
            volume = (hasStepSound) ? mechanic.blockSounds().stepVolume() : VANILLA_STEP_VOLUME;
            pitch = (hasStepSound) ? mechanic.blockSounds().stepPitch() : VANILLA_STEP_PITCH;
        } else if (gameEvent == GameEvent.HIT_GROUND) {
            boolean hasFallSound = mechanic != null && mechanic.hasBlockSounds() && mechanic.blockSounds().hasFallSound();
            sound = (hasFallSound) ? mechanic.blockSounds().fallSound() : VANILLA_STONE_FALL;
            volume = (hasFallSound) ? mechanic.blockSounds().fallVolume() : VANILLA_FALL_VOLUME;
            pitch = (hasFallSound) ? mechanic.blockSounds().fallPitch() : VANILLA_FALL_PITCH;
        } else return;

        BlockHelpers.playCustomBlockSound(entity.getLocation(), sound, SoundCategory.PLAYERS, volume, pitch);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlacingFurniture(final OraxenFurniturePlaceEvent event) {
        final FurnitureMechanic mechanic = event.getMechanic();
        if (!mechanic.hasBlockSounds()) return;
        BlockSounds blockSounds = mechanic.blockSounds();
        if (blockSounds.hasPlaceSound())
            BlockHelpers.playCustomBlockSound(event.getBaseEntity().getLocation(), blockSounds.placeSound(), blockSounds.placeVolume(), blockSounds.placePitch());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakingFurniture(final OraxenFurnitureBreakEvent event) {
        Location loc = event.getBlock() != null ? event.getBlock().getLocation() : event.getBaseEntity().getLocation();
        final FurnitureMechanic mechanic = event.getMechanic();
        if (!mechanic.hasBlockSounds()) return;
        BlockSounds blockSounds = mechanic.blockSounds();
        if (blockSounds.hasBreakSound())
            BlockHelpers.playCustomBlockSound(loc, blockSounds.breakSound(), blockSounds.breakVolume(), blockSounds.breakPitch());
    }
}
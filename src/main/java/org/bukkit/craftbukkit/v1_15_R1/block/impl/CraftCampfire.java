/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.v1_15_R1.block.impl;

import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import org.bukkit.craftbukkit.v1_15_R1.block.data.CraftBlockData;

public final class CraftCampfire extends CraftBlockData implements org.bukkit.block.data.type.Campfire, org.bukkit.block.data.Directional, org.bukkit.block.data.Lightable, org.bukkit.block.data.Waterlogged {

    public CraftCampfire() {
        super();
    }

    public CraftCampfire(net.minecraft.block.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.type.CraftCampfire

    private static final BooleanProperty SIGNAL_FIRE = getBoolean(net.minecraft.block.CampfireBlock.class, "signal_fire");

    @Override
    public boolean isSignalFire() {
        return get(SIGNAL_FIRE);
    }

    @Override
    public void setSignalFire(boolean signalFire) {
        set(SIGNAL_FIRE, signalFire);
    }

    // org.bukkit.craftbukkit.block.data.CraftDirectional

    private static final EnumProperty<?> FACING = getEnum(net.minecraft.block.CampfireBlock.class, "facing");

    @Override
    public org.bukkit.block.BlockFace getFacing() {
        return get(FACING, org.bukkit.block.BlockFace.class);
    }

    @Override
    public void setFacing(org.bukkit.block.BlockFace facing) {
        set(FACING, facing);
    }

    @Override
    public java.util.Set<org.bukkit.block.BlockFace> getFaces() {
        return getValues(FACING, org.bukkit.block.BlockFace.class);
    }

    // org.bukkit.craftbukkit.block.data.CraftLightable

    private static final BooleanProperty LIT = getBoolean(net.minecraft.block.CampfireBlock.class, "lit");

    @Override
    public boolean isLit() {
        return get(LIT);
    }

    @Override
    public void setLit(boolean lit) {
        set(LIT, lit);
    }

    // org.bukkit.craftbukkit.block.data.CraftWaterlogged

    private static final BooleanProperty WATERLOGGED = getBoolean(net.minecraft.block.CampfireBlock.class, "waterlogged");

    @Override
    public boolean isWaterlogged() {
        return get(WATERLOGGED);
    }

    @Override
    public void setWaterlogged(boolean waterlogged) {
        set(WATERLOGGED, waterlogged);
    }
}
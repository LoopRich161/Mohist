/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.v1_15_R1.block.impl;

import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import org.bukkit.craftbukkit.v1_15_R1.block.data.CraftBlockData;

public final class CraftDropper extends CraftBlockData implements org.bukkit.block.data.type.Dispenser, org.bukkit.block.data.Directional {

    public CraftDropper() {
        super();
    }

    public CraftDropper(net.minecraft.block.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.type.CraftDispenser

    private static final BooleanProperty TRIGGERED = getBoolean(net.minecraft.block.DropperBlock.class, "triggered");

    @Override
    public boolean isTriggered() {
        return get(TRIGGERED);
    }

    @Override
    public void setTriggered(boolean triggered) {
        set(TRIGGERED, triggered);
    }

    // org.bukkit.craftbukkit.block.data.CraftDirectional

    private static final EnumProperty<?> FACING = getEnum(net.minecraft.block.DropperBlock.class, "facing");

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
}
/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.v1_15_R1.block.impl;

import net.minecraft.state.BooleanProperty;
import org.bukkit.craftbukkit.v1_15_R1.block.data.CraftBlockData;

public final class CraftRedstoneTorch extends CraftBlockData implements org.bukkit.block.data.Lightable {

    public CraftRedstoneTorch() {
        super();
    }

    public CraftRedstoneTorch(net.minecraft.block.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.CraftLightable

    private static final BooleanProperty LIT = getBoolean(net.minecraft.block.RedstoneTorchBlock.class, "lit");

    @Override
    public boolean isLit() {
        return get(LIT);
    }

    @Override
    public void setLit(boolean lit) {
        set(LIT, lit);
    }
}

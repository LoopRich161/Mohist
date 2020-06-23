package org.bukkit.craftbukkit.block.data.type;

import net.minecraft.state.EnumProperty;
import org.bukkit.block.data.type.Bamboo;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public abstract class CraftBamboo extends CraftBlockData implements Bamboo {

    private static final EnumProperty<?> LEAVES = getEnum("leaves");

    @Override
    public Leaves getLeaves() {
        return get(LEAVES, Leaves.class);
    }

    @Override
    public void setLeaves(Leaves leaves) {
        set(LEAVES, leaves);
    }
}

package org.bukkit.craftbukkit.v1_15_R1.block.data;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.command.arguments.BlockStateParser;
import net.minecraft.block.Block;
import net.minecraft.state.StateHolder;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.Direction;
import net.minecraft.block.BlockState;
import net.minecraft.state.IProperty;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.registry.Registry;
import net.minecraft.nbt.CompoundNBT;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_15_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_15_R1.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v1_15_R1.block.impl.*;
import org.bukkit.craftbukkit.v1_15_R1.block.impl.CraftRotatable;

public class CraftBlockData implements BlockData {

    private BlockState state;
    private Map<IProperty<?>, Comparable<?>> parsedStates;

    protected CraftBlockData() {
        throw new AssertionError("Template Constructor");
    }

    protected CraftBlockData(BlockState state) {
        this.state = state;
    }

    @Override
    public Material getMaterial() {
        return CraftMagicNumbers.getMaterial(state.getBlock());
    }

    public BlockState getState() {
        return state;
    }

    /**
     * Get a given EnumProperty's value as its Bukkit counterpart.
     *
     * @param nms the NMS state to convert
     * @param bukkit the Bukkit class
     * @param <B> the type
     * @return the matching Bukkit type
     */
    protected <B extends Enum<B>> B get(EnumProperty<?> nms, Class<B> bukkit) {
        return toBukkit(state.get(nms), bukkit);
    }

    /**
     * Convert all values from the given EnumProperty to their appropriate
     * Bukkit counterpart.
     *
     * @param nms the NMS state to get values from
     * @param bukkit the bukkit class to convert the values to
     * @param <B> the bukkit class type
     * @return an immutable Set of values in their appropriate Bukkit type
     */
    @SuppressWarnings("unchecked")
    protected <B extends Enum<B>> Set<B> getValues(EnumProperty<?> nms, Class<B> bukkit) {
        ImmutableSet.Builder<B> values = ImmutableSet.builder();

        for (Enum<?> e : nms.getAllowedValues()) {
            values.add(toBukkit(e, bukkit));
        }

        return values.build();
    }

    /**
     * Set a given {@link EnumProperty} with the matching enum from Bukkit.
     *
     * @param nms the NMS EnumProperty to set
     * @param bukkit the matching Bukkit Enum
     * @param <B> the Bukkit type
     * @param <N> the NMS type
     */
    protected <B extends Enum<B>, N extends Enum<N> & IStringSerializable> void set(EnumProperty<N> nms, Enum<B> bukkit) {
        this.parsedStates = null;
        this.state = this.state.with(nms, toNMS(bukkit, nms.getValueClass()));
    }

    @Override
    public BlockData merge(BlockData data) {
        CraftBlockData craft = (CraftBlockData) data;
        Preconditions.checkArgument(craft.parsedStates != null, "Data not created via string parsing");
        Preconditions.checkArgument(this.state.getBlock() == craft.state.getBlock(), "States have different types (got %s, expected %s)", data, this);

        CraftBlockData clone = (CraftBlockData) this.clone();
        clone.parsedStates = null;

        for (IProperty parsed : craft.parsedStates.keySet()) {
            clone.state = clone.state.with(parsed, craft.state.get(parsed));
        }

        return clone;
    }

    @Override
    public boolean matches(BlockData data) {
        if (data == null) {
            return false;
        }
        if (!(data instanceof CraftBlockData)) {
            return false;
        }

        CraftBlockData craft = (CraftBlockData) data;
        if (this.state.getBlock() != craft.state.getBlock()) {
            return false;
        }

        // Fastpath an exact match
        boolean exactMatch = this.equals(data);

        // If that failed, do a merge and check
        if (!exactMatch && craft.parsedStates != null) {
            return this.merge(data).equals(this);
        }

        return exactMatch;
    }

    private static final Map<Class, BiMap<Enum<?>, Enum<?>>> classMappings = new HashMap<>();

    /**
     * Convert an NMS Enum (usually a EnumProperty) to its appropriate Bukkit
     * enum from the given class.
     *
     * @throws IllegalStateException if the Enum could not be converted
     */
    @SuppressWarnings("unchecked")
    private static <B extends Enum<B>> B toBukkit(Enum<?> nms, Class<B> bukkit) {
        Enum<?> converted;
        BiMap<Enum<?>, Enum<?>> nmsToBukkit = classMappings.get(nms.getClass());

        if (nmsToBukkit != null) {
            converted = nmsToBukkit.get(nms);
            if (converted != null) {
                return (B) converted;
            }
        }

        if (nms instanceof Direction) {
            converted = CraftBlock.notchToBlockFace((Direction) nms);
        } else {
            converted = bukkit.getEnumConstants()[nms.ordinal()];
        }

        Preconditions.checkState(converted != null, "Could not convert enum %s->%s", nms, bukkit);

        if (nmsToBukkit == null) {
            nmsToBukkit = HashBiMap.create();
            classMappings.put(nms.getClass(), nmsToBukkit);
        }

        nmsToBukkit.put(nms, converted);

        return (B) converted;
    }

    /**
     * Convert a given Bukkit enum to its matching NMS enum type.
     *
     * @param bukkit the Bukkit enum to convert
     * @param nms the NMS class
     * @return the matching NMS type
     * @throws IllegalStateException if the Enum could not be converted
     */
    @SuppressWarnings("unchecked")
    private static <N extends Enum<N> & IStringSerializable> N toNMS(Enum<?> bukkit, Class<N> nms) {
        Enum<?> converted;
        BiMap<Enum<?>, Enum<?>> nmsToBukkit = classMappings.get(nms);

        if (nmsToBukkit != null) {
            converted = nmsToBukkit.inverse().get(bukkit);
            if (converted != null) {
                return (N) converted;
            }
        }

        if (bukkit instanceof BlockFace) {
            converted = CraftBlock.blockFaceToNotch((BlockFace) bukkit);
        } else {
            converted = nms.getEnumConstants()[bukkit.ordinal()];
        }

        Preconditions.checkState(converted != null, "Could not convert enum %s->%s", nms, bukkit);

        if (nmsToBukkit == null) {
            nmsToBukkit = HashBiMap.create();
            classMappings.put(nms, nmsToBukkit);
        }

        nmsToBukkit.put(converted, bukkit);

        return (N) converted;
    }

    /**
     * Get the current value of a given state.
     *
     * @param ibs the state to check
     * @param <T> the type
     * @return the current value of the given state
     */
    protected <T extends Comparable<T>> T get(IProperty<T> ibs) {
        // Straight integer or boolean getter
        return this.state.get(ibs);
    }

    /**
     * Set the specified state's value.
     *
     * @param ibs the state to set
     * @param v the new value
     * @param <T> the state's type
     * @param <V> the value's type. Must match the state's type.
     */
    public <T extends Comparable<T>, V extends T> void set(IProperty<T> ibs, V v) {
        // Straight integer or boolean setter
        this.parsedStates = null;
        this.state = this.state.with(ibs, v);
    }

    @Override
    public String getAsString() {
        return toString(((StateHolder) state).getValues());
    }

    @Override
    public String getAsString(boolean hideUnspecified) {
        return (hideUnspecified && parsedStates != null) ? toString(parsedStates) : getAsString();
    }

    @Override
    public BlockData clone() {
        try {
            return (BlockData) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError("Clone not supported", ex);
        }
    }

    @Override
    public String toString() {
        return "CraftBlockData{" + getAsString() + "}";
    }

    // Mimicked from StateHolder#toString()
    public String toString(Map<IProperty<?>, Comparable<?>> states) {
        StringBuilder stateString = new StringBuilder(Registry.BLOCK.getKey(state.getBlock()).toString());

        if (!states.isEmpty()) {
            stateString.append('[');
            stateString.append(states.entrySet().stream().map(StateHolder.MAP_ENTRY_TO_STRING).collect(Collectors.joining(",")));
            stateString.append(']');
        }

        return stateString.toString();
    }

    public CompoundNBT toStates() {
        CompoundNBT compound = new CompoundNBT();

        for (Map.Entry<IProperty<?>, Comparable<?>> entry : state.getValues().entrySet()) {
            IProperty iblockstate = (IProperty) entry.getKey();

            compound.putString(iblockstate.getName(), iblockstate.getName(entry.getValue()));
        }

        return compound;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CraftBlockData && state.equals(((CraftBlockData) obj).state);
    }

    @Override
    public int hashCode() {
        return state.hashCode();
    }

    protected static BooleanProperty getBoolean(String name) {
        throw new AssertionError("Template Method");
    }

    protected static BooleanProperty getBoolean(String name, boolean optional) {
        throw new AssertionError("Template Method");
    }

    protected static EnumProperty<?> getEnum(String name) {
        throw new AssertionError("Template Method");
    }

    protected static IntegerProperty getInteger(String name) {
        throw new AssertionError("Template Method");
    }

    protected static BooleanProperty getBoolean(Class<? extends Block> block, String name) {
        return (BooleanProperty) getState(block, name, false);
    }

    protected static BooleanProperty getBoolean(Class<? extends Block> block, String name, boolean optional) {
        return (BooleanProperty) getState(block, name, optional);
    }

    protected static EnumProperty<?> getEnum(Class<? extends Block> block, String name) {
        return (EnumProperty<?>) getState(block, name, false);
    }

    protected static IntegerProperty getInteger(Class<? extends Block> block, String name) {
        return (IntegerProperty) getState(block, name, false);
    }

    /**
     * Get a specified {@link IProperty} from a given block's class with a
     * given name
     *
     * @param block the class to retrieve the state from
     * @param name the name of the state to retrieve
     * @param optional if the state can be null
     * @return the specified state or null
     * @throws IllegalStateException if the state is null and {@code optional}
     * is false.
     */
    private static IProperty<?> getState(Class<? extends Block> block, String name, boolean optional) {
        IProperty<?> state = null;

        for (Block instance : Registry.BLOCK) {
            if (instance.getClass() == block) {
                if (state == null) {
                    state = instance.getStateContainer().getProperty(name);
                } else {
                    IProperty<?> newState = instance.getStateContainer().getProperty(name);

                    Preconditions.checkState(state == newState, "State mistmatch %s,%s", state, newState);
                }
            }
        }

        Preconditions.checkState(optional || state != null, "Null state for %s,%s", block, name);

        return state;
    }

    /**
     * Get the minimum value allowed by the IntegerProperty.
     *
     * @param state the state to check
     * @return the minimum value allowed
     */
    protected static int getMin(IntegerProperty state) {
        return state.min;
    }

    /**
     * Get the maximum value allowed by the IntegerProperty.
     *
     * @param state the state to check
     * @return the maximum value allowed
     */
    protected static int getMax(IntegerProperty state) {
        return state.max;
    }

    //
    private static final Map<Class<? extends Block>, Function<BlockState, CraftBlockData>> MAP = new HashMap<>();

    static {
        //<editor-fold desc="CraftBlockData Registration" defaultstate="collapsed">
        register(net.minecraft.block.AnvilBlock.class, CraftAnvil::new);
        register(net.minecraft.block.BambooBlock.class, CraftBamboo::new);
        register(net.minecraft.block.BannerBlock.class, CraftBanner::new);
        register(net.minecraft.block.WallBannerBlock.class, CraftBannerWall::new);
        register(net.minecraft.block.BarrelBlock.class, CraftBarrel::new);
        register(net.minecraft.block.BedBlock.class, CraftBed::new);
        register(net.minecraft.block.BeehiveBlock.class, CraftBeehive::new);
        register(net.minecraft.block.BeetrootBlock.class, CraftBeetroot::new);
        register(net.minecraft.block.BellBlock.class, CraftBell::new);
        register(net.minecraft.block.BlastFurnaceBlock.class, CraftBlastFurnace::new);
        register(net.minecraft.block.BrewingStandBlock.class, CraftBrewingStand::new);
        register(net.minecraft.block.BubbleColumnBlock.class, CraftBubbleColumn::new);
        register(net.minecraft.block.CactusBlock.class, CraftCactus::new);
        register(net.minecraft.block.CakeBlock.class, CraftCake::new);
        register(net.minecraft.block.CampfireBlock.class, CraftCampfire::new);
        register(net.minecraft.block.CarrotBlock.class, CraftCarrots::new);
        register(net.minecraft.block.CauldronBlock.class, CraftCauldron::new);
        register(net.minecraft.block.ChestBlock.class, CraftChest::new);
        register(net.minecraft.block.TrappedChestBlock.class, CraftChestTrapped::new);
        register(net.minecraft.block.ChorusFlowerBlock.class, CraftChorusFlower::new);
        register(net.minecraft.block.ChorusPlantBlock.class, CraftChorusFruit::new);
        register(net.minecraft.block.WallBlock.class, CraftCobbleWall::new);
        register(net.minecraft.block.CocoaBlock.class, CraftCocoa::new);
        register(net.minecraft.block.CommandBlockBlock.class, CraftCommand::new);
        register(net.minecraft.block.ComposterBlock.class, CraftComposter::new);
        register(net.minecraft.block.ConduitBlock.class, CraftConduit::new);
        register(net.minecraft.block.DeadCoralPlantBlock.class, CraftCoralDead::new);
        register(net.minecraft.block.CoralFinBlock.class, CraftCoralFan::new);
        register(net.minecraft.block.CoralFanBlock.class, CraftCoralFanAbstract::new);
        register(net.minecraft.block.CoralWallFanBlock.class, CraftCoralFanWall::new);
        register(net.minecraft.block.DeadCoralWallFanBlock.class, CraftCoralFanWallAbstract::new);
        register(net.minecraft.block.CoralPlantBlock.class, CraftCoralPlant::new);
        register(net.minecraft.block.CropsBlock.class, CraftCrops::new);
        register(net.minecraft.block.DaylightDetectorBlock.class, CraftDaylightDetector::new);
        register(net.minecraft.block.SnowyDirtBlock.class, CraftDirtSnow::new);
        register(net.minecraft.block.DispenserBlock.class, CraftDispenser::new);
        register(net.minecraft.block.DoorBlock.class, CraftDoor::new);
        register(net.minecraft.block.DropperBlock.class, CraftDropper::new);
        register(net.minecraft.block.EndRodBlock.class, CraftEndRod::new);
        register(net.minecraft.block.EnderChestBlock.class, CraftEnderChest::new);
        register(net.minecraft.block.EndPortalFrameBlock.class, CraftEnderPortalFrame::new);
        register(net.minecraft.block.FenceBlock.class, CraftFence::new);
        register(net.minecraft.block.FenceGateBlock.class, CraftFenceGate::new);
        register(net.minecraft.block.FireBlock.class, CraftFire::new);
        register(net.minecraft.block.StandingSignBlock.class, CraftFloorSign::new);
        register(net.minecraft.block.FlowingFluidBlock.class, CraftFluids::new);
        register(net.minecraft.block.FurnaceBlock.class, CraftFurnaceFurace::new);
        register(net.minecraft.block.GlazedTerracottaBlock.class, CraftGlazedTerracotta::new);
        register(net.minecraft.block.GrassBlock.class, CraftGrass::new);
        register(net.minecraft.block.GrindstoneBlock.class, CraftGrindstone::new);
        register(net.minecraft.block.HayBlock.class, CraftHay::new);
        register(net.minecraft.block.HopperBlock.class, CraftHopper::new);
        register(net.minecraft.block.HugeMushroomBlock.class, CraftHugeMushroom::new);
        register(net.minecraft.block.FrostedIceBlock.class, CraftIceFrost::new);
        register(net.minecraft.block.PaneBlock.class, CraftIronBars::new);
        register(net.minecraft.block.JigsawBlock.class, CraftJigsaw::new);
        register(net.minecraft.block.JukeboxBlock.class, CraftJukeBox::new);
        register(net.minecraft.block.KelpTopBlock.class, CraftKelp::new);
        register(net.minecraft.block.LadderBlock.class, CraftLadder::new);
        register(net.minecraft.block.LanternBlock.class, CraftLantern::new);
        register(net.minecraft.block.LeavesBlock.class, CraftLeaves::new);
        register(net.minecraft.block.LecternBlock.class, CraftLectern::new);
        register(net.minecraft.block.LeverBlock.class, CraftLever::new);
        register(net.minecraft.block.LogBlock.class, CraftLogAbstract::new);
        register(net.minecraft.block.LoomBlock.class, CraftLoom::new);
        register(net.minecraft.block.DetectorRailBlock.class, CraftMinecartDetector::new);
        register(net.minecraft.block.RailBlock.class, CraftMinecartTrack::new);
        register(net.minecraft.block.MyceliumBlock.class, CraftMycel::new);
        register(net.minecraft.block.NetherWartBlock.class, CraftNetherWart::new);
        register(net.minecraft.block.NoteBlock.class, CraftNote::new);
        register(net.minecraft.block.ObserverBlock.class, CraftObserver::new);
        register(net.minecraft.block.PistonBlock.class, CraftPiston::new);
        register(net.minecraft.block.PistonHeadBlock.class, CraftPistonExtension::new);
        register(net.minecraft.block.MovingPistonBlock.class, CraftPistonMoving::new);
        register(net.minecraft.block.NetherPortalBlock.class, CraftPortal::new);
        register(net.minecraft.block.PotatoBlock.class, CraftPotatoes::new);
        register(net.minecraft.block.PoweredRailBlock.class, CraftPoweredRail::new);
        register(net.minecraft.block.PressurePlateBlock.class, CraftPressurePlateBinary::new);
        register(net.minecraft.block.WeightedPressurePlateBlock.class, CraftPressurePlateWeighted::new);
        register(net.minecraft.block.CarvedPumpkinBlock.class, CraftPumpkinCarved::new);
        register(net.minecraft.block.ComparatorBlock.class, CraftRedstoneComparator::new);
        register(net.minecraft.block.RedstoneLampBlock.class, CraftRedstoneLamp::new);
        register(net.minecraft.block.RedstoneOreBlock.class, CraftRedstoneOre::new);
        register(net.minecraft.block.RedstoneTorchBlock.class, CraftRedstoneTorch::new);
        register(net.minecraft.block.RedstoneWallTorchBlock.class, CraftRedstoneTorchWall::new);
        register(net.minecraft.block.RedstoneWireBlock.class, CraftRedstoneWire::new);
        register(net.minecraft.block.SugarCaneBlock.class, CraftReed::new);
        register(net.minecraft.block.RepeaterBlock.class, CraftRepeater::new);
        register(net.minecraft.block.RotatedPillarBlock.class, CraftRotatable::new);
        register(net.minecraft.block.SaplingBlock.class, CraftSapling::new);
        register(net.minecraft.block.ScaffoldingBlock.class, CraftScaffolding::new);
        register(net.minecraft.block.SeaPickleBlock.class, CraftSeaPickle::new);
        register(net.minecraft.block.ShulkerBoxBlock.class, CraftShulkerBox::new);
        register(net.minecraft.block.SkullBlock.class, CraftSkull::new);
        register(net.minecraft.block.SkullPlayerBlock.class, CraftSkullPlayer::new);
        register(net.minecraft.block.SkullWallPlayerBlock.class, CraftSkullPlayerWall::new);
        register(net.minecraft.block.WallSkullBlock.class, CraftSkullWall::new);
        register(net.minecraft.block.SmokerBlock.class, CraftSmoker::new);
        register(net.minecraft.block.SnowBlock.class, CraftSnow::new);
        register(net.minecraft.block.FarmlandBlock.class, CraftSoil::new);
        register(net.minecraft.block.StainedGlassPaneBlock.class, CraftStainedGlassPane::new);
        register(net.minecraft.block.StairsBlock.class, CraftStairs::new);
        register(net.minecraft.block.StemBlock.class, CraftStem::new);
        register(net.minecraft.block.AttachedStemBlock.class, CraftStemAttached::new);
        register(net.minecraft.block.SlabBlock.class, CraftStepAbstract::new);
        register(net.minecraft.block.StoneButtonBlock.class, CraftStoneButton::new);
        register(net.minecraft.block.StonecutterBlock.class, CraftStonecutter::new);
        register(net.minecraft.block.StructureBlock.class, CraftStructure::new);
        register(net.minecraft.block.SweetBerryBushBlock.class, CraftSweetBerryBush::new);
        register(net.minecraft.block.TNTBlock.class, CraftTNT::new);
        register(net.minecraft.block.DoublePlantBlock.class, CraftTallPlant::new);
        register(net.minecraft.block.TallFlowerBlock.class, CraftTallPlantFlower::new);
        register(net.minecraft.block.TallSeaGrassBlock.class, CraftTallSeaGrass::new);
        register(net.minecraft.block.WallTorchBlock.class, CraftTorchWall::new);
        register(net.minecraft.block.TrapDoorBlock.class, CraftTrapdoor::new);
        register(net.minecraft.block.TripWireBlock.class, CraftTripwire::new);
        register(net.minecraft.block.TripWireHookBlock.class, CraftTripwireHook::new);
        register(net.minecraft.block.TurtleEggBlock.class, CraftTurtleEgg::new);
        register(net.minecraft.block.VineBlock.class, CraftVine::new);
        register(net.minecraft.block.WallSignBlock.class, CraftWallSign::new);
        register(net.minecraft.block.WitherSkeletonSkullBlock.class, CraftWitherSkull::new);
        register(net.minecraft.block.WitherSkeletonWallSkullBlock.class, CraftWitherSkullWall::new);
        register(net.minecraft.block.WoodButtonBlock.class, CraftWoodButton::new);
        //</editor-fold>
    }

    private static void register(Class<? extends Block> nms, Function<BlockState, CraftBlockData> bukkit) {
        Preconditions.checkState(MAP.put(nms, bukkit) == null, "Duplicate mapping %s->%s", nms, bukkit);
    }

    public static CraftBlockData newData(Material material, String data) {
        Preconditions.checkArgument(material == null || material.isBlock(), "Cannot get data for not block %s", material);

        BlockState blockData;
        Block block = CraftMagicNumbers.getBlock(material);
        Map<IProperty<?>, Comparable<?>> parsed = null;

        // Data provided, use it
        if (data != null) {
            try {
                // Material provided, force that material in
                if (block != null) {
                    data = Registry.BLOCK.getKey(block) + data;
                }

                StringReader reader = new StringReader(data);
                BlockStateParser arg = new BlockStateParser(reader, false).parse(false);
                Preconditions.checkArgument(!reader.canRead(), "Spurious trailing data: " + data);

                blockData = arg.getState();
                parsed = arg.getProperties();
            } catch (CommandSyntaxException ex) {
                throw new IllegalArgumentException("Could not parse data: " + data, ex);
            }
        } else {
            blockData = block.getDefaultState();
        }

        CraftBlockData craft = fromData(blockData);
        craft.parsedStates = parsed;
        return craft;
    }

    public static CraftBlockData fromData(BlockState data) {
        return MAP.getOrDefault(data.getBlock().getClass(), CraftBlockData::new).apply(data);
    }
}

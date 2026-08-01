package yc.ycqin.doth.util;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import java.util.*;

public class BlockHighlightConfig {
    public static final Set<Block> highlightBlocks = new HashSet<>();
    public static boolean enabled = false;
    public static int searchRange = 32;
    public static int updateFrequency = 10;
    public static boolean mineAllEnabled = false;
    public static int mineAllRadius = 4; // ticks

    public static void addBlock(Block block) { highlightBlocks.add(block); }
    public static void removeBlock(Block block) { highlightBlocks.remove(block); }
    public static void toggle() { enabled = !enabled; }
    public static boolean isHighlighted(IBlockState state) { return highlightBlocks.contains(state.getBlock()); }
}

package yc.ycqin.doth.common.item;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/**
 * 虫灵快跑入场券配方原料：2 个鞍 + 1 张拍摄有虫灵（srparasites:buglin）的生物照片。
 * 照片 NBT 的 EntityIDs 列表包含 srparasites:buglin 即可（不要求仅有虫灵）。
 * 用自定义 NBT Ingredient，JEI 也能正常显示。
 */
public class RecipeBuglinTicket {

    public static final String BUGLIN_ID = "srparasites:buglin";

    /** 照片 NBT 的 EntityIDs 列表里是否有虫灵 */
    public static boolean photoHasBuglin(ItemStack photo) {
        NBTTagCompound tag = photo.getTagCompound();
        if (tag == null || !tag.hasKey("EntityIDs")) return false;
        NBTTagList list = tag.getTagList("EntityIDs", 8);
        for (int i = 0; i < list.tagCount(); i++) {
            if (BUGLIN_ID.equals(list.getStringTagAt(i))) return true;
        }
        return false;
    }

    /**
     * 带 NBT 的 Ingredient：JEI 显示带虫灵的生物照片，合成匹配也走这里
     */
    public static class BuglinPhotoIngredient extends Ingredient {

        private final ItemStack displayStack;

        public BuglinPhotoIngredient() {
            super(buildDisplayStack());
            this.displayStack = buildDisplayStack();
        }

        private static ItemStack buildDisplayStack() {
            ItemStack stack = new ItemStack(ItemReg.BIO_PHOTO);
            NBTTagCompound tag = new NBTTagCompound();
            NBTTagList ids = new NBTTagList();
            ids.appendTag(new NBTTagString(BUGLIN_ID));
            tag.setTag("EntityIDs", ids);
            stack.setTagCompound(tag);
            return stack;
        }

        @Override
        public boolean apply(ItemStack input) {
            if (input == null || input.isEmpty()) return false;
            if (!(input.getItem() instanceof BioPhotoItem)) return false;
            return photoHasBuglin(input);
        }

        /**
         * 关键：有 NBT 检查的 Ingredient 必须返回 false，
         * 否则 ShapelessRecipes 走 RecipeItemHelper 快速路径（按 item 匹配，忽略 NBT），NBT 检测会失效
         */
        @Override
        public boolean isSimple() {
            return false;
        }

        @Override
        public ItemStack[] getMatchingStacks() {
            return new ItemStack[]{displayStack};
        }
    }
}

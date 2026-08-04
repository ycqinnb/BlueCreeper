package yc.ycqin.doth.common.item;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * 生物快跑入场券配方：2 个鞍 + 1 张拍摄有任意生物的生物照片。
 * 输出入场券并写入照片 EntityIDs 列表里第一个生物的注册名（doth_rush_mob）。
 * 照片无生物 → 不匹配。
 */
public class RecipeRushTicket extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    /** 照片 NBT 的实体注册名列表 key */
    public static final String PHOTO_IDS_KEY = "EntityIDs";

    /** 取照片 EntityIDs 里第一个生物注册名，无则返回 null */
    public static String firstMobId(ItemStack photo) {
        if (photo.isEmpty() || !(photo.getItem() instanceof BioPhotoItem)) return null;
        NBTTagCompound tag = photo.getTagCompound();
        if (tag == null || !tag.hasKey(PHOTO_IDS_KEY)) return null;
        NBTTagList list = tag.getTagList(PHOTO_IDS_KEY, 8);
        for (int i = 0; i < list.tagCount(); i++) {
            String id = list.getStringTagAt(i);
            if (id != null && !id.isEmpty()) return id;
        }
        return null;
    }

    /** 带生物的入场券 */
    public static ItemStack createTicket(String mobId) {
        ItemStack out = new ItemStack(ItemReg.RUSH_TICKET);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(ItemRushTicket.NBT_MOB, mobId);
        out.setTagCompound(tag);
        return out;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        int saddles = 0;
        ItemStack photo = ItemStack.EMPTY;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s.isEmpty()) continue;
            if (s.getItem() == net.minecraft.init.Items.SADDLE) {
                saddles++;
            } else if (s.getItem() instanceof BioPhotoItem && photo.isEmpty()) {
                photo = s;
            } else {
                return false; // 其他物品
            }
        }
        return saddles == 2 && firstMobId(photo) != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack photo = ItemStack.EMPTY;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (!s.isEmpty() && s.getItem() instanceof BioPhotoItem) {
                photo = s;
                break;
            }
        }
        String mobId = firstMobId(photo);
        return mobId != null ? createTicket(mobId) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return createTicket("minecraft:zombie");
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        return NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
    }

    /** JEI 显示用：2 鞍 + 1 张任意生物照片 */
    @Override
    public NonNullList<net.minecraft.item.crafting.Ingredient> getIngredients() {
        NonNullList<net.minecraft.item.crafting.Ingredient> list = NonNullList.create();
        list.add(net.minecraft.item.crafting.Ingredient.fromItem(net.minecraft.init.Items.SADDLE));
        list.add(net.minecraft.item.crafting.Ingredient.fromItem(net.minecraft.init.Items.SADDLE));
        list.add(net.minecraft.item.crafting.Ingredient.fromItem(ItemReg.BIO_PHOTO));
        return list;
    }
}

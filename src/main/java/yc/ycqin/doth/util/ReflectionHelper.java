package yc.ycqin.doth.util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.NonNullList;


import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class ReflectionHelper {
    private static Field lightmapColorsField;
    private static Field allInventories;
    private static Field activePotionsMapField;

    //private static Unsafe UNSAFE;

    static {
        try {
            lightmapColorsField = EntityRenderer.class.getDeclaredField("field_78504_Q");
            lightmapColorsField.setAccessible(true);
            allInventories = InventoryPlayer.class.getDeclaredField("field_184440_g");
            allInventories.setAccessible(true);
            activePotionsMapField = EntityLivingBase.class.getDeclaredField("field_70713_bf");
            activePotionsMapField.setAccessible(true);

            //Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            //unsafeField.setAccessible(true);
            //UNSAFE = (Unsafe) unsafeField.get(null);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static int[] getLightmapColors() {
        if (lightmapColorsField == null) return null;
        try {
            EntityRenderer renderer = Minecraft.getMinecraft().entityRenderer;
            return (int[]) lightmapColorsField.get(renderer);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<NonNullList<ItemStack>> getAllInventories(InventoryPlayer inventoryPlayer){
        if (allInventories == null)return null;
        try{
            return (List<NonNullList<ItemStack>>) allInventories.get(inventoryPlayer);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Map<Potion, PotionEffect> getActivePotionsMap(EntityLivingBase entityLivingBase){
        if (activePotionsMapField == null)return null;
        try{
            return (Map<Potion, PotionEffect>) activePotionsMapField.get(entityLivingBase);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void nbSetHealth(EntityLivingBase target,float newHealth) {
        try {
            Field field = EntityLivingBase.class.getDeclaredField("field_184632_c");
            if (field == null) {
                field = EntityLivingBase.class.getDeclaredField("HEALTH");
            }
            field.setAccessible(true);
            DataParameter<Float> key = (DataParameter<Float>) field.get(null);
            target.getDataManager().set(key,newHealth);
        } catch (Exception e) {
            target.setHealth(newHealth);
        }
    }
}

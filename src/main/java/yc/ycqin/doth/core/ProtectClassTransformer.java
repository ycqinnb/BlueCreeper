package yc.ycqin.doth.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

public class ProtectClassTransformer implements IClassTransformer {

    private static final Map<String, Set<String>> SUPER_METHODS_CACHE = new HashMap<>();

    public ProtectClassTransformer() {}

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        // 虫灵快跑：禁止 SRP 虫灵进化（growStage 维度判断）
        if (transformedName.equals("com.dhanantry.scapeandrunparasites.entity.monster.inborn.EntityLodo")) {
            return transformLodoGrow(basicClass);
        }
        boolean shouldApplyAllReturn = shouldApplyAllReturn(transformedName);
        if (shouldApplyAllReturn) {
            DOTHConfig.reload();
            if (DOTHConfig.enableAllReturn) {
                try { return applyAllReturn(transformedName, basicClass); }
                catch (Exception e) { return basicClass; }
            }
        }
        if (transformedName.equals("net.minecraft.client.gui.GuiScreen") && DOTHConfig.replaceEventBus)
            return transformGuiScreen(basicClass);
        DOTHConfig.reload();
        if (DOTHConfig.replaceEventBus && transformedName.equals("net.minecraftforge.fml.common.eventhandler.EventBus")) {
            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(ASM5, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String mname, String desc, String sig, String[] exc) {
                    if (mname.equals("post") && desc.equals("(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z")) {
                        MethodVisitor mv = super.visitMethod(access, mname, desc, sig, exc);
                        mv.visitCode();
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitVarInsn(ALOAD, 1);
                        mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "safePost",
                            "(Lnet/minecraftforge/fml/common/eventhandler/EventBus;Lnet/minecraftforge/fml/common/eventhandler/Event;)Z", false);
                        mv.visitInsn(IRETURN);
                        mv.visitMaxs(2, 2);
                        mv.visitEnd();
                        return null;
                    }
                    return super.visitMethod(access, mname, desc, sig, exc);
                }
            };
            cr.accept(cv, 0);
            return cw.toByteArray();
        }

        if (transformedName.equals("net.minecraft.client.Minecraft")) {
            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
                @Override protected String getCommonSuperClass(String t1, String t2) {
                    try { return super.getCommonSuperClass(t1, t2); }
                    catch (Exception e) { return "java/lang/Object"; }
                }
            };
            ClassVisitor cv = new ClassVisitor(ASM5, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String mname, String desc, String sig, String[] exc) {
                    if ((mname.equals("func_71411_J") || mname.equals("runGameLoop")) && desc.equals("()V")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "onClientTick", "()V", false);
                                super.visitCode();
                            }
                        };
                    }
                    if ((mname.equals("func_71403_a") || mname.equals("func_71353_a") || mname.equals("loadWorld"))) {
                        DOTHConfig.reload();
                        if (DOTHConfig.strongCompat) {
                            return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                                @Override public void visitCode() {
                                    mv.visitMethodInsn(INVOKESTATIC, "net/minecraftforge/fml/common/FMLCommonHandler", "instance", "()Lnet/minecraftforge/fml/common/FMLCommonHandler;", false);
                                    mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraftforge/fml/common/FMLCommonHandler", "getEffectiveSide", "()Lnet/minecraftforge/fml/relauncher/Side;", false);
                                    mv.visitFieldInsn(GETSTATIC, "net/minecraftforge/fml/relauncher/Side", "SERVER", "Lnet/minecraftforge/fml/relauncher/Side;");
                                    Label cl = new Label();
                                    mv.visitJumpInsn(IF_ACMPNE, cl);
                                    mv.visitInsn(RETURN);
                                    mv.visitLabel(cl);
                                    super.visitCode();
                                }
                            };
                        }
                    }
                    if ((mname.equals("func_147108_a") || mname.equals("displayGuiScreen")) && desc.equals("(Lnet/minecraft/client/gui/GuiScreen;)V") && DOTHConfig.replaceEventBus) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitFieldInsn(GETSTATIC, "yc/ycqin/doth/core/DOTHConfig", "replaceEventBus", "Z");
                                LabelNode skipAll = new LabelNode();
                                mv.visitJumpInsn(IFEQ, skipAll.getLabel());
                                mv.visitVarInsn(ALOAD, 1);
                                LabelNode sn = new LabelNode();
                                mv.visitJumpInsn(IFNULL, sn.getLabel());
                                mv.visitVarInsn(ALOAD, 1);
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "onDisplayGui", "(Lnet/minecraft/client/gui/GuiScreen;)Z", false);
                                LabelNode l = new LabelNode();
                                mv.visitJumpInsn(IFEQ, l.getLabel());
                                mv.visitInsn(RETURN);
                                mv.visitLabel(sn.getLabel());
                                mv.visitLabel(l.getLabel());
                                mv.visitLabel(skipAll.getLabel());
                                super.visitCode();
                            }
                        };
                    }

                    if (mname.equals("func_99999_d") && desc.equals("()V")) {
                        DOTHConfig.reload();
                        if (DOTHConfig.strongCompat) {
                            MethodVisitor mv = super.visitMethod(access, mname, desc, sig, exc);
                            mv.visitCode();
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "clientRun", "(Lnet/minecraft/client/Minecraft;)V", false);
                            mv.visitInsn(RETURN);
                            mv.visitMaxs(1, 1); mv.visitEnd();
                            return null;
                        }
                    }
                    return super.visitMethod(access, mname, desc, sig, exc);
                }
            };
            cr.accept(cv, 0); return cw.toByteArray();
        }

        if (transformedName.equals("net.minecraftforge.common.ForgeHooks")) {
            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(ASM5, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String mname, String desc, String sig, String[] exc) {
                    if (mname.equals("onLivingUpdate") && desc.equals("(Lnet/minecraft/entity/EntityLivingBase;)Z")) {
                        MethodVisitor mv = cv.visitMethod(access, mname, desc, sig, exc);
                        mv.visitCode();
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper",
                                "hookOnUpdate0", "(Lnet/minecraft/entity/EntityLivingBase;)Z", false);
                        mv.visitInsn(IRETURN);
                        mv.visitMaxs(1, 1);
                        mv.visitEnd();
                        return null;
                    }
                    return cv.visitMethod(access, mname, desc, sig, exc);
                }
            };
            cr.accept(cv, 0); return cw.toByteArray();
        }

        if (transformedName.equals("net.minecraft.client.LoadingScreenRenderer")) {
            DOTHConfig.reload();
            if (!DOTHConfig.strongCompat) return basicClass;
            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(ASM5, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String mname, String desc, String sig, String[] exc) {
                    MethodVisitor mv = super.visitMethod(access, mname, desc, sig, exc);
                    if (desc != null && desc.endsWith(")V") && !mname.equals("<init>") && !mname.equals("<clinit>")) {
                        return new MethodVisitor(ASM5, mv) {
                            @Override public void visitCode() {
                                mv.visitMethodInsn(INVOKESTATIC, "net/minecraftforge/fml/common/FMLCommonHandler", "instance", "()Lnet/minecraftforge/fml/common/FMLCommonHandler;", false);
                                mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraftforge/fml/common/FMLCommonHandler", "getEffectiveSide", "()Lnet/minecraftforge/fml/relauncher/Side;", false);
                                mv.visitFieldInsn(GETSTATIC, "net/minecraftforge/fml/relauncher/Side", "SERVER", "Lnet/minecraftforge/fml/relauncher/Side;");
                                Label skip = new Label();
                                mv.visitJumpInsn(IF_ACMPNE, skip);
                                mv.visitInsn(RETURN);
                                mv.visitLabel(skip);
                                super.visitCode();
                            }
                        };
                    }
                    return mv;
                }
            };
            cr.accept(cv, 0); return cw.toByteArray();
        }

        if (transformedName.equals("net.minecraft.client.gui.GuiMainMenu")
                || transformedName.equals("net.minecraft.client.gui.GuiScreen")
                || transformedName.equals("net.minecraft.client.gui.Gui")) {
            DOTHConfig.reload();
            if (!DOTHConfig.strongCompat) return basicClass;
            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(ASM5, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String mname, String desc, String sig, String[] exc) {
                    MethodVisitor mv = super.visitMethod(access, mname, desc, sig, exc);
                    // 对所有非构造器方法注入 side check
                    if (!mname.equals("<clinit>") && !mname.equals("<init>")) {
                        final char retType = desc.charAt(desc.lastIndexOf(')') + 1);
                        return new MethodVisitor(ASM5, mv) {
                            @Override public void visitCode() {
                                mv.visitMethodInsn(INVOKESTATIC, "net/minecraftforge/fml/common/FMLCommonHandler", "instance", "()Lnet/minecraftforge/fml/common/FMLCommonHandler;", false);
                                mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraftforge/fml/common/FMLCommonHandler", "getEffectiveSide", "()Lnet/minecraftforge/fml/relauncher/Side;", false);
                                mv.visitFieldInsn(GETSTATIC, "net/minecraftforge/fml/relauncher/Side", "SERVER", "Lnet/minecraftforge/fml/relauncher/Side;");
                                Label cl = new Label();
                                mv.visitJumpInsn(IF_ACMPNE, cl);
                                // 根据返回类型放正确默认值 + return
                                switch (retType) {
                                    case 'V': mv.visitInsn(RETURN); break;
                                    case 'Z': case 'B': case 'C': case 'S': case 'I':
                                        mv.visitInsn(ICONST_0); mv.visitInsn(IRETURN); break;
                                    case 'J': mv.visitInsn(LCONST_0); mv.visitInsn(LRETURN); break;
                                    case 'F': mv.visitInsn(FCONST_0); mv.visitInsn(FRETURN); break;
                                    case 'D': mv.visitInsn(DCONST_0); mv.visitInsn(DRETURN); break;
                                    default: mv.visitInsn(ACONST_NULL); mv.visitInsn(ARETURN); break;
                                }
                                mv.visitLabel(cl);
                                super.visitCode();
                            }
                        };
                    }
                    return mv;
                }
            };
            cr.accept(cv, 0); return cw.toByteArray();
        }

        if (transformedName.equals("net.minecraft.entity.EntityLivingBase")) {
            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(ASM5, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String mname, String desc, String sig, String[] exc) {
                    if ((mname.equals("func_70097_a") || mname.equals("attackEntityFrom")) && desc.equals("(Lnet/minecraft/util/DamageSource;F)Z")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitVarInsn(ALOAD,0); mv.visitVarInsn(ALOAD,1); mv.visitVarInsn(FLOAD,2);
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "onAttacked", "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;F)Z", false);
                                LabelNode l = new LabelNode(); mv.visitJumpInsn(IFNE, l.getLabel()); mv.visitInsn(ICONST_0); mv.visitInsn(IRETURN);
                                mv.visitLabel(l.getLabel()); super.visitCode();
                            }
                        };
                    }
                    if ((mname.equals("func_70645_a") || mname.equals("onDeath")) && desc.equals("(Lnet/minecraft/util/DamageSource;)V")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitVarInsn(ALOAD,0); mv.visitVarInsn(ALOAD,1);
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "onDeath", "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;)Z", false);
                                LabelNode l = new LabelNode(); mv.visitJumpInsn(IFNE, l.getLabel()); mv.visitInsn(RETURN);
                                mv.visitLabel(l.getLabel()); super.visitCode();
                            }
                        };
                    }
                    if ((mname.equals("func_110143_aJ") || mname.equals("getHealth")) && desc.equals("()F")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitVarInsn(ALOAD,0);
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "getHealth", "(Lnet/minecraft/entity/EntityLivingBase;)F", false);
                                mv.visitInsn(FRETURN); mv.visitMaxs(1,1); mv.visitEnd();
                            }
                        };
                    }
                    if ((mname.equals("func_110138_aP") || mname.equals("getMaxHealth")) && desc.equals("()F")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitVarInsn(ALOAD,0);
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "getMaxHealth", "(Lnet/minecraft/entity/EntityLivingBase;)F", false);
                                mv.visitInsn(FRETURN); mv.visitMaxs(1,1); mv.visitEnd();
                            }
                        };
                    }
                    if ((mname.equals("func_70071_h_") || mname.equals("onUpdate")) && desc.equals("()V")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            private boolean sawForgeHook = false;
                            private Label forgeHookLabel = null;
                            @Override public void visitMethodInsn(int op, String o, String n, String d, boolean itf) {
                                if (n.equals("onLivingUpdate") && o.equals("net/minecraftforge/common/ForgeHooks"))
                                    sawForgeHook = true;
                                deadWriteFlag = false;
                                super.visitMethodInsn(op, o, n, d, itf);
                            }
                            @Override public void visitJumpInsn(int op, Label l) {
                                if (sawForgeHook && op == IFNE) { forgeHookLabel = l; sawForgeHook = false; }
                                deadWriteFlag = false;
                                super.visitJumpInsn(op, l);
                            }
                            @Override public void visitLabel(Label l) {
                                super.visitLabel(l);
                                if (forgeHookLabel != null && l == forgeHookLabel) {
                                    mv.visitVarInsn(ALOAD, 0);
                                    mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper",
                                            "hookOnUpdate", "(Lnet/minecraft/entity/EntityLivingBase;)Z", false);
                                    LabelNode ln = new LabelNode(); mv.visitJumpInsn(IFEQ, ln.getLabel()); mv.visitInsn(RETURN);
                                    mv.visitLabel(ln.getLabel());
                                    forgeHookLabel = null;
                                }
                            }
                            private boolean deadWriteFlag = false;
                            @Override public void visitInsn(int op) {
                                if (deadWriteFlag && op == RETURN) { deadWriteFlag = false; return; }
                                deadWriteFlag = false;
                                if (op == RETURN) {
                                    mv.visitVarInsn(ALOAD,0);
                                    mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "onLivingUpdatePost", "(Lnet/minecraft/entity/EntityLivingBase;)V", false);
                                }
                                super.visitInsn(op);
                            }
                            @Override public void visitVarInsn(int op, int v) { deadWriteFlag = false; super.visitVarInsn(op, v); }
                            @Override public void visitFieldInsn(int op, String o, String n, String d) {
                                if (op == PUTFIELD && n.equals("field_70128_L")) deadWriteFlag = true;
                                super.visitFieldInsn(op, o, n, d);
                            }
                            @Override public void visitIntInsn(int op, int v) { deadWriteFlag = false; super.visitIntInsn(op, v); }
                            @Override public void visitTypeInsn(int op, String t) { deadWriteFlag = false; super.visitTypeInsn(op, t); }
                            @Override public void visitLdcInsn(Object c) { deadWriteFlag = false; super.visitLdcInsn(c); }
                            @Override public void visitIincInsn(int v, int inc) { deadWriteFlag = false; super.visitIincInsn(v, inc); }
                            @Override public void visitTableSwitchInsn(int min, int max, Label dflt, Label... lbs) { deadWriteFlag = false; super.visitTableSwitchInsn(min, max, dflt, lbs); }
                            @Override public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] lbs) { deadWriteFlag = false; super.visitLookupSwitchInsn(dflt, keys, lbs); }
                            @Override public void visitMultiANewArrayInsn(String d, int dim) { deadWriteFlag = false; super.visitMultiANewArrayInsn(d, dim); }
                        };
                    }
                    if ((mname.equals("func_70089_S") || mname.equals("isEntityAlive")) && desc.equals("()Z")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitVarInsn(ALOAD,0);
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "isEntityAlive", "(Lnet/minecraft/entity/EntityLivingBase;)Z", false);
                                mv.visitInsn(IRETURN); mv.visitMaxs(1,1); mv.visitEnd();
                            }
                        };
                    }
                    return super.visitMethod(access, mname, desc, sig, exc);
                }
            };
            cr.accept(cv, 0); return cw.toByteArray();
        }

        if (transformedName.equals("net.minecraft.entity.player.EntityPlayer")) {
            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(ASM5, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String mname, String desc, String sig, String[] exc) {
                    if ((mname.equals("func_70645_a") || mname.equals("onDeath")) && desc.equals("(Lnet/minecraft/util/DamageSource;)V")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitVarInsn(ALOAD,0); mv.visitVarInsn(ALOAD,1);
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "onDeath", "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;)Z", false);
                                LabelNode l = new LabelNode(); mv.visitJumpInsn(IFNE, l.getLabel()); mv.visitInsn(RETURN);
                                mv.visitLabel(l.getLabel()); super.visitCode();
                            }
                        };
                    }
                    return super.visitMethod(access, mname, desc, sig, exc);
                }
            };
            cr.accept(cv, 0); return cw.toByteArray();
        }

        if (transformedName.equals("net.minecraft.util.CooldownTracker")) {
            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(ASM5, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String mname, String desc, String sig, String[] exc) {
                    // 拦截 setCooldown(Item, int) → 蓝C小剑剑不设冷却
                    if ((mname.equals("func_185145_a") || mname.equals("setCooldown"))
                            && desc.equals("(Lnet/minecraft/item/Item;I)V")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                // ProtectHelper.shouldSkipCooldown(item)
                                mv.visitVarInsn(ALOAD, 1); // Item 参数
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper",
                                        "shouldSkipCooldown", "(Lnet/minecraft/item/Item;)Z", false);
                                LabelNode l = new LabelNode();
                                mv.visitJumpInsn(IFEQ, l.getLabel());
                                mv.visitInsn(RETURN); // 是蓝C小剑剑 → 直接返回，不设冷却
                                mv.visitLabel(l.getLabel());
                                super.visitCode();
                            }
                        };
                    }
                    // 拦截 hasCooldown(Item) → 蓝C小剑剑永远返回 false
                    if ((mname.equals("func_185141_a") || mname.equals("hasCooldown"))
                            && desc.equals("(Lnet/minecraft/item/Item;)Z")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitVarInsn(ALOAD, 1);
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper",
                                        "shouldSkipCooldown", "(Lnet/minecraft/item/Item;)Z", false);
                                LabelNode l = new LabelNode();
                                mv.visitJumpInsn(IFEQ, l.getLabel());
                                mv.visitInsn(ICONST_0);
                                mv.visitInsn(IRETURN); // 是蓝C小剑剑 → 返回 false（无冷却）
                                mv.visitLabel(l.getLabel());
                                super.visitCode();
                            }
                        };
                    }
                    return super.visitMethod(access, mname, desc, sig, exc);
                }
            };
            cr.accept(cv, 0);
            return cw.toByteArray();
        }

        if (transformedName.equals("net.minecraft.entity.player.InventoryPlayer")) {
            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(ASM5, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String mname, String desc, String sig, String[] exc) {
                    if ((mname.equals("func_174888_l") || mname.equals("clear")) && desc.equals("()V")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitVarInsn(ALOAD, 0);
                                mv.visitFieldInsn(GETFIELD, "net/minecraft/entity/player/InventoryPlayer", "field_70458_d", "Lnet/minecraft/entity/player/EntityPlayer;");
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper",
                                        "shouldPreventClear", "(Lnet/minecraft/entity/player/EntityPlayer;)Z", false);
                                LabelNode ln = new LabelNode(); mv.visitJumpInsn(IFEQ, ln.getLabel()); mv.visitInsn(RETURN);
                                mv.visitLabel(ln.getLabel()); super.visitCode();
                            }
                        };
                    }
                    if (mname.equals("func_174925_a") && desc.equals("(Lnet/minecraft/item/Item;IILnet/minecraft/nbt/NBTTagCompound;)I")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitVarInsn(ALOAD, 0);
                                mv.visitFieldInsn(GETFIELD, "net/minecraft/entity/player/InventoryPlayer", "field_70458_d", "Lnet/minecraft/entity/player/EntityPlayer;");
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper",
                                        "shouldPreventClear", "(Lnet/minecraft/entity/player/EntityPlayer;)Z", false);
                                LabelNode ln = new LabelNode(); mv.visitJumpInsn(IFEQ, ln.getLabel()); mv.visitInsn(ICONST_0); mv.visitInsn(IRETURN);
                                mv.visitLabel(ln.getLabel()); super.visitCode();
                            }
                        };
                    }
                    return super.visitMethod(access, mname, desc, sig, exc);
                }
            };
            cr.accept(cv, 0); return cw.toByteArray();
        }

        if (transformedName.equals("net.minecraft.server.MinecraftServer")) {
            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(ASM5, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String mname, String desc, String sig, String[] exc) {
                    MethodVisitor mv = super.visitMethod(access, mname, desc, sig, exc);
                    if ((mname.equals("func_71217_p") || mname.equals("tick")) && desc.equals("()V")) {
                        return new MethodVisitor(ASM5, mv) {
                            @Override public void visitCode() {
                                mv.visitVarInsn(ALOAD,0);
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "onServerTick", "(Lnet/minecraft/server/MinecraftServer;)V", false);
                                super.visitCode();
                            }
                        };
                    }
                    if (mname.equals("run") && desc.equals("()V")) {
                        DOTHConfig.reload();
                        if (DOTHConfig.strongCompat) {
                            mv.visitCode();
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "serverRun", "(Lnet/minecraft/server/MinecraftServer;)V", false);
                            mv.visitInsn(RETURN);
                            mv.visitMaxs(1, 1); mv.visitEnd();
                            return null;
                        }
                    }
                    return mv;
                }
            };
            cr.accept(cv, ClassReader.SKIP_FRAMES); return cw.toByteArray();
        }

        if (transformedName.equals("net.minecraft.world.World")) {
            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(ASM5, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String mname, String desc, String sig, String[] exc) {
                    if ((mname.equals("func_72838_d") || mname.equals("spawnEntity")) && desc.equals("(Lnet/minecraft/entity/Entity;)Z")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitVarInsn(ALOAD,1);
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "onEntityJoinWorld", "(Lnet/minecraft/entity/Entity;)Z", false);
                                LabelNode l = new LabelNode(); mv.visitJumpInsn(IFNE, l.getLabel()); mv.visitInsn(ICONST_0); mv.visitInsn(IRETURN);
                                mv.visitLabel(l.getLabel()); super.visitCode();
                            }
                        };
                    }
                    if ((mname.equals("func_72900_e") || mname.equals("removeEntity")) && desc.equals("(Lnet/minecraft/entity/Entity;)V")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitVarInsn(ALOAD,1);
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "onRemoveEntity", "(Lnet/minecraft/entity/Entity;)Z", false);
                                LabelNode l = new LabelNode(); mv.visitJumpInsn(IFNE, l.getLabel()); mv.visitInsn(RETURN);
                                mv.visitLabel(l.getLabel()); super.visitCode();
                            }
                        };
                    }
                    if ((mname.equals("func_72835_b") || mname.equals("removeEntityDangerously")) && desc.equals("(Lnet/minecraft/entity/Entity;)V")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitVarInsn(ALOAD,1);
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "onRemoveEntity", "(Lnet/minecraft/entity/Entity;)Z", false);
                                LabelNode l = new LabelNode(); mv.visitJumpInsn(IFNE, l.getLabel()); mv.visitInsn(RETURN);
                                mv.visitLabel(l.getLabel()); super.visitCode();
                            }
                        };
                    }
                    if ((mname.equals("func_72939_s") || mname.equals("updateEntities")) && desc.equals("()V")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitVarInsn(ALOAD,0);
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "onWorldUpdatePre", "(Lnet/minecraft/world/World;)V", false);
                                super.visitCode();
                            }
                        };
                    }
                    return super.visitMethod(access, mname, desc, sig, exc);
                }
            };
            cr.accept(cv, 0); return cw.toByteArray();
        }

        if (transformedName.equals("net.minecraft.item.Item")) {
            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(ASM5, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String mname, String desc, String sig, String[] exc) {
                    if ((mname.equals("func_77663_a") || mname.equals("onUpdate")) && desc.equals("(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;IZ)V")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitVarInsn(ALOAD,1);
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "onItemUpdate", "(Lnet/minecraft/item/ItemStack;)Z", false);
                                LabelNode l = new LabelNode(); mv.visitJumpInsn(IFNE, l.getLabel()); mv.visitInsn(ICONST_0); mv.visitInsn(RETURN);
                                mv.visitLabel(l.getLabel()); super.visitCode();
                            }
                        };
                    }
                    return super.visitMethod(access, mname, desc, sig, exc);
                }
            };
            cr.accept(cv, 0); return cw.toByteArray();
        }

        // ----- Entity（setDead + onUpdate 末尾注入）-----
        if (transformedName.equals("net.minecraft.entity.Entity")) {
            ClassReader cr = new ClassReader(basicClass);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(ASM5, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String mname, String desc, String sig, String[] exc) {
                    if ((mname.equals("func_70071_h_") || mname.equals("onUpdate")) && desc.equals("()V")) {
                        return new MethodVisitor(ASM5, super.visitMethod(access, mname, desc, sig, exc)) {
                            @Override public void visitCode() {
                                mv.visitVarInsn(ALOAD,0);
                                mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "onEntityUpdatePre", "(Lnet/minecraft/entity/Entity;)V", false);
                                super.visitCode();
                            }
                            @Override public void visitInsn(int opcode) {
                                if (opcode == RETURN) {
                                    mv.visitVarInsn(ALOAD,0);
                                    mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "onEntityUpdatePost", "(Lnet/minecraft/entity/Entity;)V", false);
                                }
                                super.visitInsn(opcode);
                            }
                        };
                    }
                    return super.visitMethod(access, mname, desc, sig, exc);
                }
            };
            cr.accept(cv, 0); return cw.toByteArray();
        }

        return basicClass;
    }

    /**
     * EntityLodo 补丁：
     *  - growStage / func_82167_n 注入维度守卫（不进化、不叠 COTH_E）
     *  - 新增骑乘覆写方法：canBeRidden(true) / canBeSteered / travel / entityInit(注册 DataWatcher)
     * 全部只调自己的 RushAsmHooks，运行时方法名用 srg 名（SRP 类名不变）。
     */
    private byte[] transformLodoGrow(byte[] basicClass) {
        try {
            ClassReader cr = new ClassReader(basicClass);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);

            // 1. 已有方法注入维度守卫
            int patched = 0;
            String[] injectTargets = {"growStage", "func_82167_n"};
            String[] injectDescs = {"()V", "(Lnet/minecraft/entity/Entity;)V"};
            for (MethodNode mn : cn.methods) {
                for (int i = 0; i < injectTargets.length; i++) {
                    if (injectTargets[i].equals(mn.name) && injectDescs[i].equals(mn.desc)) {
                        InsnList insns = new InsnList();
                        insns.add(new VarInsnNode(ALOAD, 0)); // this
                        insns.add(new MethodInsnNode(INVOKESTATIC,
                                "yc/ycqin/doth/core/RushAsmHooks",
                                "shouldBlockLodoGrow",
                                "(Lnet/minecraft/entity/Entity;)Z", false));
                        LabelNode skip = new LabelNode();
                        insns.add(new JumpInsnNode(IFEQ, skip));
                        insns.add(new InsnNode(RETURN));
                        insns.add(skip);
                        mn.instructions.insert(insns);
                        patched++;
                    }
                }
            }

            // 2. 新增骑乘覆写方法（EntityLodo 未覆写这些，需添加）
            boolean addedRide = false;

            // canBeRidden(Entity)Z → true
            if (findMethod(cn, "func_184228_n", "(Lnet/minecraft/entity/Entity;)Z") == null) {
                MethodNode mn = new MethodNode(ACC_PUBLIC, "func_184228_n",
                        "(Lnet/minecraft/entity/Entity;)Z", null, null);
                mn.instructions.add(new InsnNode(ICONST_1));
                mn.instructions.add(new InsnNode(IRETURN));
                cn.methods.add(mn);
                addedRide = true;
            }

            // canBeSteered()Z → isBeingRidden()
            if (findMethod(cn, "func_82171_bF", "()Z") == null) {
                MethodNode mn = new MethodNode(ACC_PUBLIC, "func_82171_bF", "()Z", null, null);
                mn.instructions.add(new VarInsnNode(ALOAD, 0));
                mn.instructions.add(new MethodInsnNode(INVOKESTATIC,
                        "yc/ycqin/doth/core/RushAsmHooks",
                        "canBeSteered", "(Lnet/minecraft/entity/EntityLivingBase;)Z", false));
                mn.instructions.add(new InsnNode(IRETURN));
                cn.methods.add(mn);
                addedRide = true;
            }

            // travel(FFF)V → rushTravel(this,...) ? return : super.travel(this,...)
            if (findMethod(cn, "func_191986_a", "(FFF)V") == null) {
                MethodNode mn = new MethodNode(ACC_PUBLIC, "func_191986_a", "(FFF)V", null, null);
                InsnList body = new InsnList();
                body.add(new VarInsnNode(ALOAD, 0));
                body.add(new VarInsnNode(FLOAD, 1));
                body.add(new VarInsnNode(FLOAD, 2));
                body.add(new VarInsnNode(FLOAD, 3));
                body.add(new MethodInsnNode(INVOKESTATIC,
                        "yc/ycqin/doth/core/RushAsmHooks",
                        "rushTravel", "(Lnet/minecraft/entity/EntityLivingBase;FFF)Z", false));
                LabelNode done = new LabelNode();
                body.add(new JumpInsnNode(IFNE, done));
                // super.travel（EntityLivingBase 声明，运行时 srg 名 func_191986_a）
                body.add(new VarInsnNode(ALOAD, 0));
                body.add(new VarInsnNode(FLOAD, 1));
                body.add(new VarInsnNode(FLOAD, 2));
                body.add(new VarInsnNode(FLOAD, 3));
                body.add(new MethodInsnNode(INVOKESPECIAL,
                        "net/minecraft/entity/EntityLivingBase", "func_191986_a", "(FFF)V", false));
                body.add(done);
                body.add(new InsnNode(RETURN));
                mn.instructions.add(body);
                cn.methods.add(mn);
                addedRide = true;
            }

            // entityInit()V → super.entityInit + 注册 DataWatcher 状态位
            if (findMethod(cn, "func_70088_a", "()V") == null) {
                MethodNode mn = new MethodNode(ACC_PROTECTED, "func_70088_a", "()V", null, null);
                InsnList body = new InsnList();
                body.add(new VarInsnNode(ALOAD, 0));
                body.add(new MethodInsnNode(INVOKESPECIAL,
                        "com/dhanantry/scapeandrunparasites/entity/ai/misc/EntityParasiteBase",
                        "func_70088_a", "()V", false));
                body.add(new VarInsnNode(ALOAD, 0));
                body.add(new MethodInsnNode(INVOKESTATIC,
                        "yc/ycqin/doth/core/RushAsmHooks",
                        "registerRushState", "(Lnet/minecraft/entity/EntityLivingBase;)V", false));
                body.add(new InsnNode(RETURN));
                mn.instructions.add(body);
                cn.methods.add(mn);
                addedRide = true;
            }

            if (patched == 0 && !addedRide) {
                System.out.println("[DOTH] EntityLodo patch targets not found, skip");
                return basicClass;
            }

            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            System.out.println("[DOTH] EntityLodo patched: guards=" + patched + ", rideMethods=" + addedRide);
            return cw.toByteArray();
        } catch (Exception e) {
            System.out.println("[DOTH] EntityLodo transform failed: " + e);
            return basicClass;
        }
    }

    private static MethodNode findMethod(ClassNode cn, String name, String desc) {
        for (MethodNode mn : cn.methods) {
            if (name.equals(mn.name) && desc.equals(mn.desc)) {
                return mn;
            }
        }
        return null;
    }

    private boolean shouldApplyAllReturn(String className) {
        if (className.startsWith("net.minecraft.advancements.") ||
            className.startsWith("net.minecraft.client.") ||
            className.startsWith("net.minecraft.block.") ||
            className.startsWith("net.minecraft.command.")||
            className.startsWith("net.minecraft.crash.")||
            className.startsWith("net.minecraft.creativetab.")||
            className.startsWith("net.minecraft.dispenser.")||
            className.startsWith("net.minecraft.enchantment.")||
            className.startsWith("net.minecraft.entity.")||
            className.startsWith("net.minecraft.init.")||
            className.startsWith("net.minecraft.inventory.")||
            className.startsWith("net.minecraft.item.")||
            className.startsWith("net.minecraft.nbt.")||
            className.startsWith("net.minecraft.network.")||
            className.startsWith("net.minecraft.pathfinding.")||
            className.startsWith("net.minecraft.potion.")||
            className.startsWith("net.minecraft.profiler.")||
            className.startsWith("net.minecraft.realms.")||
            className.startsWith("net.minecraft.scoreboard.")||
            className.startsWith("net.minecraft.server.")||
            className.startsWith("net.minecraft.stats.")||
            className.startsWith("net.minecraft.tileentity.") ||
            className.startsWith("net.minecraft.util.") ||
            className.startsWith("net.minecraft.village.") ||
            className.startsWith("net.minecraft.world.") ||
            className.startsWith("net.minecraftforge.") ||
            className.startsWith("yc.ycqin.doth.") ||
            className.startsWith("java.") ||
            className.startsWith("javax.") ||
            className.startsWith("org.apache.") ||
            className.startsWith("org.lwjgl.") ||
            className.startsWith("com.google.") ||
            className.startsWith("com.mojang.") ||
            className.startsWith("io.netty.") ||
            className.startsWith("it.unimi.dsi.fastutil.") ||
            className.startsWith("org.objectweb.asm.") ||
            className.startsWith("joptsimple.") ||
            className.startsWith("paulscode.") ||
            className.startsWith("oshi.") ||
            className.startsWith("jline.") ||
            className.startsWith("gnu.") ||
            className.startsWith("codechicken.lib.") ||
            className.startsWith("goblinbob.mobends.") ||
            className.startsWith("com.replaymod.") ||
            className.startsWith("com.dhanantry.scapeandrunparasites.")) return false;
        return !className.startsWith("sun.") && !className.startsWith("com.sun.");
    }

    private byte[] applyAllReturn(String className, byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        String superName = cr.getSuperName();
        Set<String> superMethods = getSuperMethods(superName);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassVisitor cv = new ClassVisitor(ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String mname, String desc, String sig, String[] exc) {
                MethodVisitor mv = super.visitMethod(access, mname, desc, sig, exc);
                if (mname.equals("<init>") || mname.equals("<clinit>") || (access & ACC_ABSTRACT) != 0 || (access & ACC_NATIVE) != 0) return mv;
                String rt = getReturnType(desc); boolean isVoid = rt.equals("V");
                if (!isVoid && !superMethods.contains(mname + desc)) return mv;
                return new MethodVisitor(ASM5, mv) {
                    @Override public void visitCode() {
                        mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/AllreturnConfig", "isEnabled", "()Z", false);
                        Label cl = new Label(); mv.visitJumpInsn(IFEQ, cl);
                        if (isVoid) { mv.visitInsn(RETURN); }
                        else {
                            mv.visitVarInsn(ALOAD,0); int li=1;
                            String pd = getParamDesc(desc); String[] ps = parseParams(pd);
                            for (String p : ps) { mv.visitVarInsn(loadOpcode(p), li); li += getSize(p); }
                            mv.visitMethodInsn(INVOKESPECIAL, superName, mname, desc, false);
                            insertReturnByType(rt, mv);
                        }
                        mv.visitLabel(cl); super.visitCode();
                    }
                };
            }
        };
        cr.accept(cv, 0); return cw.toByteArray();
    }

    private byte[] transformGuiScreen(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
            @Override protected String getCommonSuperClass(String t1, String t2) {
                try { return super.getCommonSuperClass(t1, t2); } catch (Exception e) { return "java/lang/Object"; }
            }
        };
        ClassVisitor cv = new ClassVisitor(ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String mname, String desc, String sig, String[] exc) {
                MethodVisitor mv = super.visitMethod(access, mname, desc, sig, exc);
                if ((mname.equals("func_146276_a_") || mname.equals("drawHoveringText")) && desc.equals("(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)V")) {
                    return new MethodVisitor(ASM5, mv) {
                        @Override public void visitCode() {
                            mv.visitVarInsn(ALOAD,0); mv.visitVarInsn(ALOAD,1); mv.visitVarInsn(ILOAD,2); mv.visitVarInsn(ILOAD,3); mv.visitVarInsn(ALOAD,4);
                            mv.visitMethodInsn(INVOKESTATIC, "yc/ycqin/doth/core/ProtectHelper", "handleTooltip", "(Lnet/minecraft/client/gui/GuiScreen;Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)Z", false);
                            Label l = new Label(); mv.visitJumpInsn(IFEQ, l); mv.visitInsn(RETURN); mv.visitLabel(l); super.visitCode();
                        }
                    };
                }
                return mv;
            }
        };
        cr.accept(cv, 0); return cw.toByteArray();
    }

    private Set<String> getSuperMethods(String sn) {
        if (sn.equals("java/lang/Object")) return new HashSet<>();
        if (SUPER_METHODS_CACHE.containsKey(sn)) return SUPER_METHODS_CACHE.get(sn);
        Set<String> m = new HashSet<>();
        try {
            LaunchClassLoader l = (LaunchClassLoader) ProtectClassTransformer.class.getClassLoader();
            InputStream is = l.getResourceAsStream(sn + ".class");
            if (is == null) { SUPER_METHODS_CACHE.put(sn, m); return m; }
            new ClassReader(is).accept(new ClassVisitor(ASM5) {
                @Override public MethodVisitor visitMethod(int a, String n, String d, String s, String[] e) {
                    if ((a & ACC_STATIC) == 0 && (a & ACC_PRIVATE) == 0 && !n.equals("<init>") && !n.equals("<clinit>")) m.add(n + d);
                    return null;
                }
            }, 0);
            is.close();
        } catch (Exception e) {}
        SUPER_METHODS_CACHE.put(sn, m); return m;
    }

    private String getReturnType(String d) { int p = d.lastIndexOf(')'); if (p == -1) return "V"; String r = d.substring(p+1); return (r.startsWith("L")||r.startsWith("["))? "L":r; }
    private String getParamDesc(String d) { int s=d.indexOf('('),e=d.lastIndexOf(')'); return (s==-1||e==-1)?"":d.substring(s+1,e); }
    private String[] parseParams(String d) {
        java.util.List<String> ps = new java.util.ArrayList<>(); int i=0;
        while(i<d.length()){
            char c=d.charAt(i);
            if(c=='L'){ int e=d.indexOf(';',i); if(e!=-1){ ps.add(d.substring(i,e+1)); i=e+1; } else break; }
            else if(c=='['){ int s=i; while(i<d.length()&&d.charAt(i)=='[')i++; if(i<d.length()&&d.charAt(i)=='L'){ int e=d.indexOf(';',i); if(e!=-1){ ps.add(d.substring(s,e+1)); i=e+1; } else break; } else { ps.add(d.substring(s,i+1)); i++; } }
            else { ps.add(String.valueOf(c)); i++; }
        }
        return ps.toArray(new String[0]);
    }
    private int loadOpcode(String t) { switch(t.charAt(0)) { case 'L':case '[':return ALOAD; case 'I':case 'B':case 'C':case 'S':case 'Z':return ILOAD; case 'J':return LLOAD; case 'F':return FLOAD; case 'D':return DLOAD; default:return ALOAD; } }
    private int getSize(String t) { return (t.charAt(0)=='J'||t.charAt(0)=='D')?2:1; }
    private void insertReturnByType(String t, MethodVisitor mv) { switch(t) { case "V":mv.visitInsn(RETURN);break; case "Z":case "B":case "C":case "S":case "I":mv.visitInsn(IRETURN);break; case "J":mv.visitInsn(LRETURN);break; case "F":mv.visitInsn(FRETURN);break; case "D":mv.visitInsn(DRETURN);break; default:mv.visitInsn(ARETURN); } }
}

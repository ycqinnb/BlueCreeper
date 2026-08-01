package yc.ycqin.doth.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class PlayerDeadTransformer implements IClassTransformer {
    private static final String TARGET_CLASS = "net.minecraft.entity.player.EntityPlayer";
    private static final String METHOD_NAME = "setDead";
    private static final String METHOD_DESC = "()V";
    private static final String HELPER_CLASS = "yc/ycqin/doth/core/ProtectHelper";
    private static final String HELPER_METHOD = "setDead";
    private static final String HELPER_DESC = "(Lnet/minecraft/entity/player/EntityPlayer;)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!transformedName.equals(TARGET_CLASS)) {
            return basicClass;
        }

        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String mname, String mdesc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, mname, mdesc, signature, exceptions);
                if (mname.equals(METHOD_NAME) && mdesc.equals(METHOD_DESC)) {
                    return new MethodVisitor(ASM5, mv) {
                        @Override
                        public void visitCode() {
                            // 调用 ProtectHelper.setDead(this)
                            mv.visitVarInsn(ALOAD, 0); // this
                            mv.visitMethodInsn(INVOKESTATIC, HELPER_CLASS, HELPER_METHOD, HELPER_DESC, false);
                            // 直接返回
                            mv.visitInsn(RETURN);
                            // 设置最大栈深度和局部变量数（COMPUTE_MAXS 会自动计算，但为保险手动设置）
                            mv.visitMaxs(1, 1);
                            mv.visitEnd();
                        }
                    };
                }
                return mv;
            }
        };
        cr.accept(cv, 0);
        return cw.toByteArray();
    }
}

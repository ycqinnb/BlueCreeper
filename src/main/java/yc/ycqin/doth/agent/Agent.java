package yc.ycqin.doth.agent;

import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class Agent {
    public static boolean is = false;
    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer)
                    throws IllegalClassFormatException {
                if ("net/minecraft/launchwrapper/LaunchClassLoader".equals(className)) {
                    return rewriteRegisterTransformer(classfileBuffer);
                }
                return null;
            }
        }); // 支持 retransform
    }

    private static byte[] rewriteRegisterTransformer(byte[] original) {
        ClassReader cr = new ClassReader(original);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if ("registerTransformer".equals(name) && "(Ljava/lang/String;)V".equals(desc)) {
                    System.out.println("[DOTH Agent] Found registerTransformer method, rewriting...");
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        public void visitCode() {
                            // 1. 加载参数（类名）
                            mv.visitVarInsn(Opcodes.ALOAD, 1);
                            // 2. 调用检查方法：判断是否为 Forge 核心转换器
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                    "yc/ycqin/doth/agent/Agent",
                                    "isForgeTransformer",
                                    "(Ljava/lang/String;)Z",
                                    false);
                            // 3. 如果返回 true（是 Forge），跳转到原方法体
                            Label continueLabel = new Label();
                            mv.visitJumpInsn(Opcodes.IFNE, continueLabel);
                            // 4. 不是 Forge：拦截，直接返回
                            // 我们无法在字节码中直接打印 className，可以省略日志，或通过静态方法打印
                            mv.visitInsn(Opcodes.RETURN);
                            // 5. 原方法体标签
                            mv.visitLabel(continueLabel);
                            // 6. 继续执行原方法
                            super.visitCode();
                        }
                    };
                }
                return super.visitMethod(access, name, desc, signature, exceptions);
            }
        };
        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    public static boolean isForgeTransformer(String className) {
        System.out.println("[doth]"+className);
        // 判断是否为 Forge 核心转换器
        return className.startsWith("net.minecraftforge.") ||
                className.startsWith("$wrapper.net.minecraftforge.") ||
                className.contains("yc.ycqin.") ||
                className.startsWith("net.minecraft.launchwrapper.");
    }
}
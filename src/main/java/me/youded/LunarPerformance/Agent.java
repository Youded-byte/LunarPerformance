package me.youded.LunarPerformance;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Arrays;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class Agent {
    public static void premain(String args, Instrumentation inst) {
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if (classfileBuffer == null || classfileBuffer.length == 0) {
                    return new byte[0];
                }

                if (!className.contains("/")) {
                    ClassReader cr = new ClassReader(classfileBuffer);
                    if (cr.getInterfaces().length == 0 && "java/lang/Object".equals(cr.getSuperName())) {
                        ClassNode cn = new ClassNode();

                        cr.accept(cn, 0);

                        for (MethodNode method : cn.methods) {
                            if ("(Ljava/lang/String;[BZ)Z".equals(method.desc)
                                    && method.access == Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL) {
                                method.instructions.clear();
                                method.localVariables.clear();
                                method.exceptions.clear();
                                method.tryCatchBlocks.clear();
                                method.instructions.add(new InsnNode(Opcodes.ICONST_1));
                                method.instructions.add(new InsnNode(Opcodes.IRETURN));
                                ClassWriter cw = new ClassWriter(cr, 0);
                                cn.accept(cw);
                                return cw.toByteArray();
                            }
                        }
                    }
                }

                if (className.startsWith("net/minecraft")) {
                    ClassReader cr = new ClassReader(classfileBuffer);

                    if (cr.getInterfaces().length == 0 && "java/lang/Object".equals(cr.getSuperName())) {
                        ClassNode cn = new ClassNode();

                        cr.accept(cn, 0);

                        for (MethodNode method : cn.methods) {

                            boolean hasString = Arrays.stream(method.instructions.toArray())
                                    .filter(LdcInsnNode.class::isInstance)
                                    .map(LdcInsnNode.class::cast)
                                    .map(inst -> inst.cst)
                                    .anyMatch("VboRegions not supported, missing: "::equals);
                            if (hasString) {
                                for (AbstractInsnNode insn : method.instructions) {
                                    if (insn.getOpcode() == Opcodes.INVOKESTATIC
                                            && ((MethodInsnNode) insn).name.equals("initDisplay")) {
                                        method.instructions.remove(insn);
                                        ClassWriter cw = new ClassWriter(cr, 0);
                                        cn.accept(cw);
                                        return cw.toByteArray();
                                    }
                                }
                            }
                        }
                    }

                    if (cr.getInterfaces().length == 3 && "java/lang/Object".equals(cr.getSuperName())) {
                        ClassNode cn = new ClassNode();

                        cr.accept(cn, 0);

                        for (MethodNode method : cn.methods) {
                            boolean hasString = Arrays.stream(method.instructions.toArray())
                                    .filter(LdcInsnNode.class::isInstance)
                                    .map(LdcInsnNode.class::cast)
                                    .map(inst -> inst.cst)
                                    .anyMatch("Couldn't set pixel format"::equals);

                            if (hasString) {
                                method.instructions.clear();
                                method.localVariables.clear();
                                method.exceptions.clear();
                                method.tryCatchBlocks.clear();
                                method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/optifine/Config",
                                        "initDisplay", "()V"));
                                method.instructions.add(new InsnNode(Opcodes.RETURN));
                                ClassWriter cw = new ClassWriter(cr, 0);
                                cn.accept(cw);
                                return cw.toByteArray();
                            }
                        }
                    }
                }

                return classfileBuffer;
            }
        });
    }
}
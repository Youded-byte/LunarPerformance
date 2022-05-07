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
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
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

                if (className.equals("net/optifine/Config")) {
                    ClassReader cr = new ClassReader(classfileBuffer);

                    if (cr.getInterfaces().length == 0 && "java/lang/Object".equals(cr.getSuperName())) {
                        ClassNode cn = new ClassNode();

                        cr.accept(cn, 0);

                        boolean found = false;
                        for (MethodNode method : cn.methods) {
                            if (method.name.equals("checkDisplaySettings")) {
                                found = true;
                                cn.fields.add(
                                        new FieldNode(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_VOLATILE,
                                                "isDisplayCreated", "Z", null, true));

                                for (AbstractInsnNode insn : method.instructions) {
                                    if (insn.getOpcode() == Opcodes.IFLE) {
                                        method.instructions.insert(insn, new FieldInsnNode(Opcodes.PUTSTATIC,
                                                "net/optifine/Config", "isDisplayCreated", "Z"));
                                        method.instructions.insert(insn, new InsnNode(Opcodes.ICONST_1));
                                    }
                                }
                            }
                            if (method.name.equals("initDisplay")) {
                                for (AbstractInsnNode insn : method.instructions) {
                                    if (insn.getOpcode() == Opcodes.INVOKESTATIC
                                            && ((MethodInsnNode) insn).name.equals("checkDisplaySettings")) {
                                        method.instructions.remove(insn);
                                    }
                                }
                            }
                        }
                        if (found) {
                            ClassWriter cw = new ClassWriter(cr, 0);
                            cn.accept(cw);
                            return cw.toByteArray();
                        }

                    }
                }

                if (className.startsWith("net/minecraft")) {
                    ClassReader cr = new ClassReader(classfileBuffer);

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
                                method.instructions
                                        .insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/optifine/Config",
                                                "checkDisplaySettings", "()V"));
                                for (MethodNode methoda : cn.methods) {
                                    if (methoda.name.endsWith("onCreateDisplay")) {
                                        for (AbstractInsnNode insn : methoda.instructions) {
                                            if (insn.getOpcode() == Opcodes.BIPUSH) {
                                                methoda.instructions.insertBefore(insn,
                                                        new FieldInsnNode(Opcodes.GETSTATIC, "net/optifine/Config",
                                                                "isDisplayCreated", "Z"));
                                                methoda.instructions.insert(insn, new InsnNode(Opcodes.IMUL));
                                            }
                                        }
                                    }
                                }
                                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
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
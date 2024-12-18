package mappings.plugin.task.build;

import org.gradle.api.tasks.TaskContainer;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.quiltmc.draftsman.asm.visitor.DraftsmanAdapterClassVisitor;
import mappings.plugin.plugin.ProcessMappingsPlugin;

public abstract class EraseByteCodeTask extends TransformJarClassesTask {
    /**
     * {@linkplain TaskContainer#register Registered} by {@link ProcessMappingsPlugin}.
     */
    public static final String ERASE_BYTECODE_TASK_NAME = "eraseBytecode";

    public EraseByteCodeTask() {
        this.getVisitorFactories().add(DraftsmanAdapterClassVisitor::new);
        // Set protected/package-private classes to public so that we don't have any access compile errors.
        // TODO: Fix this by putting the classes in the same package. Javadoc shows the modifier
        this.getVisitorFactories().add(PublicClassVisitor::new);

        // filter out anonymous classes
        this.getFilters().add(classNode -> classNode.outerClass == null);
    }

    private static class PublicClassVisitor extends ClassVisitor {
        protected PublicClassVisitor(ClassVisitor classVisitor) {
            super(Opcodes.ASM9, classVisitor);
        }

        private static int toPublicAccess(int access) {
            if (
                (access & Opcodes.ACC_PROTECTED) != 0 || (
                    (access & Opcodes.ACC_PRIVATE) == 0
                    && (access & Opcodes.ACC_PUBLIC) == 0
                )
            ) {
                access = access & ~Opcodes.ACC_PROTECTED;
                access = access | Opcodes.ACC_PUBLIC;
            }

            return access;
        }

        @Override
        public void visit(
            int version, int access, String name, String signature, String superName, String[] interfaces
        ) {
            super.visit(version, toPublicAccess(access), name, signature, superName, interfaces);
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            super.visitInnerClass(name, outerName, innerName, toPublicAccess(access));
        }
    }
}

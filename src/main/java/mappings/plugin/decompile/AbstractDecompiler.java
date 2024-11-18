package mappings.plugin.decompile;

import org.gradle.api.logging.Logger;
import mappings.plugin.decompile.javadoc.ClassJavadocProvider;
import mappings.plugin.decompile.javadoc.FieldJavadocProvider;
import mappings.plugin.decompile.javadoc.MethodJavadocProvider;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

public abstract class AbstractDecompiler {
    private final Logger logger;

    public AbstractDecompiler(Logger logger) {
        this.logger = logger;
    }

    public void decompile(
        Collection<Path> sources, Path outputDir, Map<String, Object> options, Collection<File> libraries
    ) {
        this.decompile(sources.stream().map(Path::toFile).toList(), outputDir.toFile(), options, libraries);
    }

    public abstract void decompile(
        Collection<File> sources, File outputDir, Map<String, Object> options, Collection<File> libraries
    );

    protected Logger getLogger() {
        return this.logger;
    }

    public void withClassJavadocProvider(ClassJavadocProvider javadocProvider) {
    }

    public void withFieldJavadocProvider(FieldJavadocProvider javadocProvider) {
    }

    public void withMethodJavadocProvider(MethodJavadocProvider javadocProvider) {
    }
}

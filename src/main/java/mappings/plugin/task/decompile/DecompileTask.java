package mappings.plugin.task.decompile;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import mappings.plugin.constants.Groups;
import mappings.plugin.decompile.AbstractDecompiler;
import mappings.plugin.decompile.Decompilers;
import mappings.plugin.decompile.javadoc.ClassJavadocProvider;
import mappings.plugin.decompile.javadoc.FieldJavadocProvider;
import mappings.plugin.decompile.javadoc.MethodJavadocProvider;
import mappings.plugin.decompile.javadoc.UniversalJavadocProvider;

import java.io.IOException;
import java.util.HashMap;

import static mappings.plugin.util.ProviderUtil.toOptional;

public abstract class DecompileTask extends DefaultTask {
    @Input
    public abstract Property<Decompilers> getDecompiler();

    @Optional
    @Input
    public abstract MapProperty<String, Object> getDecompilerOptions();

    /**
     * If this is present,
     * {@link #getClassJavadocSource() classJavadocSource},
     * {@link #getFieldJavadocSource() fieldJavadocSource}, and
     * {@link #getMethodJavadocSource() methodJavadocSource}
     * will each use this {@link mappings.plugin.decompile.javadoc.JavadocProvider JavadocProvider}
     * if they haven't been assigned their own.
     */
    @Optional
    @Input
    public abstract Property<UniversalJavadocProvider> getDefaultJavadocSource();

    @Optional
    @Input
    public abstract Property<ClassJavadocProvider> getClassJavadocSource();

    @Optional
    @Input
    public abstract Property<FieldJavadocProvider> getFieldJavadocSource();

    @Optional
    @Input
    public abstract Property<MethodJavadocProvider> getMethodJavadocSource();

    @InputFiles
    public abstract ConfigurableFileCollection getSources();

    @InputFiles
    public abstract ConfigurableFileCollection getLibraries();

    @OutputDirectory
    public abstract DirectoryProperty getOutput();

    public DecompileTask() {
        this.setGroup(Groups.DECOMPILE);

        this.getClassJavadocSource().convention(this.getDefaultJavadocSource());
        this.getFieldJavadocSource().convention(this.getDefaultJavadocSource());
        this.getMethodJavadocSource().convention(this.getDefaultJavadocSource());
    }

    @TaskAction
    public void decompile() throws IOException {
        final AbstractDecompiler decompiler = this.getDecompiler().get().create(this.getLogger());

        toOptional(this.getClassJavadocSource())
            .ifPresent(decompiler::withClassJavadocProvider);

        toOptional(this.getFieldJavadocSource())
            .ifPresent(decompiler::withFieldJavadocProvider);

        toOptional(this.getMethodJavadocSource())
            .ifPresent(decompiler::withMethodJavadocProvider);

        decompiler.decompile(
            this.getSources().getFiles(),
            this.getOutput().get().getAsFile(),
            // mapping to HashMap is required; the unmapped Map is unmodifiable and VineflowerDecompiler needs to
            // modify it (this issue occurred for decompileTargetVineflower)
            this.getDecompilerOptions().map(HashMap::new).getOrElse(new HashMap<>()),
            this.getLibraries().getFiles()
        );
    }
}

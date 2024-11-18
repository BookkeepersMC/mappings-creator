package mappings.plugin.task.lint;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;
import mappings.plugin.constants.Groups;
import mappings.plugin.plugin.MapMinecraftJarsPlugin;
import mappings.plugin.plugin.MappingsBasePlugin;
import mappings.plugin.task.MappingsDirConsumingTask;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Searches the passed {@link #getMappingsDir() mappingsDir} for any mappings files that map the same class.<br>
 * The task fails if any duplicates or other errors are found.
 * <p>
 * Duplicate mappings are usually the result of running {@code git merge/rebase} and
 * inadvertently combining two histories that give the same class two different names.
 * <p>
 * Any file that's empty or that lacks the {@value MAPPING_EXTENSION} extension will also be reported.
 *
 * @see MappingsBasePlugin QuiltMappingsBasePlugin's configureEach
 */
public abstract class FindDuplicateMappingFilesTask extends DefaultTask implements MappingsDirConsumingTask {
    /**
     * {@linkplain org.gradle.api.tasks.TaskContainer#register Registered} by {@link MapMinecraftJarsPlugin}.
     */
    public static final String FIND_DUPLICATE_MAPPING_FILES_TASK_NAME = "findDuplicateMappingFiles";

    public static final String MAPPING_EXTENSION = "mapping";

    private static final Pattern EXPECTED_CLASS =
        Pattern.compile("^CLASS (?:net/minecraft|com/mojang/blaze3d)/(?:\\w+/)*\\w+(?= )");

    public FindDuplicateMappingFilesTask() {
        this.setGroup(Groups.LINT);
    }

    @TaskAction
    public void run() {
        final Multimap<String, File> allMappings = HashMultimap.create();
        final Set<String> duplicateMappings = new HashSet<>();
        final List<File> emptyFiles = new ArrayList<>();
        final List<File> wrongExtensionFiles = new ArrayList<>();

        try (Stream<Path> mappingPaths = Files.walk(this.getMappingsDir().get().getAsFile().toPath())) {
            mappingPaths.map(Path::toFile)
                .filter(File::isFile)
                .forEach(mappingFile -> {
                    try (var reader = new BufferedReader(new FileReader(mappingFile))) {
                        final String firstLine = reader.readLine();
                        if (firstLine != null) {
                            getClassMatch(firstLine).ifPresent(
                                classMatch -> {
                                    final Collection<File> classMappings = allMappings.get(classMatch);

                                    if (!classMappings.isEmpty()) duplicateMappings.add(classMatch);

                                    classMappings.add(mappingFile);
                                }
                            );
                        } else {
                            emptyFiles.add(mappingFile);
                        }

                        if (!mappingFile.toString().endsWith("." + MAPPING_EXTENSION)) {
                            wrongExtensionFiles.add(mappingFile);
                        }
                    } catch (IOException e) {
                        throw new GradleException("Unexpected error accessing " + MAPPING_EXTENSION + " file", e);
                    }
                });
        } catch (IOException e) {
            throw new GradleException("Unexpected error accessing " + MAPPING_EXTENSION + "s directory", e);
        }

        final Logger logger = this.getLogger();
        final List<String> errorMessages = new ArrayList<>();
        if (!duplicateMappings.isEmpty()) {
            final String message = "%d class%s mapped by multiple files".formatted(
                duplicateMappings.size(),
                duplicateMappings.size() == 1 ? "" : "es"
            );
            errorMessages.add(message);

            logger.error("Found {}!", message);
            for (final String duplicateMapping : duplicateMappings) {
                logger.error("\t{} is mapped by:", duplicateMapping);

                for (final File mappingFile : allMappings.get(duplicateMapping)) {
                    logger.error("\t\t{}", mappingFile);
                }
            }
        }

        if (!emptyFiles.isEmpty()) {
            final String message = "%d empty file%s".formatted(
                emptyFiles.size(),
                emptyFiles.size() == 1 ? "" : "s"
            );
            errorMessages.add(message);

            logger.error("Found {}!", message);
            for (final File emptyFile : emptyFiles) {
                logger.error("\t{}", emptyFile);
            }
        }

        if (!wrongExtensionFiles.isEmpty()) {
            final String message = ("%d file%s without the " + MAPPING_EXTENSION + " extension").formatted(
                wrongExtensionFiles.size(),
                wrongExtensionFiles.size() == 1 ? "" : "s"
            );
            errorMessages.add(message);

            logger.error("Found {}!", message);
            for (final File wrongExtensionFile : wrongExtensionFiles) {
                logger.error("\t{}", wrongExtensionFile);
            }
        }

        if (!errorMessages.isEmpty()) {
            final var fullError = new StringBuilder("Found ");
            switch (errorMessages.size()) {
                case 1 -> { }
                case 2 -> fullError.append(errorMessages.getFirst()).append(" and ");
                default -> {
                    final List<String> allButLastMessage = errorMessages.subList(0, errorMessages.size() - 1);
                    for (final String message : allButLastMessage) {
                        fullError.append(message).append(", ");
                    }

                    fullError.append("and ");
                }
            }

            fullError.append(errorMessages.getLast()).append("! See the log for details.");

            throw new GradleException(fullError.toString());
        }
    }

    private static Optional<String> getClassMatch(String firstLine) {
        final var expectedClassMatcher = EXPECTED_CLASS.matcher(firstLine);
        return expectedClassMatcher.find() ?
            Optional.of(expectedClassMatcher.group(0)) :
            Optional.empty();
    }
}

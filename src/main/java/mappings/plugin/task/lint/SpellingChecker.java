package mappings.plugin.task.lint;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import mappings.plugin.task.build.GeneratePackageInfoMappingsTask;

import static mappings.plugin.task.lint.SpellingChecker.PluralSuffixes.IES;
import static mappings.plugin.task.lint.SpellingChecker.PluralSuffixes.SES;
import static mappings.plugin.task.lint.SpellingChecker.PluralSuffixes.VES;

public class SpellingChecker implements Checker<Entry<?>> {
    private static final String METHOD_PREFIX = "m_";
    private static final String CLASS_PREFIX = "C_";
    private static final char PACKAGE_SEPARATOR = '/';

    private static final Set<String> MINECRAFT_WORDS = new HashSet<>();
    static {
        // collect minecraft words
        final InputStream parsedFile =
            SpellingChecker.class.getClassLoader().getResourceAsStream("minecraft_specific_words.txt");
        if (parsedFile == null) {
            throw new RuntimeException("could not find minecraft word file for spelling check in mapping lint task!");
        }

        final Set<String> minecraftWords = getLines(parsedFile);
        MINECRAFT_WORDS.addAll(minecraftWords);
    }

    private static Set<String> getLines(InputStream stream) {
        return new BufferedReader(new InputStreamReader(stream))
            .lines()
            .filter(line -> !line.isBlank() && !line.startsWith("//"))
            .collect(Collectors.toSet());
    }

    private static final Set<String> words = new HashSet<>();

    @Override
    public void update(MappingLintTask.LintParameters parameters) {
        words.addAll(MINECRAFT_WORDS);

        try {
            final FileInputStream spellingFile = new FileInputStream(parameters.getSpellingFile().get().getAsFile());
            final Set<String> spellingWords = getLines(spellingFile);
            words.addAll(spellingWords);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Unable to find spelling file!");
        }
    }

    @Override
    public void check(
        Entry<?> entry, EntryMapping mapping, Function<Entry<?>, AccessFlags> accessProvider, ErrorReporter errorReporter
    ) {
        final String name = mapping.targetName();

        if (name != null
            && !name.startsWith(GeneratePackageInfoMappingsTask.DEFAULT_PACKAGE_NAME)
            // ignore unmapped methods and classes
            && !(entry instanceof MethodEntry && name.startsWith(METHOD_PREFIX))
            && !(entry instanceof ClassEntry && name.startsWith(CLASS_PREFIX))) {

            checkMapping(entry, name, errorReporter);
            checkJavadoc(mapping.javadoc(), errorReporter);
        }
    }

    private static void checkJavadoc(String javadoc, ErrorReporter errorReporter) {
        if (javadoc != null && !javadoc.isEmpty()) {
            // in this case we just have to run the world's slowest regex,
            // there's no clever optimisations based on type to be had
            // note: this regex does not exclude dashes, we cannot handle every single "x-like" scattered around
            final List<String> splitJavadoc = splitWords(List.of(javadoc), word -> true, "[^A-Za-z']");
            for (final String word : splitJavadoc) {
                if (!word.isBlank()) {
                    checkWord(word.toLowerCase(), word, errorReporter, MappingType.JAVADOC);
                }
            }
        }
    }

    private static void checkMapping(Entry<?> entry, String name, ErrorReporter errorReporter) {
        final List<String> namesToSplit = new ArrayList<>();

        // a contains check is necessary here because inner classes do not have package data
        if (entry instanceof ClassEntry && name.contains(String.valueOf(PACKAGE_SEPARATOR))) {
            // add class name - we don't have to handle underscores
            namesToSplit.add(name.substring(name.lastIndexOf(PACKAGE_SEPARATOR) + 1));
            // add package names and handle underscores
            final String[] packageNames =
                name.substring(0, name.lastIndexOf(PACKAGE_SEPARATOR)).split(String.valueOf(PACKAGE_SEPARATOR));
            for (final String packageName : packageNames) {
                final String[] split = packageName.split("_");
                namesToSplit.addAll(List.of(split));
            }
        } else {
            // handle underscores
            final String[] split = name.split("_");
            namesToSplit.addAll(List.of(split));
        }

        final List<String> splitNames = splitWords(namesToSplit, word -> word.chars().anyMatch(Character::isDigit), "\\d");

        // check
        for (final String word : splitNames) {
            checkWord(word, word, errorReporter, MappingType.ENTRY);
        }
    }

    /**
     * Splits provided words by camelCase, then proceeds to split them by the provided regex if the stripping check succeeds
     * @param words the words to split
     * @param strippingCheck a check to run before splitting via the regex
     * @param strippingRegex a regex to split words by
     * @return a list of split words
     */
    private static List<String> splitWords(List<String> words, Predicate<String> strippingCheck, String strippingRegex) {
        // split by uppercase characters and numbers and preserve
        final List<String> splitNames = new ArrayList<>();
        for (final String nameToSplit : words) {
            // skip regex on all uppercase words
            if (!nameToSplit.matches("[A-Z]+")) {
                // split by uppercase letters and preserve them in the split strings
                // also map to all lowercase
                final Set<String> split = Arrays.stream(nameToSplit.split("(?<=[a-zA-Z])(?=[A-Z])"))
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

                splitNames.addAll(split);
            } else {
                splitNames.add(nameToSplit.toLowerCase());
            }
        }

        final List<String> splitWords = new ArrayList<>();

        for (final String word : splitNames) {
            // ignore numbers
            // this requires special handling since we can't use the same regex we use for uppercase characters
            // that way it would preserve the numbers, and we'd have to strip them anyway
            if (strippingCheck.test(word)) {
                final String[] splitByInts = word.split(strippingRegex);
                splitWords.addAll(Arrays.asList(splitByInts));
            } else {
                splitWords.add(word);
            }
        }

        return splitWords;
    }

    private static void checkWord(String word, String reportedWord, ErrorReporter reporter, MappingType type) {
        if (!word.isBlank() && !words.contains(word)) {
            // ignore words that end with '
            // this is to avoid things like "classes'" throwing an error, as it's a perfectly valid usage
            if (word.endsWith("'")) {
                // the word still needs to be run through the plural check, so we validate
                word = word.substring(word.length() - 1);
            }

            // ignore plural versions of words
            if (word.endsWith("s") && checkPlural(word)) {
                return;
            }

            // single characters are always allowed
            if (word.length() > 1) {
                reporter.error(type + " contains unknown/misspelled word: \"" + reportedWord + "\"");
            }
        }
    }

    private static boolean checkPlural(String word) {
        final String nonPlural;
        if (word.endsWith(IES)) { // berries -> berry
            nonPlural = word.substring(word.length() - IES.length()) + "y";
        } else if (word.endsWith(VES)) { // leaves -> leaf
            final String substring = word.substring(word.length() - VES.length());

            if (words.contains(substring + "f")) {
                // word has been found, we can skip the next contains check
                return true;
            } else { // knives -> knife
                nonPlural = substring + "fe";
            }
        } else if (word.endsWith(SES)) { // glasses -> glass
            // leave one "s"
            nonPlural = word.substring(word.length() - (SES.length() - 1));
        } else { // ones -> one
            nonPlural = word.substring(word.length() - 1);
        }

        return words.contains(nonPlural);
    }

    private enum MappingType {
        JAVADOC("javadoc"),
        ENTRY("entry name");

        private final String name;

        MappingType(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String toString() {
            return this.getName();
        }
    }

    interface PluralSuffixes {
        String IES = "ies";
        String VES = "ves";
        String SES = "ses";
    }
}

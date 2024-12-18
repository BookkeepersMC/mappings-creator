package mappings.plugin.task.lint;

import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.function.Function;
import java.util.regex.Pattern;

public final class JavadocChecker implements Checker<Entry<?>> {
    private static final Pattern PARAM_DOC_LINE = Pattern.compile("^@param\\s+[^<].*$");

    @Override
    public void check(
        Entry<?> entry, EntryMapping mapping,
        Function<Entry<?>, AccessFlags> accessProvider,
        ErrorReporter errorReporter
    ) {
        final String javadoc = mapping.javadoc();

        if (javadoc != null && !javadoc.isEmpty()) {
            if (entry instanceof LocalVariableEntry lv && lv.isArgument()) {
                if (javadoc.endsWith(".")) {
                    errorReporter.error("parameter javadoc ends with '.'");
                }

                if (Character.isUpperCase(javadoc.charAt(0))) {
                    String word = getFirstWord(javadoc);

                    // ignore single-letter "words" (like X or Z)
                    if (word.length() > 1) {
                        errorReporter.error("parameter javadoc starts with uppercase word '" + word + "'");
                    }
                }
            } else if (entry instanceof MethodEntry) {
                if (javadoc.lines().anyMatch(JavadocChecker::isRegularMethodParameter)) {
                    errorReporter
                        .error("method javadoc contains parameter docs, which should be on the parameter itself");
                }
            }
        }
    }

    private static boolean isRegularMethodParameter(String line) {
        return PARAM_DOC_LINE.matcher(line).matches();
    }

    private static String getFirstWord(String str) {
        int i = str.indexOf(' ');
        return i != -1 ? str.substring(0, i) : str;
    }
}

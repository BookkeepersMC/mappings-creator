package mappings.plugin.decompile.javadoc;

public interface FieldJavadocProvider extends JavadocProvider {
    FieldJavadocProvider EMPTY = (fieldName, descriptor, owner) -> null;

    String provideFieldJavadoc(String fieldName, String descriptor, String owner);
}

package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jabref.model.util.OptionalUtil;

public class FieldFactory {

    /**
     * Character separating field names that are to be used in sequence as fallbacks for a single column
     * (e.g. "author/editor" to use editor where author is not set):
     */
    private static final String FIELD_OR_SEPARATOR = "/";
    private static final String DELIMITER = ";";

    public static String orFields(Field... fields) {
        return orFields(Arrays.asList(fields));
    }

    public static String orFields(Collection<Field> fields) {
        return fields.stream()
                     .map(Field::getName)
                     .collect(Collectors.joining(FIELD_OR_SEPARATOR));
    }

    public static List<Field> getNotTextFieldNames() {
        return Arrays.asList(StandardField.DOI, StandardField.FILE, StandardField.URL, StandardField.URI, StandardField.ISBN, StandardField.ISSN, StandardField.MONTH, StandardField.DATE, StandardField.YEAR);
    }

    public static List<Field> getIdentifierFieldNames() {
        return Arrays.asList(StandardField.DOI, StandardField.EPRINT, StandardField.PMID);
    }

    public static OrFields parseOrFields(String fieldNames) {
        Set<Field> fields = Arrays.stream(fieldNames.split(FieldFactory.FIELD_OR_SEPARATOR))
                     .map(FieldFactory::parseField)
                     .collect(Collectors.toSet());
        return new OrFields(fields);
    }

    public static Set<Field> parseFields(String fieldNames) {
        return Arrays.stream(fieldNames.split(FieldFactory.DELIMITER))
                     .map(FieldFactory::parseField)
                     .collect(Collectors.toSet());
    }

    public static String serializeFields(Collection<Field> fields) {
        return fields.stream()
                     .map(Field::getName)
                     .collect(Collectors.joining(DELIMITER));
    }

    public static Field parseField(String fieldName) {
        return OptionalUtil.orElse(OptionalUtil.orElse(OptionalUtil.<Field>orElse(
                InternalField.fromName(fieldName),
                StandardField.fromName(fieldName)),
                SpecialField.fromName(fieldName)),
                IEEEField.fromName(fieldName))
                           .orElse(new UnknownField(fieldName));
    }

    public static Set<Field> getKeyFields() {
        return getFieldsFiltered(field -> field.getProperties().contains(FieldProperty.SINGLE_ENTRY_LINK) || field.getProperties().contains(FieldProperty.MULTIPLE_ENTRY_LINK));
    }

    public static boolean isInternalField(Field field) {
        return field.getName().startsWith("__");
    }

    public static Set<Field> getJournalNameFields() {
        return getFieldsFiltered(field -> field.getProperties().contains(FieldProperty.JOURNAL_NAME));
    }

    /**
     * Returns a sorted List with all standard fields and including some common internal fields
     */
    public static Set<Field> getCommonFields() {
        TreeSet<Field> publicAndInternalFields = new TreeSet<>();
        publicAndInternalFields.add(InternalField.INTERNAL_ALL_FIELD);
        publicAndInternalFields.add(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD);
        publicAndInternalFields.add(InternalField.KEY_FIELD);
        publicAndInternalFields.addAll(EnumSet.allOf(StandardField.class));

        return publicAndInternalFields;
    }

    public static Set<Field> getBookNameFields() {
        return getFieldsFiltered(field -> field.getProperties().contains(FieldProperty.BOOK_NAME));
    }

    public static Set<Field> getPersonNameFields() {
        return getFieldsFiltered(field -> field.getProperties().contains(FieldProperty.PERSON_NAMES));
    }

    private static Set<Field> getFieldsFiltered(Predicate<Field> selector) {
        return getAllFields().stream()
                             .filter(selector)
                             .collect(Collectors.toSet());
    }

    private static Set<Field> getAllFields() {
        Set<Field> fields = new HashSet<>();
        fields.addAll(EnumSet.allOf(IEEEField.class));
        fields.addAll(EnumSet.allOf(InternalField.class));
        fields.addAll(EnumSet.allOf(SpecialField.class));
        fields.addAll(EnumSet.allOf(StandardField.class));
        return fields;
    }

    /**
     * These are the fields JabRef always displays as default {@link org.jabref.preferences.JabRefPreferences#setLanguageDependentDefaultValues()}
     *
     * A user can change them. The change is currently stored in the preferences only and not explicitly exposed as
     * separate preferences object
     */
    public static List<Field> getDefaultGeneralFields() {
        List<Field> defaultGeneralFields = Arrays.asList(StandardField.CROSSREF, StandardField.KEYWORDS, StandardField.FILE, InternalField.GROUPS, InternalField.OWNER, InternalField.TIMESTAMP);
        defaultGeneralFields.addAll(EnumSet.allOf(SpecialField.class));
        return defaultGeneralFields;
    }

    public static boolean isSingleLineField(final Field field) {
        return !field.getProperties().contains(FieldProperty.MULTILINE_TEXT);
    }
}
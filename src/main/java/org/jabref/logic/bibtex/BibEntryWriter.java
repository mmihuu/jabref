package org.jabref.logic.bibtex;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jabref.logic.TypedBibEntry;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.strings.StringUtil;

public class BibEntryWriter {

    private final LatexFieldFormatter fieldFormatter;
    private final BibEntryTypesManager entryTypesManager;

    public BibEntryWriter(LatexFieldFormatter fieldFormatter, BibEntryTypesManager entryTypesManager) {
        this.fieldFormatter = fieldFormatter;
        this.entryTypesManager = entryTypesManager;
    }

    public String serializeAll(List<BibEntry> entries, BibDatabaseMode databaseMode) throws IOException {
        StringWriter writer = new StringWriter();

        for (BibEntry entry : entries) {
            write(entry, writer, databaseMode);
        }
        return writer.toString();
    }

    public void write(BibEntry entry, Writer out, BibDatabaseMode bibDatabaseMode) throws IOException {
        write(entry, out, bibDatabaseMode, false);
    }

    /**
     * Writes the given BibEntry using the given writer
     *
     * @param entry           The entry to write
     * @param out             The writer to use
     * @param bibDatabaseMode The database mode (bibtex or biblatex)
     * @param reformat        Should the entry be in any case, even if no change occurred?
     */
    public void write(BibEntry entry, Writer out, BibDatabaseMode bibDatabaseMode, Boolean reformat) throws IOException {
        // if the entry has not been modified, write it as it was
        if (!reformat && !entry.hasChanged()) {
            out.write(entry.getParsedSerialization());
            return;
        }

        writeUserComments(entry, out);
        out.write(OS.NEWLINE);
        writeRequiredFieldsFirstRemainingFieldsSecond(entry, out, bibDatabaseMode);
        out.write(OS.NEWLINE);
    }

    private void writeUserComments(BibEntry entry, Writer out) throws IOException {
        String userComments = entry.getUserComments();

        if (!userComments.isEmpty()) {
            out.write(userComments + OS.NEWLINE);
        }
    }

    public void writeWithoutPrependedNewlines(BibEntry entry, Writer out, BibDatabaseMode bibDatabaseMode) throws IOException {
        // if the entry has not been modified, write it as it was
        if (!entry.hasChanged()) {
            out.write(entry.getParsedSerialization().trim());
            return;
        }

        writeRequiredFieldsFirstRemainingFieldsSecond(entry, out, bibDatabaseMode);
    }

    /**
     * Write fields in the order of requiredFields, optionalFields and other fields, but does not sort the fields.
     *
     * @param entry
     * @param out
     * @throws IOException
     */
    private void writeRequiredFieldsFirstRemainingFieldsSecond(BibEntry entry, Writer out,
                                                               BibDatabaseMode bibDatabaseMode) throws IOException {
        // Write header with type and bibtex-key.
        TypedBibEntry typedEntry = new TypedBibEntry(entry, bibDatabaseMode);
        out.write('@' + typedEntry.getTypeForDisplay() + '{');

        writeKeyField(entry, out);

        Set<Field> written = new HashSet<>();
        written.add(InternalField.KEY_FIELD);
        int indentation = getLengthOfLongestFieldName(entry);

        Optional<BibEntryType> type = entryTypesManager.enrich(entry.getType(), bibDatabaseMode);
        if (type.isPresent()) {
            // Write required fields first.
            for (OrFields value : type.get().getRequiredFields()) {
                writeField(entry, out, value.getPrimary(), indentation);
                written.add(value.getPrimary());
            }
            // Then optional fields.
            for (Field field : type.get().getOptionalFields()) {
                if (!written.contains(field)) { // If field appears both in req. and opt. don't repeat.
                    writeField(entry, out, field, indentation);
                    written.add(field);
                }
            }
        }
        // Then write remaining fields in alphabetic order.
        Set<Field> remainingFields = entry.getFields()
                                          .stream()
                                          .filter(key -> !written.contains(key))
                                          .collect(Collectors.toSet());

        for (Field field : remainingFields) {
            writeField(entry, out, field, indentation);
        }

        // Finally, end the entry.
        out.write('}');
    }

    private void writeKeyField(BibEntry entry, Writer out) throws IOException {
        String keyField = StringUtil.shaveString(entry.getCiteKeyOptional().orElse(""));
        out.write(keyField + ',' + OS.NEWLINE);
    }

    /**
     * Write a single field, if it has any content.
     *
     * @param entry the entry to write
     * @param out   the target of the write
     * @param name  The field name
     * @throws IOException In case of an IO error
     */
    private void writeField(BibEntry entry, Writer out, Field name, int indentation) throws IOException {
        Optional<String> field = entry.getField(name);
        // only write field if is is not empty
        // field.ifPresent does not work as an IOException may be thrown
        if (field.isPresent() && !field.get().trim().isEmpty()) {
            out.write("  " + getFormattedFieldName(name, indentation));

            try {
                out.write(fieldFormatter.format(field.get(), name));
                out.write(',' + OS.NEWLINE);
            } catch (InvalidFieldValueException ex) {
                throw new IOException("Error in field '" + name + "': " + ex.getMessage(), ex);
            }
        }
    }

    private int getLengthOfLongestFieldName(BibEntry entry) {
        Predicate<Field> isNotBibtexKey = field -> !InternalField.KEY_FIELD.equals(field);
        return entry.getFields()
                    .stream()
                    .filter(isNotBibtexKey)
                    .mapToInt(field -> field.getName().length())
                    .max()
                    .orElse(0);
    }

    /**
     * Get display version of a entry field.
     * <p>
     * BibTeX is case-insensitive therefore there is no difference between:
     * howpublished, HOWPUBLISHED, HowPublished, etc.
     * <p>
     * The was a long discussion about how JabRef should write the fields.
     * See https://github.com/JabRef/jabref/issues/116
     * <p>
     * The team decided to do the biblatex way and use lower case for the field names.
     *
     * @param field The name of the field.
     * @return The display version of the field name.
     */
    private String getFormattedFieldName(Field field, int intendation) {
        String fieldName = field.getName();
        return fieldName.toLowerCase(Locale.ROOT) + StringUtil.repeatSpaces(intendation - fieldName.length()) + " = ";
    }
}

package org.jabref.gui.entryeditor;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.field.InternalField;

public class RequiredFieldsTab extends FieldsEditorTab {
    public RequiredFieldsTab(BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders, UndoManager undoManager, DialogService dialogService) {
        super(false, databaseContext, suggestionProviders, undoManager, dialogService);

        setText(Localization.lang("Required fields"));
        setTooltip(new Tooltip(Localization.lang("Show required fields")));
        setGraphic(IconTheme.JabRefIcons.REQUIRED.getGraphicNode());
    }

    @Override
    protected Collection<String> determineFieldsToShow(BibEntry entry, EntryType entryType) {
        Set<String> fields = new LinkedHashSet<>();
        fields.addAll(entryType.getRequiredFieldsFlat());

        // Add the edit field for Bibtex-key.
        fields.add(InternalField.KEY_FIELD);

        return fields;
    }
}

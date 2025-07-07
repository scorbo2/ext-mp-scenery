package ca.corbett.musicplayer.extensions.scenery;

import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.Properties;
import ca.corbett.extras.properties.PropertiesDialog;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.ComboField;
import ca.corbett.forms.fields.FormField;
import ca.corbett.forms.fields.PanelField;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CompanionChooserProperty extends AbstractProperty {

    private static final Logger logger = Logger.getLogger(CompanionChooserProperty.class.getName());

    protected final List<Companion> companions;
    protected int selectedIndex;

    public CompanionChooserProperty(String name, String label, List<Companion> companions, int selectedIndex) {
        super(name, label);
        this.companions = companions;
        this.selectedIndex = selectedIndex;
    }

    public void setSelectedIndex(int index) {
        if (index < 0 || index >= companions.size()) {
            return;
        }
        selectedIndex = index;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedItem(String name) {
        for (int i = 0; i < companions.size(); i++) {
            if (companions.get(i).getName().equals(name)) {
                selectedIndex = i;
                break;
            }
        }
    }

    public int indexOf(String name) {
        for (int i = 0; i < companions.size(); i++) {
            if (companions.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public Companion getSelectedItem() {
        return companions.get(selectedIndex);
    }

    @Override
    public void saveToProps(Properties props) {
        props.setString(fullyQualifiedName, getSelectedItem().getName());
    }

    @Override
    public void loadFromProps(Properties props) {
        int index = indexOf(props.getString(fullyQualifiedName, getSelectedItem().getName()));
        if (index == -1) {
            selectedIndex = 0; // fallback default
        }
        else {
            selectedIndex = index;
        }
    }

    @Override
    public FormField generateFormField() {

        // This is a rough draft...
        // basically I'm limited by my own swing-extras library, specifically issue #44
        // there's currently no way to specify custom form logic for extension config properties.
        //
        // WHAT I WANT TO DO:
        //    Have a dropdown for selected Companion
        //    All the built-in ones and all the user-defined ones are shown in one flat list
        //    when you select one, the details panel underneath changes to show that Companion
        //    like, preview image, name, description, language, basic stats (count of responses/triggers, etc)
        //
        // WHAT I HAVE TO DO HERE:
        //    create a custom AbstractProperty implementation to wrap everything (combo + details panel)
        //    add custom form logic here in generateFormField to my combo field
        //    have a custom panel underneath the combo but within the same PanelField
        //    when the combo selection changes, manually update the details panel
        //
        // will this even work? I'm not sure... never tried this before with my own library, lol

        PanelField panelField = new PanelField();
        panelField.setIdentifier(fullyQualifiedName);
        JPanel panel = panelField.getPanel();
        panel.setLayout(new BorderLayout());

        FormPanel subForm = new FormPanel(FormPanel.Alignment.TOP_LEFT);
        List<String> items = new ArrayList<>();
        for (Companion companion : companions) {
            items.add(companion.getName());
        }
        ComboField field = new ComboField(propertyLabel, items, selectedIndex, false);
        field.setIdentifier(fullyQualifiedName + ".chooser");
        field.setEnabled(!isReadOnly);
        subForm.addFormField(field);

        // Great, now build out a custom JPanel with companion details:
        // TODO
        // And initialize these labels with values from whatever is currently selected (so it comes up first load looking correct)

        // Now add a value change listener on our chooser to update the details panel as it changes:
        field.addValueChangedAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO
            }
        });

        subForm.render();
        panel.add(subForm);
        return panelField;
    }

    @Override
    public void loadFromFormField(FormField field) {
        if (field.getIdentifier() == null
            || !field.getIdentifier().equals(fullyQualifiedName)
            || !(field instanceof PanelField)) {
            logger.log(Level.SEVERE, "CompanionChooserProperty.loadFromFormField: received the wrong field \"{0}\"",
                       field.getIdentifier());
            return;
        }

        // TODO how do I get the selected index from the ComboField embedded inside the PanelField?
        //      only approach I can think of is to interrogate the PanelField's panel's components until you find a JComboBox...
        //      which I guess would work since we know there will only be one JComboBox in that entire PanelField.
        //selectedIndex = ((ComboField)field).getSelectedIndex();
    }
}

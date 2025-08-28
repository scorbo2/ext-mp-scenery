package ca.corbett.musicplayer.extensions.scenery;

import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.Properties;
import ca.corbett.forms.fields.FormField;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This custom AbstractProperty wraps our Companion chooser into a neat little preview area
 * that will update to show an image of the selected Companion and also their basic stats.
 * There's some unpleasantness here in our generateFormField method to get around some
 * current limitations in swing-extras which I will fix at some point:
 * <A HREF="https://github.com/scorbo2/swing-extras/issues/44">Issue 44</A>
 */
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
        // Note that despite all the stuff we show in our generated form field,
        // the only thing we really care about is the currently selected Companion:
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
    protected FormField generateFormFieldImpl() {
        return new CompanionChooser(companions, selectedIndex);
    }

    @Override
    public void loadFromFormField(FormField field) {
        if (field.getIdentifier() == null
            || !field.getIdentifier().equals(fullyQualifiedName)
            || !(field instanceof CompanionChooser)) {
            logger.log(Level.SEVERE, "CompanionChooserProperty.loadFromFormField: received the wrong field \"{0}\"",
                       field.getIdentifier());
            return;
        }

        selectedIndex = ((CompanionChooser)field).getSelectedIndex();
    }
}

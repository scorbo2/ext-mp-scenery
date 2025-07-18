package ca.corbett.musicplayer.extensions.scenery;

import ca.corbett.extras.image.ImagePanel;
import ca.corbett.extras.image.ImagePanelConfig;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.Properties;
import ca.corbett.forms.fields.ComboField;
import ca.corbett.forms.fields.FormField;
import ca.corbett.forms.fields.PanelField;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
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

        // This is a bit ugly, but it works!
        // Basically I'm limited by my own swing-extras library, specifically issue #44.
        // There's currently no way to specify custom form logic for extension config properties.
        //
        // WHAT I WANT TO DO:
        //    Have a dropdown for selected Companion
        //    All the built-in ones and all the user-defined ones are shown in one flat list
        //    when you select one, the details panel underneath changes to show that Companion's details
        //    like: preview image, description, language, basic stats (count of responses/triggers, etc)
        //    But swing-extras doesn't currently allow custom form logic from the Property level...
        //
        // WHAT I HAVE TO DO HERE:
        //    create a custom AbstractProperty implementation to wrap everything (combo + details panel)
        //    add custom form logic here in generateFormField to my combo field
        //    have a custom panel underneath the combo but within the same PanelField
        //    when the combo selection changes, manually update the details panel
        //
        // Never tried this before with my own library, lol
        // But it works!

        PanelField panelField = new PanelField();
        panelField.setIdentifier(fullyQualifiedName);
        JPanel panel = panelField.getPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);

        List<String> items = new ArrayList<>();
        for (Companion companion : companions) {
            items.add(companion.getName());
        }
        ComboField field = new ComboField(propertyLabel, items, selectedIndex, false);
        field.setIdentifier(fullyQualifiedName + ".chooser");
        field.setEnabled(isInitiallyEditable);
        field.render(panel, gbc);
        gbc.gridx = 3;
        JLabel spacerLabel = new JLabel("");
        gbc.weightx = 1.0;
        panel.add(spacerLabel, gbc);

        Companion currentCompanion = getSelectedItem();

        gbc = new GridBagConstraints();
        gbc.gridy = 1;
        gbc.gridx = 1;
        gbc.gridheight = 5;
        gbc.anchor = GridBagConstraints.CENTER;
        final ImagePanel imagePanel = new ImagePanel(currentCompanion.getRandomImage(Color.BLACK), ImagePanelConfig.createSimpleReadOnlyProperties());
        imagePanel.setPreferredSize(new Dimension(125,125));
        panel.add(imagePanel, gbc);

        // I want a JTextArea for description instead of a simple JLabel because the description
        // might be a bit long and JTextArea handles line wrap much more gracefully than a JLabel:
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10,4,4,0);
        final JTextArea descArea = new JTextArea(currentCompanion.getDescription());
        descArea.setWrapStyleWord(true);
        descArea.setLineWrap(true);
        descArea.setEditable(false);
        descArea.setOpaque(false); // Makes background transparent
        descArea.setFocusable(false); // Prevents focus
        descArea.setBorder(null); // Removes border to look like a label
        panel.add(descArea, gbc);

        gbc.gridy++;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(16,4,2,0);
        final JLabel langLabel = new JLabel("Language: "+currentCompanion.getLanguage());
        langLabel.setFont(langLabel.getFont().deriveFont(Font.PLAIN));
        panel.add(langLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(4,4,2,0);
        final JLabel triggerCountLabel = new JLabel("Trigger count: "+currentCompanion.getTriggerCount());
        triggerCountLabel.setFont(triggerCountLabel.getFont().deriveFont(Font.PLAIN));
        panel.add(triggerCountLabel, gbc);

        gbc.gridy++;
        final JLabel totalResponseLabel = new JLabel("Total messages: "+currentCompanion.getTotalResponseCount());
        totalResponseLabel.setFont(totalResponseLabel.getFont().deriveFont(Font.PLAIN));
        panel.add(totalResponseLabel, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        final JLabel spacer = new JLabel("");
        panel.add(spacer, gbc);

        // Now add a value change listener on our chooser to update the details panel as it changes:
        field.addValueChangedAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Companion c = companions.get(field.getSelectedIndex());
                imagePanel.setImage(c.getRandomImage(Color.BLACK));
                descArea.setText(c.getDescription());
                langLabel.setText("Language: "+c.getLanguage());
                triggerCountLabel.setText("Trigger count: "+c.getTriggerCount());
                totalResponseLabel.setText("Total messages: "+c.getTotalResponseCount());
            }
        });

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

        // How do I get the selected index from the ComboField embedded inside the PanelField?
        // only approach I can think of is to interrogate the PanelField's panel's components until you find
        // a JComboBox... which I guess would work since we know there will only be one JComboBox in that
        // entire PanelField.
        PanelField panelField = (PanelField)field;
        for (Component c : panelField.getPanel().getComponents()) {
            if (c instanceof JComboBox) {
                selectedIndex = ((JComboBox)c).getSelectedIndex();
                return;
            }
        }
        logger.warning("CompanionChooserProperty.loadFromFormField: no chooser found!");
    }
}

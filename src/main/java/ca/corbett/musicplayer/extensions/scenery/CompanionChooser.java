package ca.corbett.musicplayer.extensions.scenery;

import ca.corbett.extras.image.ImagePanel;
import ca.corbett.extras.image.ImagePanelConfig;
import ca.corbett.extras.image.LogoFormField;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.ComboField;
import ca.corbett.forms.fields.FormField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.forms.fields.ValueChangedListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

public class CompanionChooser extends FormField {

    private final List<Companion> companions;
    private final FormPanel formPanel;
    private ComboField<Companion> companionCombo;
    private PanelField displayPanel;
    private boolean shouldExpand = true;

    public CompanionChooser(List<Companion> companions, int selectedIndex) {
        this.companions = new ArrayList<>(companions);
        formPanel = createFormPanel(selectedIndex);
        fieldComponent = formPanel;
    }

    @Override
    public boolean isMultiLine() {
        return true;
    }

    public CompanionChooser setShouldExpand(boolean should) {
        shouldExpand = should;
        return this;
    }

    @Override
    public boolean shouldExpand() {
        return shouldExpand;
    }

    /**
     * This field itself typically will not show a validation label, as we delegate field validation
     * to our embedded form fields. But, in keeping with the swing-forms general contract, you can
     * still assign FieldValidators to instances of this field if you wish.
     */
    @Override
    public boolean hasValidationLabel() {
        return !fieldValidators.isEmpty();
    }

    @Override
    public boolean validate() {
        // ask the parent to validate also, in case we have any FieldValidators assigned here
        return super.validate() && formPanel.isFormValid();
    }

    public Companion getSelectedCompanion() {
        return companionCombo.getSelectedItem();
    }

    public int getSelectedIndex() {
        return companionCombo.getSelectedIndex();
    }

    private FormPanel createFormPanel(int selectedIndex) {
        FormPanel formPanel = new FormPanel(Alignment.TOP_LEFT);

        companionCombo = new ComboField<>("Choose your tour guide:", companions, selectedIndex);
        formPanel.add(companionCombo);

        displayPanel = new PanelField(new GridBagLayout());
        displayPanel.setShouldExpand(true);
        JPanel panel = displayPanel.getPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);

        List<String> items = new ArrayList<>();
        for (Companion companion : companions) {
            items.add(companion.getName());
        }

        Companion currentCompanion = companions.get(selectedIndex);

        gbc = new GridBagConstraints();
        gbc.gridy = 1;
        gbc.gridx = 1;
        gbc.gridheight = 5;
        gbc.anchor = GridBagConstraints.CENTER;
        final ImagePanel imagePanel = new ImagePanel(currentCompanion.getRandomImage(Color.BLACK), ImagePanelConfig.createSimpleReadOnlyProperties());
        imagePanel.setPreferredSize(new Dimension(125, 125));
        panel.add(imagePanel, gbc);

        // I want a JTextArea for description instead of a simple JLabel because the description
        // might be a bit long and JTextArea handles line wrap much more gracefully than a JLabel:
        gbc.gridx = 2;
        gbc.gridheight = 1;
        gbc.weightx = 99;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
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
        //gbc.weightx = 0.0;
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
        companionCombo.addValueChangedListener(new ValueChangedListener() {
            @Override
            public void formFieldValueChanged(FormField field) {
                Companion c = companionCombo.getSelectedItem();
                imagePanel.setImage(c.getRandomImage(Color.BLACK));
                descArea.setText(c.getDescription());
                langLabel.setText("Language: "+c.getLanguage());
                triggerCountLabel.setText("Trigger count: "+c.getTriggerCount());
                totalResponseLabel.setText("Total messages: "+c.getTotalResponseCount());

            }
        });

        formPanel.add(displayPanel);
        return formPanel;
    }

}

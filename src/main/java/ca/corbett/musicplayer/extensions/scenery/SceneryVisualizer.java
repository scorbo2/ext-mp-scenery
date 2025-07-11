package ca.corbett.musicplayer.extensions.scenery;

import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
import ca.corbett.extras.properties.DecimalProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.FontProperty;
import ca.corbett.extras.properties.PropertiesManager;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.actions.ReloadUIAction;
import ca.corbett.musicplayer.ui.UIReloadable;
import ca.corbett.musicplayer.ui.VisualizationManager;
import ca.corbett.musicplayer.ui.VisualizationTrackInfo;
import ca.corbett.musicplayer.ui.VisualizationWindow;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;
import java.util.logging.Logger;

public class SceneryVisualizer extends VisualizationManager.Visualizer implements UIReloadable {

    private static final Logger log = Logger.getLogger(SceneryVisualizer.class.getName());

    public static final String NAME = "Scenery";

    /** A pixel margin to the left and right of the companion and text area **/
    private static final int MARGIN = 100;

    /** How long, in milliseconds, to leave a message up (TODO maybe it should be based on text length?) **/
    private static final int COMMENT_DISPLAY_TIME = 10000;

    private ImageScroller imageScroller;
    private AnimatedTextRenderer textRenderer;
    private ImageAnimator textAnimator;
    private ImageAnimator companionAnimator;

    // Companion props:
    private Companion companion;
    private boolean announceTrackChange;
    private boolean allowStyleOverride;
    private Font defaultFont;
    private Color defaultBg;
    private Color defaultFg;
    private SceneryExtension.CommentaryInterval commentaryInterval;
    private float textOpacity;

    private int displayWidth;
    private int displayHeight;
    private int textX;
    private int textWidth;

    // State vars:
    boolean isTrackAnnounced;
    long lastCommentTime;
    boolean isCommentingNow;
    String artist;
    String track;

    public SceneryVisualizer() {
        super(NAME);
    }

    @Override
    public void initialize(int width, int height) {
        displayWidth = width;
        displayHeight = height;
        artist = "N/A";
        track = "N/A";

        // Text width is the display width minus a margin on either side minus the width of the companion image:
        textWidth = displayWidth - SceneryExtension.IMAGE_MAX_DIM - (MARGIN*2);

        // Text left edge is just to the right of the companion image:
        textX = MARGIN + SceneryExtension.IMAGE_MAX_DIM;

        // Register for UI updates so we can reload our props if they change while we're running:
        ReloadUIAction.getInstance().registerReloadable(this);

        // And force a load now so we have the latest settings:
        reloadUI();

        // Background:
        imageScroller = new ImageScroller(SceneryExtension.sceneryLoader.loadRandom().getRandomImage(), width, height); // TODO we need a handle on the current SceneryImage to get its tags later

        // Text window:
        textRenderer = new AnimatedTextRenderer(textWidth, 400, "", 12);
        textAnimator = new ImageAnimator(textRenderer.getBuffer(), textX, displayHeight + 100, textX, displayHeight + 100, 777, 1.0, ImageAnimator.EasingType.EASE_IN_OUT, 0.2);
        textAnimator.setTransparency(textOpacity);

        // Companion:
        companionAnimator = new ImageAnimator(companion.getRandomImage(), -500, height - 500, -100, height - 500, 777, 1.0, ImageAnimator.EasingType.EASE_IN_OUT, 0.2);

        // This might be a bit rude, but let's switch the text overlay off if it's on.
        // (user can always hit "i" to bring it back; we won't muck with it again)
        if (VisualizationWindow.getInstance().isTextOverlayEnabled()) {
            VisualizationWindow.getInstance().toggleTextOverlayEnabled();
        }

        lastCommentTime = 0;
    }

    @Override
    public void renderFrame(Graphics2D g, VisualizationTrackInfo trackInfo) {

        // Look for track changes and make note of artist and track for text substitution:
        if (trackInfo != null) {
            if (! artist.equalsIgnoreCase(trackInfo.getArtist()) &&
                ! track.equalsIgnoreCase(trackInfo.getTitle())) {
                isTrackAnnounced = false;
            }
            artist = trackInfo.getArtist();
            track = trackInfo.getTitle();
        }

        // Render background scenery image
        imageScroller.renderFrame(g);

        // Announce the track if it hasn't been done yet:
        if (! isTrackAnnounced && ! isCommentingNow && trackInfo != null) {
            isCommentingNow = true;
            lastCommentTime = System.currentTimeMillis();
            companionAnimator.setImage(companion.getRandomImage());
            companionAnimator.setDestination(MARGIN, displayHeight - 500);
            isTrackAnnounced = true;

            String msg = companion.getRandomTrackChangeMessage();
            msg = msg.replaceAll("\\$\\{artist}", artist);
            msg = msg.replaceAll("\\$\\{track}", track);
            textRenderer.setText(msg);
            textAnimator.setDestination(textX, displayHeight - 500);
        }

        // Look for a conversation trigger:
        else if (! isCommentingNow && (System.currentTimeMillis() - lastCommentTime) > commentaryInterval.getIntervalMs()) {
            isCommentingNow = true;
            lastCommentTime = System.currentTimeMillis();
            companionAnimator.setImage(companion.getRandomImage());
            companionAnimator.setDestination(MARGIN, displayHeight - 500);

            String msg = companion.getResponse(artist, track, List.of()); // TODO scenery tags
            if (msg == null) {
                // TODO idle chitchat
                msg = "I have nothing to say...";
            }
            textRenderer.setText(msg);
            textAnimator.setDestination(textX, displayHeight - 500);
        }

        // Remove companion if a message has been up for a certain time:
        else if (isCommentingNow && (System.currentTimeMillis() - lastCommentTime) > COMMENT_DISPLAY_TIME) {
            companionAnimator.setDestination(-500, displayHeight - 500);
            textAnimator.setDestination(textX, displayHeight + 100);
            isCommentingNow = false;
        }

        companionAnimator.renderFrame(g);
        textRenderer.updateTextAnimation();
        textAnimator.setImage(textRenderer.getBuffer());
        textAnimator.renderFrame(g);
    }

    @Override
    public void stop() {
        imageScroller.stop();
        textRenderer.dispose();

        // Stop listening for UI updates now:
        ReloadUIAction.getInstance().unregisterReloadable(this);
    }

    @Override
    public void reloadUI() {
        PropertiesManager propsManager = AppConfig.getInstance().getPropertiesManager();
        AbstractProperty chooserProp = propsManager.getProperty("Scenery.Tour guide.chooser");
        AbstractProperty announceTrackChangeProp = propsManager.getProperty("Scenery.Tour guide.announceTrackChange");
        AbstractProperty commentaryIntervalProp = propsManager.getProperty("Scenery.Tour guide.interval");
        AbstractProperty styleOverrideProp = propsManager.getProperty("Scenery.Tour guide.allowStyleOverride");
        AbstractProperty defaultStyleProp = propsManager.getProperty("Scenery.Tour guide.defaultFont");
        AbstractProperty transparencyProp = propsManager.getProperty("Scenery.Tour guide.transparency");

        if (! (chooserProp instanceof CompanionChooserProperty) ||
            ! (announceTrackChangeProp instanceof BooleanProperty) ||
            ! (commentaryIntervalProp instanceof EnumProperty) ||
            ! (styleOverrideProp instanceof BooleanProperty) ||
            ! (defaultStyleProp instanceof FontProperty) ||
            ! (transparencyProp instanceof DecimalProperty)) {
            log.severe("SceneryVisualizer: our properties are of the wrong type!");
            return;
        }

        companion = ((CompanionChooserProperty)chooserProp).getSelectedItem();
        announceTrackChange = ((BooleanProperty)announceTrackChangeProp).getValue();
        allowStyleOverride = ((BooleanProperty)styleOverrideProp).getValue();
        defaultFont = ((FontProperty)defaultStyleProp).getFont();
        defaultBg = ((FontProperty)defaultStyleProp).getBgColor();
        defaultFg = ((FontProperty)defaultStyleProp).getTextColor();
        textOpacity = (float)((DecimalProperty)transparencyProp).getValue();

        // We can't use instanceof to pre-check these class casts because of type erasure, but eh, it'll be fine.
        //noinspection unchecked
        commentaryInterval = ((EnumProperty<SceneryExtension.CommentaryInterval>)commentaryIntervalProp).getSelectedItem();
    }
}

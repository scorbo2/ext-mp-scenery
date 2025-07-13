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
import java.util.logging.Logger;

public class SceneryVisualizer extends VisualizationManager.Visualizer implements UIReloadable {

    private static final Logger log = Logger.getLogger(SceneryVisualizer.class.getName());

    public static final String NAME = "Scenery";

    /** A pixel margin to the left and right of the companion and text area **/
    private static final int MARGIN = 100;

    /** How long, in milliseconds, to leave a message up **/
    private static final int BASE_COMMENT_TIME_MS = 8888;

    private ImageScroller imageScroller;
    private AnimatedTextRenderer textRenderer;
    private ImageAnimator textAnimator;
    private ImageAnimator companionAnimator;

    // Companion props:
    private Companion companion;
    private SceneryImage scenery;
    private boolean announceTrackChange;
    private boolean allowStyleOverride;
    private Font defaultFont;
    private Color defaultBg;
    private Color defaultFg;
    private SceneryExtension.CommentaryInterval commentaryInterval;
    private float textOpacity;

    private SceneryExtension.SceneryInterval sceneryInterval;

    private int displayWidth;
    private int displayHeight;
    private int textX;
    private int textWidth;

    // State vars:
    boolean isTrackAnnounced;
    long lastCommentTime;
    long lastSceneryChangeTime;
    boolean isCommentingNow;
    String artist;
    String track;
    Font effectiveFont;
    Color effectiveTextFg;
    Color effectiveTextBg;
    boolean isFirstFrame;

    public SceneryVisualizer() {
        super(NAME);
    }

    @Override
    public void initialize(int width, int height) {
        displayWidth = width;
        displayHeight = height;
        artist = "N/A";
        track = "N/A";
        isFirstFrame = true;

        // Text width is the display width minus a margin on either side minus the width of the companion image:
        textWidth = displayWidth - SceneryExtension.IMAGE_MAX_DIM - (MARGIN*2);

        // I want a slightly larger margin on the right side:
        textWidth -= 50;

        // Text left edge is just to the right of the companion image:
        textX = MARGIN + SceneryExtension.IMAGE_MAX_DIM;

        // Register for UI updates so we can reload our props if they change while we're running:
        ReloadUIAction.getInstance().registerReloadable(this);

        // And force a load now so we have the latest settings:
        reloadUI();

        // Background:
        scenery = SceneryExtension.sceneryLoader.loadRandom();
        imageScroller = new ImageScroller(scenery.getRandomImage(), width, height);

        // Text window:
        textRenderer = new AnimatedTextRenderer(textWidth, 350, "", 16, effectiveFont, effectiveTextFg, effectiveTextBg);
        textAnimator = new ImageAnimator(textRenderer.getBuffer(), textX, displayHeight + 100, textX, displayHeight + 100, 777, 1.0, ImageAnimator.EasingType.EASE_IN_OUT, 0.2);
        textAnimator.setTransparency(textOpacity);

        // Companion:
        companionAnimator = new ImageAnimator(companion.getRandomImage(effectiveTextBg), -500, height - 500, -100, height - 500, 777, 1.0, ImageAnimator.EasingType.EASE_IN_OUT, 0.2);

        // This might be a bit rude, but let's switch the text overlay off if it's on.
        // (user can always hit "i" to bring it back; we won't muck with it again)
        if (VisualizationWindow.getInstance().isTextOverlayEnabled()) {
            VisualizationWindow.getInstance().toggleTextOverlayEnabled();
        }

        lastCommentTime = 0; // force an immediate comment
        lastSceneryChangeTime = System.currentTimeMillis(); // but wait to swap scenery
    }

    @Override
    public void renderFrame(Graphics2D g, VisualizationTrackInfo trackInfo) {

        // Note how long it's been since we had a comment or a scenery change:
        long timeSinceLastMessage = System.currentTimeMillis() - lastCommentTime;
        long timeSinceLastSceneryChange = System.currentTimeMillis() - lastSceneryChangeTime;

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

        // Change the scenery if the track has changed and we are in SceneryInterval.TRACK:
        if (sceneryInterval == SceneryExtension.SceneryInterval.TRACK && ! isTrackAnnounced && ! isFirstFrame) {
            scenery = SceneryExtension.sceneryLoader.loadRandom();
            imageScroller.setImage(scenery.getRandomImage());
            lastSceneryChangeTime = System.currentTimeMillis();
        }

        // Announce the track if it hasn't been done yet:
        if (! isTrackAnnounced && ! isCommentingNow && trackInfo != null) {
            String msg = companion.getRandomTrackChangeMessage();
            msg = msg.replaceAll("\\$\\{artist}", artist);
            msg = msg.replaceAll("\\$\\{track}", track);

            isCommentingNow = true;
            lastCommentTime = System.currentTimeMillis() + msg.length() * 25L; // give time to read longer messages; 1 extra second per 40 chars or so
            companionAnimator.setImage(companion.getRandomImage(effectiveTextBg));
            companionAnimator.setDestination(textX - companionAnimator.getImage().getWidth(), displayHeight - 500);
            isTrackAnnounced = true;

            textRenderer.setText(msg);
            textAnimator.setDestination(textX, displayHeight - 450);
        }

        // Look for a conversation trigger:
        else if (! isCommentingNow && (System.currentTimeMillis() - lastCommentTime) > commentaryInterval.getIntervalMs()) {
            // Does the companion have something to say about this artist, track, or scenery image?
            String msg = companion.getResponse(artist, track, scenery.getTags());
            if (msg == null) {
                // If not, does the companion have any idle chitchat to offer?
                msg = companion.getRandomIdleChatter();
            }

            // If the companion had nothing to say, just skip (ideally this shouldn't happen... the companion needs more triggers!)
            if (msg != null && ! msg.isBlank()) {
                isCommentingNow = true;
                lastCommentTime = System.currentTimeMillis() + msg.length() * 25L; // give time to read longer messages; 1 extra second per 40 chars or so
                companionAnimator.setImage(companion.getRandomImage(effectiveTextBg));
                companionAnimator.setDestination(textX - companionAnimator.getImage().getWidth(), displayHeight - 500);

                textRenderer.setText(msg);
                textAnimator.setDestination(textX, displayHeight - 450);
            }
            else {
                log.info("SceneryVisualizer: the companion had no response and no chatter! (add more dialog to this companion to avoid this!)");
            }
        }

        // Remove companion if a message has been up for a certain time:
        else if (isCommentingNow && (System.currentTimeMillis() - lastCommentTime) > BASE_COMMENT_TIME_MS) {
            companionAnimator.setDestination(-500, displayHeight - 500);
            textAnimator.setDestination(textX, displayHeight + 100);
            isCommentingNow = false;
        }

        // Swap scenery if it's time:
        if (sceneryInterval != SceneryExtension.SceneryInterval.TRACK &&
            // TODO a transition might be nice instead of just a hard cut to the new one...
            (System.currentTimeMillis() - lastSceneryChangeTime) > sceneryInterval.getIntervalMs()) {
            scenery = SceneryExtension.sceneryLoader.loadRandom();
            imageScroller.setImage(scenery.getRandomImage());
            lastSceneryChangeTime = System.currentTimeMillis();
        }

        companionAnimator.renderFrame(g);
        textRenderer.updateTextAnimation();
        textAnimator.setImage(textRenderer.getBuffer());
        textAnimator.renderFrame(g);

        isFirstFrame = false; // this is just to prevent scenery swap on the very first render
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
        AbstractProperty sceneryIntervalProp = propsManager.getProperty("Scenery.Scenery.interval");

        if (! (chooserProp instanceof CompanionChooserProperty) ||
            ! (announceTrackChangeProp instanceof BooleanProperty) ||
            ! (commentaryIntervalProp instanceof EnumProperty) ||
            ! (styleOverrideProp instanceof BooleanProperty) ||
            ! (defaultStyleProp instanceof FontProperty) ||
            ! (transparencyProp instanceof DecimalProperty) ||
            ! (sceneryIntervalProp instanceof EnumProperty)) {
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

        Font font = defaultFont;
        log.info("defaultFont size is "+font.getSize());
        Color fg = defaultFg;
        Color bg = defaultBg;

        // If overrides are enabled and this companion supplies them, use them:
        if (allowStyleOverride && companion.getFont() != null) {
            font = companion.getFont();
            log.info("Overridden font size is "+font.getSize());
        }
        if (allowStyleOverride && companion.getTextColor() != null) {
            fg = companion.getTextColor();
        }
        if (allowStyleOverride && companion.getTextBgColor() != null) {
            bg = companion.getTextBgColor();
        }
        effectiveFont = font;
        effectiveTextFg = fg;
        effectiveTextBg = bg;

        // Update the text renderer if it has been created:
        if (textRenderer != null) {
            textRenderer.setFont(effectiveFont);
            textRenderer.setTextColor(effectiveTextFg);
            textRenderer.setBackgroundColor(effectiveTextBg);
        }

        // We can't use instanceof to pre-check these class casts because of type erasure, but eh, it'll be fine.
        //noinspection unchecked
        commentaryInterval = ((EnumProperty<SceneryExtension.CommentaryInterval>)commentaryIntervalProp).getSelectedItem();
        //noinspection unchecked
        sceneryInterval = ((EnumProperty<SceneryExtension.SceneryInterval>)sceneryIntervalProp).getSelectedItem();
    }
}

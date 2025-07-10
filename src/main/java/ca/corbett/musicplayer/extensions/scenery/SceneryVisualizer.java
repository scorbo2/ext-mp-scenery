package ca.corbett.musicplayer.extensions.scenery;

import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.BooleanProperty;
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
    private ImageScroller imageScroller;
    private AnimatedTextRenderer textRenderer;
    private ImageAnimator imageAnimator;

    // Companion props:
    private Companion companion;
    private boolean announceTrackChange;
    private boolean allowStyleOverride;
    private Font defaultFont;
    private Color defaultBg;
    private Color defaultFg;
    private SceneryExtension.CommentaryInterval commentaryInterval;

    private int displayWidth;
    private int displayHeight;

    // State vars:
    boolean isTrackAnnounced;
    long lastCommentTime;
    boolean isCommentingNow;

    public SceneryVisualizer() {
        super(NAME);
    }

    @Override
    public void initialize(int width, int height) {
        displayWidth = width;
        displayHeight = height;

        // Register for UI updates so we can reload our props if they change while we're running:
        ReloadUIAction.getInstance().registerReloadable(this);

        // And force a load now so we have the latest settings:
        reloadUI();

        imageScroller = new ImageScroller(SceneryExtension.sceneryLoader.loadRandom().getRandomImage(), width, height);

        // TODO load image, create ImageScroller and text animator
        textRenderer = new AnimatedTextRenderer(width - 200, 500, "Hello there! This is a test of the text animation code! Just ignore me for now, it will get a lot slicker soon!", 12);
        imageAnimator = new ImageAnimator(companion.getRandomImage(), -500, height - 500, -500, height - 500, 777, 1.0, ImageAnimator.EasingType.EASE_IN, 0.2);

        // This might be a bit rude, but let's switch the text overlay off if it's on.
        // (user can always hit "i" to bring it back; we won't muck with it again)
        if (VisualizationWindow.getInstance().isTextOverlayEnabled()) {
            VisualizationWindow.getInstance().toggleTextOverlayEnabled();
        }
    }

    @Override
    public void renderFrame(Graphics2D g, VisualizationTrackInfo trackInfo) {
        // Render background scenery image
        imageScroller.renderFrame(g);

        // TODO companion overlay

        // TODO companion text animation
        // TODO this should have a render() method like the animator and image scroller - standardize these!
        //textRenderer.updateTextAnimation();

        if (! isTrackAnnounced && ! isCommentingNow) {
            isCommentingNow = true;
            lastCommentTime = System.currentTimeMillis();
            imageAnimator.setDestination(100.0, displayHeight - 500);
            isTrackAnnounced = true;
        }

        // TODO companion movement:
        imageAnimator.renderFrame(g);

        // Step 4: Render the animated text
        //int leftEdge = 100; // 100px margin on either side
        //int topEdge = displayHeight - 500; // bottom - 400px text window height - 100px margin
        // TODO the text area should ideally be configurable instead of hard coded...
        //      although, really, how much config do we want to push on the user here?
        //      maybe it's more like "what you get is what you get, use it and like it"
        //      The alternative is to let the user configure it all, but it would get
        //      pretty complicated pretty quickly.
        //g.drawImage(textRenderer.getBuffer(), leftEdge, topEdge, null);

        // TODO at certain (configurable) intervals, we load or generate the next text string
        //      and give it to our textRenderer. The animation should restart.
        //if (textRenderer.isAnimationComplete()) {
            //textRenderer.setText("Hello again!");
        //}
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

        if (! (chooserProp instanceof CompanionChooserProperty) ||
            ! (announceTrackChangeProp instanceof BooleanProperty) ||
            ! (commentaryIntervalProp instanceof EnumProperty) ||
            ! (styleOverrideProp instanceof BooleanProperty) ||
            ! (defaultStyleProp instanceof FontProperty)) {
            log.severe("SceneryVisualizer: our properties are of the wrong type!");
            return;
        }

        companion = ((CompanionChooserProperty)chooserProp).getSelectedItem();
        announceTrackChange = ((BooleanProperty)announceTrackChangeProp).getValue();
        allowStyleOverride = ((BooleanProperty)styleOverrideProp).getValue();
        defaultFont = ((FontProperty)defaultStyleProp).getFont();
        defaultBg = ((FontProperty)defaultStyleProp).getBgColor();
        defaultFg = ((FontProperty)defaultStyleProp).getTextColor();

        // We can't use instanceof to pre-check these class casts because of type erasure, but eh, it'll be fine.
        //noinspection unchecked
        commentaryInterval = ((EnumProperty<SceneryExtension.CommentaryInterval>)commentaryIntervalProp).getSelectedItem();
    }
}

package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.animation.Pose;
import com.group_finity.mascot.exception.AnimationInstantiationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.hotspot.Hotspot;
import com.group_finity.mascot.image.ImagePairLoader;
import com.group_finity.mascot.image.ImagePairLoader.Filter;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.sound.SoundLoader;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class AnimationBuilder {
    private static final Logger log = Logger.getLogger(AnimationBuilder.class.getName());
    private final String condition;
    private String imageSet = "";
    private final List<Pose> poses = new ArrayList<>();
    private final List<Hotspot> hotspots = new ArrayList<>();
    private final ResourceBundle schema;
    private final String turn;

    public AnimationBuilder(final ResourceBundle schema, final Entry animationNode, final String imageSet) throws IOException {
        if (!imageSet.isEmpty()) {
            this.imageSet = imageSet;
        }

        this.schema = schema;
        condition = animationNode.getAttribute(schema.getString("Condition")) == null ? "true" : animationNode.getAttribute(schema.getString("Condition"));
        turn = animationNode.getAttribute(schema.getString("IsTurn")) == null ? "false" : animationNode.getAttribute(schema.getString("IsTurn"));

        log.log(Level.INFO, "Loading animations");

        for (final Entry frameNode : animationNode.selectChildren(schema.getString("Pose"))) {
            poses.add(loadPose(frameNode));
        }

        for (final Entry frameNode : animationNode.selectChildren(schema.getString("Hotspot"))) {
            hotspots.add(loadHotspot(frameNode));
        }

        log.log(Level.INFO, "Finished loading animations");
    }

    private Pose loadPose(final Entry frameNode) throws IOException {
        final String imageText = frameNode.getAttribute(schema.getString("Image")) != null ? Paths.get(imageSet, frameNode.getAttribute(schema.getString("Image"))).toString() : null;
        final String imageRightText = frameNode.getAttribute(schema.getString("ImageRight")) != null ? Paths.get(imageSet, frameNode.getAttribute(schema.getString("ImageRight"))).toString() : null;
        final String anchorText = frameNode.getAttribute(schema.getString("ImageAnchor"));
        final String moveText = frameNode.getAttribute(schema.getString("Velocity"));
        final String durationText = frameNode.getAttribute(schema.getString("Duration"));
        String soundText = frameNode.getAttribute(schema.getString("Sound"));
        final String volumeText = frameNode.getAttribute(schema.getString("Volume")) != null ? frameNode.getAttribute(schema.getString("Volume")) : "0";

        final double opacity = Double.parseDouble(Main.getInstance().getProperties().getProperty("Opacity", "1.0"));
        final double scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));

        String filterText = Main.getInstance().getProperties().getProperty("Filter", "false");
        Filter filter = Filter.NEAREST_NEIGHBOUR;
        if (filterText.equalsIgnoreCase("true") || filterText.equalsIgnoreCase("hqx")) {
            filter = ImagePairLoader.Filter.HQX;
        } else if (filterText.equalsIgnoreCase("bicubic")) {
            filter = ImagePairLoader.Filter.BICUBIC;
        }

        if (imageText != null) {
            try {
                final String[] anchorCoordinates = anchorText.split(",");
                final Point anchor = new Point(Integer.parseInt(anchorCoordinates[0]), Integer.parseInt(anchorCoordinates[1]));

                ImagePairLoader.load(imageText, imageRightText, anchor, scaling, filter, opacity);
            } catch (IOException | NumberFormatException e) {
                String error = imageText;
                if (imageRightText != null) {
                    error += ", " + imageRightText;
                }
                log.log(Level.SEVERE, "Failed to load image" + (imageRightText != null ? "s" : "") + ": " + error, e);
                throw new IOException(Main.getInstance().getLanguageBundle().getString("FailedLoadImageErrorMessage") + " " + error, e);
            }
        }

        final String[] moveCoordinates = moveText.split(",");
        final Point move = new Point((int) Math.round(Integer.parseInt(moveCoordinates[0]) * scaling),
                (int) Math.round(Integer.parseInt(moveCoordinates[1]) * scaling));

        final int duration = Integer.parseInt(durationText);

        if (soundText != null) {
            try {
                Path soundPath;
                if (Files.exists(Main.SOUND_DIRECTORY.resolve(soundText))) {
                    soundPath = Main.SOUND_DIRECTORY.resolve(soundText);
                } else if (Files.exists(Main.SOUND_DIRECTORY.resolve(imageSet).resolve(soundText))) {
                    soundPath = Main.SOUND_DIRECTORY.resolve(imageSet).resolve(soundText);
                } else {
                    soundPath = Main.IMAGE_DIRECTORY.resolve(imageSet).resolve(Main.SOUND_DIRECTORY).resolve(soundText);
                }
                soundText = soundPath.toString();

                float volume = Float.parseFloat(volumeText);
                SoundLoader.load(soundText, volume);
                soundText += volume;
            } catch (IOException | NumberFormatException | LineUnavailableException | UnsupportedAudioFileException e) {
                log.log(Level.SEVERE, "Failed to load sound: " + soundText, e);
                throw new IOException(Main.getInstance().getLanguageBundle().getString("FailedLoadSoundErrorMessage") + soundText, e);
            }
        }

        final Pose pose = new Pose(imageText, imageRightText, move.x, move.y, duration, soundText);

        log.log(Level.INFO, "Finished loading pose: {0}", pose);

        return pose;
    }

    private Hotspot loadHotspot(final Entry frameNode) throws IOException {
        final String shapeText = frameNode.getAttribute(schema.getString("Shape"));
        final String originText = frameNode.getAttribute(schema.getString("Origin"));
        final String sizeText = frameNode.getAttribute(schema.getString("Size"));
        final String behaviourText = frameNode.getAttribute(schema.getString("Behaviour"));
        final double scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));

        final String[] originCoordinates = originText.split(",");
        final String[] sizeCoordinates = sizeText.split(",");

        final Point origin = new Point((int) Math.round(Integer.parseInt(originCoordinates[0]) * scaling),
                (int) Math.round(Integer.parseInt(originCoordinates[1]) * scaling));
        final Dimension size = new Dimension((int) Math.round(Integer.parseInt(sizeCoordinates[0]) * scaling),
                (int) Math.round(Integer.parseInt(sizeCoordinates[1]) * scaling));

        Shape shape;
        if (shapeText.equalsIgnoreCase("Rectangle")) {
            shape = new Rectangle(origin, size);
        } else if (shapeText.equalsIgnoreCase("Ellipse")) {
            shape = new Ellipse2D.Float(origin.x, origin.y, size.width, size.height);
        } else {
            log.log(Level.SEVERE, "Failed to load hotspot shape: {0}", shapeText);
            throw new IOException(Main.getInstance().getLanguageBundle().getString("HotspotShapeNotSupportedErrorMessage") + " " + shapeText);
        }

        final Hotspot hotspot = new Hotspot(behaviourText, shape);

        log.log(Level.INFO, "Finished loading hotspot: {0}", hotspot);

        return hotspot;
    }

    public Animation buildAnimation() throws AnimationInstantiationException {
        try {
            return new Animation(Variable.parse(condition), poses.toArray(new Pose[0]), hotspots.toArray(new Hotspot[0]), Boolean.parseBoolean(turn));
        } catch (final VariableException e) {
            throw new AnimationInstantiationException(Main.getInstance().getLanguageBundle().getString("FailedConditionEvaluationErrorMessage"), e);
        }
    }
}

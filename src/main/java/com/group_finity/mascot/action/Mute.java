package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import com.group_finity.mascot.sound.Sounds;

import javax.sound.sampled.Clip;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * @author Kilkakon
 * @since 1.0.16
 */
public class Mute extends InstantAction {
    private static final Logger log = Logger.getLogger(Mute.class.getName());

    public static final String PARAMETER_SOUND = "Sound";

    private static final String DEFAULT_SOUND = null;

    public Mute(ResourceBundle schema, final VariableMap context) {
        super(schema, context);
    }

    @Override
    protected void apply() throws VariableException {
        String soundName = getSound();
        if (soundName != null) {
            List<Clip> clips = Sounds.getSoundsIgnoringVolume(Main.SOUND_DIRECTORY.resolve(soundName).toString());
            if (clips.isEmpty()) {
                clips = Sounds.getSoundsIgnoringVolume(Main.SOUND_DIRECTORY.resolve(getMascot().getImageSet()).resolve(soundName).toString());
                if (clips.isEmpty()) {
                    clips = Sounds.getSoundsIgnoringVolume(Main.IMAGE_DIRECTORY.resolve(getMascot().getImageSet()).resolve(Main.SOUND_DIRECTORY).resolve(soundName).toString());
                    for (Clip clip : clips) {
                        if (clip != null && clip.isRunning()) {
                            clip.stop();
                        }
                    }
                } else {
                    for (Clip clip : clips) {
                        if (clip != null && clip.isRunning()) {
                            clip.stop();
                        }
                    }
                }
            } else {
                for (Clip clip : clips) {
                    if (clip != null && clip.isRunning()) {
                        clip.stop();
                    }
                }
            }
        } else {
            if (!Sounds.isMuted()) {
                Sounds.setMuted(true);
                Sounds.setMuted(false);
            }
        }
    }

    private String getSound() throws VariableException {
        return eval(getSchema().getString(PARAMETER_SOUND), String.class, DEFAULT_SOUND);
    }
}
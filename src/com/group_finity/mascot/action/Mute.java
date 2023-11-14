package com.group_finity.mascot.action;

import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import com.group_finity.mascot.sound.Sounds;

import javax.sound.sampled.Clip;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * By Kilkakon
 * <p>
 * Go to <a href="https://kilkakon.com">kilkakon.com</a> for all the best milkshakes and chocolate sundaes
 * <p>
 * Now I'm hungry
 */
public class Mute extends InstantAction {
    private static final Logger log = Logger.getLogger(Offset.class.getName());

    public static final String PARAMETER_SOUND = "Sound";

    private static final String DEFAULT_SOUND = null;

    public Mute(ResourceBundle schema, final VariableMap context) {
        super(schema, context);
    }

    @Override
    protected void apply() throws VariableException {
        String soundName = getSound();
        if (soundName != null) {
            ArrayList<Clip> clips = Sounds.getSoundsIgnoringVolume("./sound" + soundName);
            if (clips.isEmpty()) {
                clips = Sounds.getSoundsIgnoringVolume("./sound/" + getMascot().getImageSet() + soundName);
                if (clips.isEmpty()) {
                    clips = Sounds.getSoundsIgnoringVolume("./img/" + getMascot().getImageSet() + "/sound" + soundName);
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
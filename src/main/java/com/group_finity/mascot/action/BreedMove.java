package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * @author Kilkakon
 * @since 1.0.18
 */
public class BreedMove extends Move {
    private static final Logger log = Logger.getLogger(BreedMove.class.getName());

    private final Breed.Delegate delegate = new Breed.Delegate(this);

    public BreedMove(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        delegate.initScaling();
        delegate.validateBornCount();
        delegate.validateBornInterval();
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();

        if (delegate.isIntervalFrame() && !isTurning() && delegate.isEnabled()) {
            // Multiply
            delegate.breed();
        }
    }
}

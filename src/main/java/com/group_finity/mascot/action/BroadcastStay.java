package com.group_finity.mascot.action;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Kilkakon
 * @since 1.0.14
 * @deprecated As of 1.0.21, broadcast functionality has been integrated into {@link ActionBase}.
 * Use {@link Stay} instead.
 */
@Deprecated(since = "1.0.21")
public class BroadcastStay extends Stay {
    private static final Logger log = LoggerFactory.getLogger(BroadcastStay.class);

    public BroadcastStay(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }
}

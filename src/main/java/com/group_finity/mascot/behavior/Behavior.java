package com.group_finity.mascot.behavior;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.CantBeAliveException;

import java.awt.event.MouseEvent;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public interface Behavior {
    void init(Mascot mascot) throws CantBeAliveException;

    void next() throws CantBeAliveException;

    void mousePressed(MouseEvent e) throws CantBeAliveException;

    void mouseReleased(MouseEvent e) throws CantBeAliveException;

    boolean isHidden();
}

package com.group_finity.mascot.behavior;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.action.ActionBase;
import com.group_finity.mascot.action.Dragged;
import com.group_finity.mascot.action.Regist;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.hotspot.Hotspot;

/**
 * Simple Sample Behavior.
 *
 * Original Author: Yuki Yamada of Group Finity
 * (http://www.group-finity.com/Shimeji/) Currently developed by Shimeji-ee
 * Group.
 */
public class UserBehavior implements Behavior
{
    private static final Logger log = Logger.getLogger( UserBehavior.class.getName() );

    public static final String BEHAVIOURNAME_FALL = "Fall";

    public static final String BEHAVIOURNAME_DRAGGED = "Dragged";

    public static final String BEHAVIOURNAME_THROWN = "Thrown";

    private final String name;

    private final Configuration configuration;

    private final Action action;

    private Mascot mascot;

    private boolean hidden;

    public UserBehavior( final String name, final Action action, final Configuration configuration, boolean hidden )
    {
        this.name = name;
        this.configuration = configuration;
        this.action = action;
        this.hidden = hidden;
    }

    @Override
    public String toString( )
    {
        return "Behavior(" + getName( ) + ")";
    }

    @Override
    public synchronized void init( final Mascot mascot ) throws CantBeAliveException
    {

        this.setMascot( mascot );

        log.log( Level.INFO, "Default Behavior({0},{1})", new Object[ ]
        {
             this.getMascot( ), this
        } );

        try
        {
            getAction( ).init( mascot );
            if( !getAction( ).hasNext( ) )
            {
                try
                {
                    mascot.setBehavior( this.getConfiguration( ).buildBehavior( getName(), mascot ) );
                }
                catch( final BehaviorInstantiationException e )
                {
                    throw new CantBeAliveException( Main.getInstance().getLanguageBundle().getString( "FailedInitialiseFollowingBehaviourErrorMessage" ), e );
                }
            }
        }
        catch( final VariableException e )
        {
            throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle().getString( "VariableEvaluationErrorMessage" ), e );
        }

    }

    private Configuration getConfiguration( )
    {
        return this.configuration;
    }

    private Action getAction( )
    {
        return this.action;
    }

    private String getName( )
    {
        return this.name;
    }

    /**
     * On Mouse Pressed. Start dragging.
     *
     * @ Throws CantBeAliveException
     */
    public synchronized void mousePressed( final MouseEvent event ) throws CantBeAliveException
    {
        if( SwingUtilities.isLeftMouseButton( event ) )
        {
            boolean handled = false;

            // check for hotspots
            if( !mascot.getHotspots( ).isEmpty( ) )
            {
                for( final Hotspot hotspot : mascot.getHotspots( ) )
                {
                    if( hotspot.contains( mascot, event.getPoint( ) ) )
                    {
                        // activate hotspot
                        handled = true;
                        try
                        {
                            getMascot( ).setCursorPosition( event.getPoint( ) );
                            getMascot( ).setBehavior( configuration.buildBehavior( hotspot.getBehaviour( ) ) );
                        }
                        catch( final BehaviorInstantiationException e )
                        {
                            throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getString( "FailedInitialiseFollowingBehaviourErrorMessage" ) + " " + hotspot.getBehaviour(), e );
                        }
                        break;
                    }
                }
            }

            // check if this action has dragging disabled
            if( !handled && action != null && action instanceof ActionBase )
            {
                try
                {
                    handled = !( (ActionBase)action ).isDraggable( );
                }
                catch( VariableException ex )
                {
                    throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getString( "FailedDragActionInitialiseErrorMessage" ), ex );
                }
            }

            if( !handled )
            {
                // Begin dragging
                try
                {
                    getMascot( ).setBehavior( configuration.buildBehavior( configuration.getSchema( ).getString( BEHAVIOURNAME_DRAGGED ) ) );
                }
                catch( final BehaviorInstantiationException e )
                {
                    throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getString( "FailedDragActionInitialiseErrorMessage" ), e );
                }
            }
        }
    }

    /**
     * On Mouse Release. End dragging.
     *
     * @ Throws CantBeAliveException
     */
    public synchronized void mouseReleased( final MouseEvent event ) throws CantBeAliveException
    {
        if( SwingUtilities.isLeftMouseButton( event ) )
        {
            if( getMascot( ).isHotspotClicked( ) )
                getMascot( ).setCursorPosition( null );

            // check if we are in the middle of a drag, otherwise we do nothing
            if( getMascot( ).isDragging( ) )
            {
                try
                {
                    getMascot( ).setDragging( false );
                    getMascot( ).setBehavior( configuration.buildBehavior( configuration.getSchema( ).getString( BEHAVIOURNAME_THROWN ) ) );
                }
                catch( final BehaviorInstantiationException e )
                {
                    throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getString( "FailedDropActionInitialiseErrorMessage" ), e );
                }
            }
        }
    }

    @Override
    public synchronized void next( ) throws CantBeAliveException
    {
        try
        {
            if( getAction( ).hasNext( ) )
            {
                getAction( ).next( );
            }

            boolean hotspotIsActive = false;
            if( getMascot( ).isHotspotClicked( ) )
            {
                // activate any hotspots that emerge while mouse is held down
                if( !mascot.getHotspots( ).isEmpty( ) )
                {
                    for( final Hotspot hotspot : mascot.getHotspots( ) )
                    {
                        if( hotspot.contains( mascot, mascot.getCursorPosition( ) ) )
                        {
                            // activate hotspot
                            hotspotIsActive = true;
                            try
                            {
                                // no need to set cursor position, it's already set
                                getMascot( ).setBehavior( configuration.buildBehavior( hotspot.getBehaviour( ) ) );
                            }
                            catch( final BehaviorInstantiationException e )
                            {
                                throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getString( "FailedInitialiseFollowingBehaviourErrorMessage" ) + " " + hotspot.getBehaviour(), e );
                            }
                            break;
                        }
                    }
                }

                if( !hotspotIsActive )
                {
                    getMascot( ).setCursorPosition( null );
                }
            }

            if( !hotspotIsActive )
            {
                if( getAction( ).hasNext( ) )
                {
                    if( ( getMascot( ).getBounds().getX() + getMascot().getBounds().getWidth()
                            <= getEnvironment().getScreen().getLeft() )
                            || ( getEnvironment().getScreen().getRight() <= getMascot().getBounds().getX() )
                            || ( getEnvironment().getScreen().getBottom() <= getMascot().getBounds().getY() ) )
                    {
                        log.log( Level.INFO, "Out of the screen bounds({0},{1})", new Object[]
                         {
                             getMascot( ), this
                        } );

                        if( Boolean.parseBoolean( Main.getInstance().getProperties().getProperty( "Multiscreen", "true" ) ) )
                        {
                            getMascot( ).setAnchor( new Point( (int)( Math.random() * ( getEnvironment().getScreen().getRight() - getEnvironment().getScreen().getLeft() ) ) + getEnvironment().getScreen().getLeft(),
                                                              getEnvironment().getScreen().getTop() - 256 ) );
                        }
                        else
                        {
                            getMascot( ).setAnchor( new Point( (int)( Math.random() * ( getEnvironment().getWorkArea().getRight() - getEnvironment().getWorkArea().getLeft() ) ) + getEnvironment().getWorkArea().getLeft(),
                                                              getEnvironment().getWorkArea().getTop() - 256 ) );
                        }

                        try
                        {
                            getMascot( ).setBehavior( this.getConfiguration().buildBehavior( configuration.getSchema().getString( BEHAVIOURNAME_FALL ) ) );
                        }
                        catch( final BehaviorInstantiationException e )
                        {
                            throw new CantBeAliveException( Main.getInstance().getLanguageBundle().getString( "FailedFallingActionInitialiseErrorMessage" ), e );
                        }
                    }

                }
                else
                {
                    log.log( Level.INFO, "Completed Behavior ({0},{1})", new Object[ ]
                     {
                         getMascot( ), this
                    } );

                    try
                    {
                        getMascot( ).setBehavior( this.getConfiguration().buildBehavior( getName( ), getMascot( ) ) );
                    }
                    catch( final BehaviorInstantiationException e )
                    {
                        throw new CantBeAliveException( Main.getInstance( ).getLanguageBundle( ).getString( "FailedInitialiseFollowingActionsErrorMessage" ), e );
                    }
                }
            }
        }
        catch( final LostGroundException e )
        {
            log.log( Level.INFO, "Lost Ground ({0},{1})", new Object[]
             {
                 getMascot( ), this
            } );

            try
            {
                getMascot( ).setCursorPosition( null );
                getMascot( ).setDragging( false );
                getMascot( ).setBehavior( configuration.buildBehavior( configuration.getSchema().getString( BEHAVIOURNAME_FALL ) ) );
            }
            catch( final BehaviorInstantiationException ex )
            {
                throw new CantBeAliveException( Main.getInstance().getLanguageBundle().getString( "FailedFallingActionInitialiseErrorMessage" ), e );
            }
        }
        catch( final VariableException e )
        {
            throw new CantBeAliveException( Main.getInstance().getLanguageBundle().getString( "VariableEvaluationErrorMessage" ), e );
        }
    }

    private void setMascot( final Mascot mascot )
    {
        this.mascot = mascot;
    }

    private Mascot getMascot( )
    {
        return this.mascot;
    }

    protected MascotEnvironment getEnvironment( )
    {
        return getMascot( ).getEnvironment( );
    }

    @Override
    public boolean isHidden( )
    {
        return hidden;
    }
}

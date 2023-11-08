package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import java.awt.Point;
import java.util.List;
import java.util.logging.Logger;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import java.util.logging.Level;

/**
 * @author Kilkakon
 */
public class BreedJump extends Jump
{
    private static final Logger log = Logger.getLogger( BreedJump.class.getName( ) );

    public static final String PARAMETER_BORNX = "BornX";

    private static final int DEFAULT_BORNX = 0;

    public static final String PARAMETER_BORNY = "BornY";

    private static final int DEFAULT_BORNY = 0;

    public static final String PARAMETER_BORNBEHAVIOUR = "BornBehaviour";

    private static final String DEFAULT_BORNBEHAVIOUR = "";

    public static final String PARAMETER_BORNMASCOT = "BornMascot";

    private static final String DEFAULT_BORNMASCOT = "";

    public static final String PARAMETER_BORNINTERVAL = "BornInterval";

    private static final int DEFAULT_BORNINTERVAL = 1;

    public static final String PARAMETER_BORNTRANSIENT = "BornTransient";

    private static final boolean DEFAULT_BORNTRANSIENT = false;
    
    private int scaling;

    public BreedJump( java.util.ResourceBundle schema, final List<Animation> animations, final VariableMap context )
    {
        super( schema, animations, context );
    }

    @Override
    public void init(final Mascot mascot) throws VariableException
    {
        super.init(mascot);

        if( getBornInterval( ) < 1 )
            throw new VariableException( "BornInterval must be positive" );
        scaling = Integer.parseInt( Main.getInstance( ).getProperties( ).getProperty( "Scaling", "1" ) );
    }

    @Override
    protected void tick( ) throws LostGroundException, VariableException
    {
        super.tick( );
        
        if( getTime() % getBornInterval( ) == 0 &&
            ( getBornTransient( ) ?
            Boolean.parseBoolean( Main.getInstance( ).getProperties( ).getProperty( "Transients", "true" ) ) :
            Boolean.parseBoolean( Main.getInstance( ).getProperties( ).getProperty( "Breeding", "true" ) ) ) )
        {
            breed( );
        }
    }
    
    private void breed( ) throws VariableException
    {
        String childType = Main.getInstance( ).getConfiguration( getBornMascot( ) ) != null ? getBornMascot( ) : getMascot( ).getImageSet( );
        
        final Mascot mascot = new Mascot( childType );

        log.log(Level.INFO, "Breed Mascot ({0},{1},{2})", new Object[] { getMascot(), this, mascot });

        if( getMascot().isLookRight( ) )
        {
            mascot.setAnchor( new Point( getMascot( ).getAnchor( ).x - ( getBornX( ) * scaling ),
                                         getMascot( ).getAnchor( ).y + ( getBornY( ) * scaling )));
        }
        else
        {
            mascot.setAnchor( new Point( getMascot( ).getAnchor( ).x + ( getBornX( ) * scaling ), 
                                         getMascot( ).getAnchor( ).y + ( getBornY( ) * scaling )));
        }
        mascot.setLookRight(getMascot().isLookRight());

        try {
                mascot.setBehavior( Main.getInstance( ).getConfiguration( childType ).buildBehavior( getBornBehaviour( ) ) );

                getMascot().getManager().add(mascot);

        } catch (final BehaviorInstantiationException e) {
                log.log(Level.SEVERE, "Fatal Exception", e);
                Main.showError( Main.getInstance( ).getLanguageBundle( ).getString( "FailedCreateNewShimejiErrorMessage" ) + "\n" + e.getMessage( ) + "\n" + Main.getInstance( ).getLanguageBundle( ).getString( "SeeLogForDetails" ) );
                mascot.dispose();
        } catch (final CantBeAliveException e) {
                log.log(Level.SEVERE, "Fatal Exception", e);
                Main.showError( Main.getInstance( ).getLanguageBundle( ).getString( "FailedCreateNewShimejiErrorMessage" ) + "\n" + e.getMessage( ) + "\n" + Main.getInstance( ).getLanguageBundle( ).getString( "SeeLogForDetails" ) );
                mascot.dispose();
        }
    }

    private int getBornX( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_BORNX ), Number.class, DEFAULT_BORNX ).intValue( );
    }

    private int getBornY( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_BORNY ), Number.class, DEFAULT_BORNY ).intValue( );
    }

    private String getBornBehaviour( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_BORNBEHAVIOUR ), String.class, DEFAULT_BORNBEHAVIOUR );
    }

    private String getBornMascot( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_BORNMASCOT ), String.class, DEFAULT_BORNMASCOT );
    }

    private boolean getBornTransient( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_BORNTRANSIENT ), Boolean.class, DEFAULT_BORNTRANSIENT );
    }

    private int getBornInterval( ) throws VariableException
    {
        return eval( getSchema( ).getString( PARAMETER_BORNINTERVAL ), Number.class, DEFAULT_BORNINTERVAL ).intValue( );
    }
}

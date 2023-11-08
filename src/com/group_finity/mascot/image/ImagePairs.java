package com.group_finity.mascot.image;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class ImagePairs {

	private static Hashtable<String,ImagePair> imagepairs = new Hashtable<String,ImagePair>(); 

	public static void load(final String filename, final ImagePair imagepair) {
		if( !imagepairs.containsKey( filename ) )
			imagepairs.put( filename, imagepair );
	}

	public static ImagePair getImagePair( String filename ) {
		if( !imagepairs.containsKey( filename ) )
			return null;
		ImagePair ip = imagepairs.get( filename );
		return ip;
	}	
	
	public static boolean contains( String filename ) {
		return imagepairs.containsKey( filename );
	}
        
        public static void clear( )
        {
            imagepairs.clear();
        }
        
        public static void removeAll( String searchTerm )
        {
            if( imagepairs.isEmpty( ) )
                return;
            
            for( Enumeration<String> key = imagepairs.keys( ); key.hasMoreElements( ); )
            {
                String filename = key.nextElement( );
                if( searchTerm.equals( filename.split( "/" )[ 1 ] ) )
                    imagepairs.remove( filename );
            }
        }
	
	public static MascotImage getImage( String filename, boolean isLookRight ) {
		if( !imagepairs.containsKey( filename ) )
			return null;
		return imagepairs.get( filename ).getImage( isLookRight );
	}
}
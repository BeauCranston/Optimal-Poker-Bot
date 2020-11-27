import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Stephen Adams
 *
 * Do not edit this source file.  Report all bugs to stephen.adams5@mohawkcollege.ca
 *
 */

public class callBot extends pokerPlayer {
    // Someone interested in "Hello" events
      
    public callBot() {
        super( "Uninitialized", 0 );
    }

    public callBot( String name, int chips ){
        super( name, chips );
    }

        @Override
    public void notification( String msg )    { 
        // Notifications from the dealer come through here.
        Pattern startPat = Pattern.compile( "^Starting hand \\d+, please ante up\\.$" );
        Pattern flopPat = Pattern.compile( "^.*Dealer shows (.)(.) (.)(.) (.)(.).*$", Pattern.DOTALL );
        Pattern riverTurnPat = Pattern.compile( "^.*Dealer shows (.)(.).*$", Pattern.DOTALL );
        Matcher matcher;
        
        // Sample response, you can also call your own processing
        if ( msg.equals( "Hello Players.  New Game Starting." )) {
            System.out.printf( "%s replies:%n\tHello dealer%n", name ); 
        } else if ( ( matcher = startPat.matcher(msg) ).matches() ) {
            this.holeCards.clear();
            this.tableCards.clear();
        } else if ( ( matcher = flopPat.matcher(msg) ).matches() || ( matcher = riverTurnPat.matcher(msg) ).matches() ) {
            String[] card = new String[2];
            for ( int i = 1; i <= matcher.groupCount(); i++ ) {
                card[ (i - 1) % 2] = matcher.group( i );
                if ( i % 2 == 0 ) {
                    tableCards.add( card );
                    card = new String[2];
                }
            }
        }
    }

    @Override
    public String chooseAction( List<String> actions ) {
        if ( actions.contains( "call" ) )
            return ( "call" );
        else {
            SecureRandom rnd = new SecureRandom();
            return actions.get( rnd.nextInt( actions.size() ) );
        }
    }

    @Override
    public int raiseAmount() {
        return 0;
    }
    
    @Override
    public int betAmount() {
        return 0;
    }
    
}

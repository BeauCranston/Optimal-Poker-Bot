import java.util.*;

/**
 *
 * @author Stephen Adams
 *
 * Do not edit this source file.  Report all bugs to stephen.adams5@mohawkcollege.ca
 * 
 */

public class pokerDealer {
    // See http://stackoverflow.com/a/6270150/759749 for more information
    private final List<DealerListener> listeners = new ArrayList<>();
    public void addListener(DealerListener toAdd) {listeners.add(toAdd);}
    public void announce( String msg ) {
        System.out.printf( "Dealer announces:\t%s%n", msg );

        // Notify everybody that may be interested.
        listeners.stream().forEach((hl) -> { hl.notification(msg); });
    }


    
    public String[][][] dealHole( Deck deck, List<pokerPlayer> players ) {
        String[][][] holeCards = new String[ players.size() ][2][2];
        
        // deal 2 cards to each player, 1 at a time
        for ( int i = 0; i < 2; i ++ ) {
            // deal first card to player who paid the big blind and rotate around the table
            int h = 0;
            for ( pokerPlayer player : players ) {
                String[] card = deck.dealCard();
                PokerTournament.debugWrite(String.format( "Dealing %s to %s.%n", card[deck.RANK]+card[deck.SUIT], player.name ));
                player.receiveCard ( card );
                holeCards[ h ][ i ] = card;
                h++;
            }
        }
        
        return holeCards;
    }
        
    public static double rankHand(String[][] hand) { return rankHand(hand, false); }
    
    public static double rankHand( String[][] hand, boolean debug ) {
        double rank = 0;
        String debugString;

        if ( hand.length == 5 ) {
            // check flush
            
            // note to check flush you could sort the array
            // by suit then check first == last but this
            // solution is O(n) so ...
            boolean isFlush = true;
            String initialSuit = hand[0][ Deck.SUIT ];
            
            for ( String[] card : hand ) {
                isFlush &= initialSuit.equals( card[ Deck.SUIT ] );
            }

            double[] ranks = new double[ hand.length ];
            int i = 0;
            for (  String[] card : hand) {
                switch ( card[ Deck.RANK ] ) {
                    case "A": ranks[i] = 14; break;
                    case "K": ranks[i] = 13; break;
                    case "Q": ranks[i] = 12; break;
                    case "J": ranks[i] = 11; break;
                    case "T": ranks[i] = 10; break;
                    default:
                        ranks[i] = Integer.parseInt( card[ Deck.RANK ] );
                }
                i++;
            }
            
            // Sort by rank to check straights easily
            java.util.Arrays.sort( ranks );
                       
            // check royal flush
            if ( isFlush && ranks[0] == 10 && ranks[1] == 11 && ranks[2] == 12 && 
                    ranks[3] == 13 && ranks[4] == 14 ) {
                rank = 9;
                debugString = "Hand Ranked: Royal Flush";
            } else {
                // check straight

                // normal run or ace low or ace high for straight
                boolean isStraight = ( ranks[ 0 ] == ranks[ 1 ] - 1 && ranks[ 0 ] == ranks[ 2 ] - 2 && 
                                       ranks[ 0 ] == ranks[ 3 ] - 3 && ranks[ 0 ] == ranks[ 4 ] - 4 ) 
                                     ||
                                     ( ranks[ 0 ] == 2 && ranks[ 1 ] == 3 && ranks[ 2 ] == 4 && ranks[ 3 ] == 5 && 
                                       ranks[ 4 ] == 14 ) 
                                     || 
                                     ( ranks[ 0 ] == 10 && ranks[ 1 ] == 11 && ranks[ 2 ] == 12 && ranks[ 3 ] == 13 && 
                                       ranks[ 4 ] == 14 );

                // check straight flush
                if ( isStraight && isFlush ) {
                    // ranks[4] is the high card due to the above sort
                    rank = 8 + ( ranks[ 4 ] / 100 );
                    debugString = "Hand Ranked: Straight Flush";
                } else if ( ( ranks[ 0 ] == ranks[ 3 ] ) || ( ranks[ 1 ] == ranks[ 4 ] ) ) {
                    // check 4 of a kind
                    // 1st 4 or last 4, only need to check the ends
                    // have to check here to ensure ranking order instead of below
                    rank = 7 + ( ranks[ 2 ] / 100 );
                    debugString = "Hand Ranked: 4 of a Kind";
                } else if ( ( ( ranks[ 0 ] == ranks[ 2 ] ) && ( ranks[ 3 ] == ranks[ 4 ] ) ) ||
                         ( ( ranks[ 0 ] == ranks[ 1 ] ) && ( ranks[ 2 ] == ranks[ 4 ] ) ) ) {
                    // check full house
                    // 1st 3 and last 2 pr first 2 and last 3
                    
                    if ( ranks[ 0 ] == ranks[ 2 ] ) {
                        // highest 3 of a kind wins, pair only matters if 3 of a kind is tied
                        rank = 6 + ( ranks[ 2 ] / 100 ) + ( ranks[ 3 ] /10000 );
                    } else {
                        rank = 6 + ( ranks[ 2 ] / 100 ) + ( ranks[ 0 ] /10000 );
                    }
                    debugString = "Hand Ranked: Full House";
                } else if ( isFlush ) {
                    rank = 5 + ( ranks[ 4 ] / 100 );
                    debugString = "Hand Ranked: Flush";
                } else if ( isStraight ) {
                    rank = 4 + ( ranks[ 4 ] / 100 );
                    debugString = "Hand Ranked: Straight";
                } else if ( ( ranks[ 0 ] == ranks[ 2 ] ) || ( ranks[ 1 ] == ranks[ 3 ] ) || ranks[ 2 ] == ranks[ 4 ] ) {
                    // 3 of a kind
                    // 012, 123, 234
                    
                    // note that card 2 is in every 3 of a kind
                    rank = 3 + ( ranks[ 2 ] / 100 );
                    debugString = "Hand Ranked: 3 of a Kind";
                } else {
                    int pairs = 0;
                    int highPair = -1;
                    int lowPair = -1;
                    
                    // not mutually exclusive
                    if ( ranks[ 3 ] == ranks[ 4 ] ) { pairs++; highPair = 4;}
                    if ( ranks[ 2 ] == ranks[ 3 ] ) { pairs++; if ( highPair == - 1 ) highPair = 3; else lowPair = 3;}
                    if ( ranks[ 1 ] == ranks[ 2 ] ) { pairs++; if ( highPair == - 1 ) highPair = 2; else lowPair = 2;}
                    if ( ranks[ 0 ] == ranks[ 1 ] ) { pairs++; if ( highPair == - 1 ) highPair = 1; else lowPair = 1;}
                    
                    rank += pairs;
                    switch ( pairs ) {
                        case 2 : debugString = "Hand Ranked: 2 Pair"; rank += ranks[ highPair ] / 100 + ranks[ lowPair ] / 10000; break;
                        case 1 : debugString = "Hand Ranked: Pair"; rank += ranks[ highPair ] / 100; break;
                        default: debugString = "Hand Ranked: High Card"; rank += ranks[4] / 100;
                    }
                }
            } // if royal
            debugString += String.format( " value = %f%n", rank );
            if (debug)
                PokerTournament.debugWrite( debugString );
        } // if 5 cards in hand
        
        return rank;
    }
}

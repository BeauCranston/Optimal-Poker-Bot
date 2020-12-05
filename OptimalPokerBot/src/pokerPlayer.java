import java.util.*;

/**
 *
 * @author Stephen Adams
 *
 * Do not edit this source file.  Report all bugs to stephen.adams5@mohawkcollege.ca
 *
 * Create a new file that implements this abstract class.  Add your player to the
 * PokerTournament.java file
 *
 */

// See http://stackoverflow.com/a/6270150/759749 for more information
// An interface to be implemented by everyone interested in "Hello" events
interface DealerListener {
    void notification( String message );
}

public abstract class pokerPlayer implements DealerListener {
    // Someone interested in "Hello" events
    public abstract void notification( String msg );
    
    protected int chipTotal;
    protected List<String[]> holeCards;
    protected List<String[]> tableCards;
    public String name;
    
    public abstract String chooseAction( List<String> actions );
    public abstract int betAmount();
    public abstract int raiseAmount();

    
    public pokerPlayer() {
        this( "Uninitialized", 0 );
    }

    public pokerPlayer( String name, int chips ){
        this.name = name;
        if ( chips < 0 )
            chips = 0;
        this.chipTotal = chips;
        this.holeCards = new ArrayList<>();
        this.tableCards = new ArrayList<>();
    }
    
    public int getChipTotal() { return chipTotal; }
    
    public void receiveCard( String[] card ) { holeCards.add(card); }
    
    public boolean bet( int amount ) {
        boolean validBet = false;
        if ( amount >= 0 && amount <= chipTotal ) {
            chipTotal -= amount;
            validBet = true;
        }       
        return validBet;
    }
    
    public void allIn() {
        bet( chipTotal );
    }
        
    public void getChips( int amount ) { chipTotal += amount; }
    
    public String[][] showHand() {
        String[][] cardsAvailable = new String[7][2];
        String[][] bestHand = new String[5][2];
        String[][] testHand = new String[5][2];
        double bestHandRank = 0.0;

        cardsAvailable[0] = (String[])(holeCards.toArray())[0];
        cardsAvailable[1] = (String[])(holeCards.toArray())[1];
        
        String[][] tc = tableCards.toArray(new String[0][0]);
        //System.out.println(tableCards.size());
        int a = 2;
        for ( String[] c : tc ) {
            cardsAvailable[a++] = c;
        }
        if (a<7) {
            // The player has not submitted enough cards
            bestHand = cardsAvailable;
        }
        else {
            for( int i = 0; i<cardsAvailable.length; i++ ) {
                testHand[0] = cardsAvailable[i];
                for ( int j = i+1; j < cardsAvailable.length; j++ ) {
                    testHand[1] = cardsAvailable[j];
                    for ( int k = j+1; k < cardsAvailable.length; k++ ) {
                        testHand[2] = cardsAvailable[k];
                        for ( int l = k+1; l < cardsAvailable.length; l++ ) {
                            testHand[3] = cardsAvailable[l];
                            for ( int m = l+1; m < cardsAvailable.length; m++ ) {
                                testHand[4] = cardsAvailable[m];
                                //System.out.println(Arrays.toString(testHand));
                                double testRank = pokerDealer.rankHand( testHand, false );
                                if ( testRank > bestHandRank ) {
                                    bestHandRank = testRank;
                                    bestHand = Arrays.copyOf( testHand, testHand.length );
                                }
                            }//m
                        }//l
                    }//k
                }//j
                // combinations.add( combo );
            }//i
        }        
        return bestHand;
    }
}

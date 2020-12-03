import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {

    public static void main(String[] args) {
        List<String[]> availableCards = new ArrayList<>(Arrays.asList(new String[]{"H", "4"}, new String[]{"C", "6"}, new String[]{"S", "9"}, new String[]{"H", "T"}, new String[]{"H", "3"}));

    }


    public String[][] getBestHandFromAvailable(List<String[]> availableCards){
        String[][] bestHand = new String[5][2];
        String[][] testHand = new  String[5][2];
        double bestHandRank = 0.0;
        if (availableCards.size() < 7) {
            // The player has not submitted enough cards
            bestHand = availableCards.toArray(new String[7][2]);
        }
        else {
            for( int i = 0; i<availableCards.size(); i++ ) {
                testHand[0] = availableCards.get(i);
                for ( int j = i+1; j < availableCards.size(); j++ ) {
                    testHand[1] = availableCards.get(j);
                    for ( int k = j+1; k < availableCards.size(); k++ ) {
                        testHand[2] = availableCards.get(k);
                        for ( int l = k+1; l < availableCards.size(); l++ ) {
                            testHand[3] = availableCards.get(l);
                            for (int m = l+1; m < availableCards.size(); m++ ) {
                                testHand[4] = availableCards.get(m);
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

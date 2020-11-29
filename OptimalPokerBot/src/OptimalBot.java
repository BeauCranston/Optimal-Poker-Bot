import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OptimalBot extends pokerPlayer {
    private int tablePosition;
    private double foldRate;
    private double callRate;
    private double raiseRate;
    private int handsPlayed;
    private int currentPot;
    private int currentBetAmount;
    private Queue<Opponent> opponents = new LinkedList<>();
    //order is important
    enum NotificationType{
        init,
        start,
        flop,
        river,
        potUpdate,
        betAmountUpdate,
        initOpponent,
        updateOpponentFoldRate,
        updateOpponentCallRate,
        updateOpponentRaiseRate,
        playerBusted,
        Invalid
    }
    Pattern initializePat = Pattern.compile("^Hello Players.  New Game Starting\\.$");
    Pattern startPat =  Pattern.compile( "^Starting hand (\\d+), please ante up\\.$" );
    Pattern flopPat = Pattern.compile( "^.*Dealer shows (.)(.) (.)(.) (.)(.).*$", Pattern.DOTALL );
    Pattern riverTurnPat = Pattern.compile( "^.*Dealer shows (.)(.).*$", Pattern.DOTALL );
    Pattern potUpdatePat = Pattern.compile("^.*As a result of betting, the pot is now (\\d)+\\.*$", Pattern.DOTALL);
    Pattern betAmountUpdatePat = Pattern.compile("^.*The bet is (\\d+) to (.*)\\.$");
    Pattern initOpponentPat = Pattern.compile("^.*Seated at this game are (.*,) (.*) and (.*).*$", Pattern.DOTALL);
    Pattern updateOpponentFoldRatePat = Pattern.compile("^(.*) has folded\\.$");
    Pattern updateOpponentCallRatePat = Pattern.compile("^(.*) calls 1000 \\.\\.\\. $");
    Pattern updateOpponentRaiseRatePat = Pattern.compile("^(.*) has called (\\d*) and raised by (\\d*)\\.$");
    Pattern playerBustedPat = Pattern.compile("^(.*) has busted at hand (\\d*) and must leave the table\\.$");


    ArrayList<Pattern> dealerAnnouncements = new ArrayList(Arrays.asList(initializePat, startPat, flopPat, riverTurnPat, potUpdatePat, betAmountUpdatePat, initOpponentPat, updateOpponentFoldRatePat, updateOpponentCallRatePat, updateOpponentRaiseRatePat, playerBustedPat));
    


    public OptimalBot() {
        super( "Uninitialized", 0 );
    }

    public OptimalBot( String name, int chips ){
        super( name, chips );
    }


    @Override
    public void notification(String msg) {
        System.out.println("BEAU SAYS: " + getNotificationType(msg));


    }

    @Override
    public String chooseAction(List<String> actions) {
        if ( actions.contains( "call" ) )
            return ( "call" );
        else {
            SecureRandom rnd = new SecureRandom();
            return actions.get( rnd.nextInt( actions.size() ) );
        }
    }

    @Override
    public int betAmount() {
        return 0;
    }

    @Override
    public int raiseAmount() {
        return 0;
    }

    public void getCurrentHandNumber(int handNum){
        handsPlayed = handNum;
    }
    public void initializeOpponent(String name){

    }
    public NotificationType getNotificationType(String msg){

        //length - 1 because i don't want to loop to the invalid state since it will be handled below
        for(int i = 0; i < NotificationType.values().length - 1; i++){
            if(msg.matches(dealerAnnouncements.get(i).pattern())){
                return NotificationType.values()[i];
            }
        }
        return NotificationType.Invalid;

    }


    private class Opponent{
        private int id;
        private String name;
        private double foldRate;
        private double callRate;
        private double raiseRate;
        private double bluffRate;

        private int tablePosition;

        public Opponent(String name){

        }

    }

}



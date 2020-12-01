import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OptimalBot extends pokerPlayer {
    private int tablePosition;
    private boolean debug = false;
    private double foldRate;
    private double callRate;
    private double raiseRate;
    private int handsPlayed;
    private int currentPot;
    private int currentBetAmount;
    private ArrayList<Opponent> opponents = new ArrayList<>();
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
    Pattern startPat =  Pattern.compile( "^.*Starting hand (.+), please ante up\\.$", Pattern.DOTALL );
    Pattern flopPat = Pattern.compile( "^.*Dealer shows (.)(.) (.)(.) (.)(.).*$", Pattern.DOTALL );
    Pattern riverTurnPat = Pattern.compile( "^.*Dealer shows (.)(.).*$", Pattern.DOTALL );
    Pattern potUpdatePat = Pattern.compile("^.*As a result of betting, the pot is now (\\d*)\\.*$", Pattern.DOTALL);
    Pattern betAmountUpdatePat = Pattern.compile("^.*The bet is (\\d+) to (.*)\\.$");
    Pattern initOpponentPat = Pattern.compile("^.*Seated at this game are (.*,) and (.*)\\.*$", Pattern.DOTALL);
    Pattern updateOpponentFoldRatePat = Pattern.compile("^(.*) has folded\\.$");
    Pattern updateOpponentCallRatePat = Pattern.compile("^(.*) callCount \\d* \\.\\.\\. $");
    Pattern updateOpponentRaiseRatePat = Pattern.compile("^(.*) has called (\\d*) and raised by (\\d*)\\.$");
    Pattern playerBustedPat = Pattern.compile("^(.*) has busted at hand (\\d*) and must leave the table\\.$");

    //order of this arraylist is also important since the riverTurn pattern technically matches the flop pattern but not vice versa
    ArrayList<Pattern> dealerAnnouncements = new ArrayList(Arrays.asList(initializePat, startPat, flopPat, riverTurnPat, potUpdatePat, betAmountUpdatePat, initOpponentPat, updateOpponentFoldRatePat, updateOpponentCallRatePat, updateOpponentRaiseRatePat, playerBustedPat));
    


    public OptimalBot() {
        super( "Uninitialized", 0 );
    }

    public OptimalBot( String name, int chips ){
        super( name, chips );
    }

    public void debugWrite(String msg){
        if(debug){
            System.out.println(msg);
        }

    }


    @Override
    public void notification(String msg) {
        processNotification(msg);
    }

    @Override
    public String chooseAction(List<String> actions) {
        String decision;
        if ( actions.contains( "call" ) && actions.contains( "fold" )){
            decision = determineCall();
            return decision;
        }
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

    /**
     * checks the message against the notification patterns and creates a match object to be used to get the data for the bot to read.
     *
     * configures the match object
     * @param msg
     */
    public void processNotification(String msg){
        //initialize matcher
        Matcher matcher;
        //length - 1 because i don't want to loop to the invalid state since it will be handled below
        for(int i = 0; i < NotificationType.values().length - 1; i++){
            //assign an instance to the match and call matches to check for the notifcation type as well as get the capture groups to pass as params
            if((matcher = dealerAnnouncements.get(i).matcher(msg)).matches()){
                RespondToAnnouncement(NotificationType.values()[i], matcher);
                break;
            }
        }

    }

    /**
     * calls the method of extracting data associated with the notification type
     * @param notificationType type of notification the message is
     * @param matcher holds the regex capture groups used to extract data
     */
    public void RespondToAnnouncement(NotificationType notificationType, Matcher matcher){

        switch(notificationType){
            case init:
                //says hello to the dealer
                System.out.printf( "%s replies:%n\tHello dealer%n", name );
                break;
            case start:
                //clears the hand
                clearHand(Integer.parseInt(matcher.group(1)));
                break;
            case flop:
            case river:
                getRiver(matcher);
                break;
            case potUpdate:
                updatePot(Integer.parseInt(matcher.group(1)));
                break;
            case betAmountUpdate:
                updateCurrentBet(Integer.parseInt(matcher.group(1)));
                break;
            case initOpponent:
                initOpponents(matcher);
                break;
            case updateOpponentFoldRate:
                updateOpponentAction(matcher.group(1),"f");
                break;
            case updateOpponentCallRate:
                updateOpponentAction(matcher.group(1),"c");
                break;

            case updateOpponentRaiseRate:
                updateOpponentAction(matcher.group(1),"r");
                break;
            case playerBusted:
                break;
            case Invalid:
                break;

        }
    }


    public void clearHand(int handNum){
        this.holeCards.clear();
        this.tableCards.clear();
        debugWrite("beau says hands played " + handNum);
    }

    public void getRiver(Matcher matcher){
        String[] card = new String[2];
        for ( int i = 1; i <= matcher.groupCount(); i++ ) {
            card[ (i - 1) % 2] = matcher.group( i );
            if ( i % 2 == 0 ) {
                tableCards.add( card );
                card = new String[2];
            }
        }
    }

    public void updatePot(int potUpdate){
        currentPot = potUpdate;
        debugWrite("Beau says CURRENT POT IS: " + currentPot);

    }
    public void updateCurrentBet(int betAmountUpdate){
        currentBetAmount = betAmountUpdate;
        debugWrite("Beau says CURRENT BET AMOUNT IS: " + currentBetAmount);

    }

    /**
     * initializes opponents by getting the names and using them to add Opponent instances to the opponents array list
     * @param matcher regex matcher from testing the notification
     */
    public void initOpponents(Matcher matcher){
        String group1 = new String();
        for(int i = 1; i <= matcher.groupCount(); i++){
                group1 = group1.concat(matcher.group(i).trim());
        }
        String[] OpponentNames = group1.split(",");
        for(String opponentNameRaw : OpponentNames){
            String opponentName = removeDot(opponentNameRaw.trim());
            debugWrite(opponentName);
            opponents.add(new Opponent(opponentName));
        }
        debugWrite("BEAU SAYS: " + group1);
    }

    public String removeDot(String msg){
        if(msg.endsWith(".")){
            msg = msg.substring(0,msg.length()-1);
        }
        return msg;
    }

    public void updateOpponentAction(String opponentName, String action){
        for(Opponent opponent : opponents){
            if(opponent.getName().equals(opponentName)){
                opponent.increment(action);
                break;
            }
        }
    }
    public String determineCall(){
        if(currentPot > 0 && currentBetAmount > 0){
            int potCheck = currentPot/currentBetAmount;
            if(potCheck > 3){
                return "call";
            }
            else{
                return "fold";
            }
        }
        else{
            return "call";
        }

    }

    private class Opponent{
        private int id;
        private String name;
        private int foldCount = 0;
        private int callCount = 0;
        private int raiseCount = 0;
        private int tablePosition;
        private String lastAction;


        public Opponent(String name){
            this.name = name;
        }

        public void increment(String action){
            lastAction = action;
            switch(action){
                case "f":
                    foldCount++;
                    debugWrite("Incremented " + getName() + "'s fold count to " + foldCount);
                    break;
                case "c":
                    callCount++;
                    debugWrite("Incremented " + getName() + "'s call count to " + callCount);
                    break;
                case "r":
                    raiseCount++;
                    debugWrite("Incremented " + getName() + "'s raise count to " + raiseCount);
                    callCount++;
                    debugWrite("Incremented " + getName() + "'s call count to " + callCount);
                    break;
                default:
                    debugWrite("invalid");
            }
        }
        public String getName(){
            return this.name;
        }
        public int getFoldCount(){
            return this.foldCount;
        }
        public int getCallCount(){
            return this.callCount;
        }
        public int getRaiseCount(){
            return this.raiseCount;
        }

        public double getRaiseRate(){
            double raiseRate = this.raiseCount / this.callCount;
            return raiseRate;
        }


    }

}



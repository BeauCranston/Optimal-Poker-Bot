import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * THIS CODE WAS DONE BY NONE OTHER THAN THE BIG BEAUSTER 000397019
 */
/*
BONUS MARKS :D :

The problem with the randBot and why it causes crashes is because it will not account for when the bot has 0 chips but
is still in the game so it will call rand.nextInt(0) which will produce an IllegalArgumentException.

 if(chipTotal > 0){
            return rnd.nextInt(chipTotal);
        }
        else{
            return 0;
        }

this is what i inserted into the randbot to make sure no errors are thrown
 */

public class OptimalBot extends pokerPlayer {
    private int tablePosition;
    private boolean debug = true;
    //default raise is just the call threshold * 1.5
    private double raiseThreshold = 0.14 * 1.5;
    //start with a call rate of 0.14 since that is the value of having a high card
    private double callThreshold = 0.14;
    private double allInThreshold = 0.8;
    private int handsPlayed;
    private ArrayList<String>playersAtTable = new ArrayList<>();
    private String[][] currentHand;
    private String[][] availableCards;
    private int currentPot;
    private int currentBetAmount;
    private boolean isAlive = false;

    //order is important
    enum NotificationType{
        init,
        initPlayers,
        start,
        flop,
        river,
        potUpdate,
        betAmountUpdate,
        playerBusted,
        Invalid
    }
    enum actions{
        check,
        call,
        bet,
        raise,
        allin,
        fold
    }
    Pattern initializePat = Pattern.compile("^Hello Players.  New Game Starting\\.$");
    Pattern initPlayersPat = Pattern.compile("^.*Seated at this game are (.*,) and (.*)\\.*$", Pattern.DOTALL);
    Pattern startPat =  Pattern.compile( "^.*Starting hand (.+), please ante up\\.$", Pattern.DOTALL );
    Pattern flopPat = Pattern.compile( "^.*Dealer shows (.)(.) (.)(.) (.)(.).*$", Pattern.DOTALL );
    Pattern riverTurnPat = Pattern.compile( "^.*Dealer shows (.)(.).*$", Pattern.DOTALL );
    Pattern potUpdatePat = Pattern.compile("^.*As a result of betting, the pot is now (\\d*)\\.*$", Pattern.DOTALL);
    Pattern betAmountUpdatePat = Pattern.compile("^.*The bet is (\\d+) to (.*)\\.$");
    Pattern playerBustedPat = Pattern.compile("^(.*) has busted at hand (\\d*) and must leave the table\\.$");

    //order of this arraylist is also important since the riverTurn pattern technically matches the flop pattern but not vice versa
    ArrayList<Pattern> dealerAnnouncements = new ArrayList(Arrays.asList(initializePat, initPlayersPat, startPat, flopPat, riverTurnPat, potUpdatePat, betAmountUpdatePat, playerBustedPat));

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
                NotificationType nt = NotificationType.values()[i];
                //check if the player is in the game or if the game is initializing. if none of these conditions are true then don't process the notification
                if(isAlive || nt.equals(NotificationType.init) || nt.equals(NotificationType.initPlayers) ){
                    RespondToAnnouncement(nt, matcher);
                }

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
            case initPlayers:
                initPlayers(matcher);
                break;
            case start:
                //clears the hand
                System.out.println("starting hand!!!!!!!!!!!!!!!!!!!!!!!");
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
            //if the bot busts don't process anymore notifications
            case playerBusted:
                if(matcher.group(1).equals(name)){
                    isAlive = false;
                }
                break;
            case Invalid:
                break;

        }
    }
    
    @Override
    public String chooseAction(List<String> actions) {
        int count = 0;
        for(String string : actions){

            System.out.println(actions.get(count));
            count++;
        }
        SetPlayerAggressiveness(3,3, 1.5);
        if(actions.contains("call") && actions.contains("raise")){
            if(actions.contains("fold")){
                //if fold is included
                return getOptimalAction(new String[]{"raise", "call"}, true);
            }
            else{
                //if fold is not included
                return getOptimalAction(new String[]{"raise", "call"}, false);
            }

        }
        //determine whether to muck or show
        else if(actions.contains("muck") && actions.contains("show")){
            double handRank = getHandRank(currentHand);
            //if the hand is good show it, otherwise don't show the hand
            if(handRank > 1 ){
                return "show";
            }
            else{
                return "muck";
            }
        }
        else{
            System.out.println("either unconsidered state, or invalid");
            return "fold";
        }

    }

    /**
     * finds the apporpriate action to take based on the curernt circumstances such as pot odds, position, hand confidence, and current bet.
     * @param actions the actions i can take
     * @param foldIncluded is fold included?
     * @return the action my bot will take
     */
    public String getOptimalAction(String[]actions, boolean foldIncluded){
        if(currentHand != null){
            double handConfidence = calculateHandConfidence();
            //if the hand is good enough to raise then it's also good enough to bet on
            if(handConfidence > raiseThreshold){
                return actions[0];
            }
            else if(foldIncluded && handConfidence < callThreshold){
                return "fold";
            }
            else{
                //good for when the hand is good enough to keep playing but better to get by without paying anything
                if(handConfidence > callThreshold && handConfidence < raiseThreshold){
                    return actions[1];
                }
                else{
                    return "fold";
                }
            }
        }
        //pre flop
        else{
            //i dont want to bet on preflop unless if the bet is relatively small
            if(currentBetAmount < 200){
                return actions[1];
            }
            else{
                return "fold";
            }
        }
    }

    /**
     * sets the bet amount based on if the hand confidence is > than certain thresholds
     * @return
     */
    @Override
    public int betAmount() {
        double handConfidence = calculateHandConfidence();
        //if the hand is good enough then raise
        if( handConfidence > raiseThreshold){
            return (int)((handConfidence) * (1000/2));
        }
        //if the hand is really good then go all in if its the last betting round
        else if(handConfidence > allInThreshold && availableCards.length == 7){
            return chipTotal;
        }
        //otherwise just check
        else{
            return 0;
        }

    }


    /**
     * sets the raise amount based on the hand confidence.
     * @return
     */
    @Override
    public int raiseAmount() {
        double handConfidence = calculateHandConfidence();
        int raise = (int)(1000 * handConfidence);
        //otherwise just raise by the raise amount
        return raise;
    }





    public void clearHand(int handNum){
        this.holeCards.clear();
        this.tableCards.clear();
        currentHand = null;
        //debugWrite("beau says hands played " + handNum);
    }

    // rank how good the hole cards are, i ended up not needing to use this
    public double rankHoleCards(List<String[]>holeCards){
        double holeRank = 0;
        double[] ranks = new double[ holeCards.size() ];
        String[] suits = new String[ holeCards.size() ];
        int i = 0;
        for (  String[] card : holeCards) {
            switch ( card[ Deck.RANK ] ) {
                case "A": ranks[i] = 14; break;
                case "K": ranks[i] = 13; break;
                case "Q": ranks[i] = 12; break;
                case "J": ranks[i] = 11; break;
                case "T": ranks[i] = 10; break;
                default:
                    ranks[i] = Integer.parseInt( card[ Deck.RANK ] );
            }
            switch (card[ Deck.SUIT]){
                case "D":
                case "H":
                case "S":
                case "C":
                    suits[i] = card[Deck.SUIT];
                    break;
                default:
                    System.out.println("Invalid suit on hole cards");

            }
            holeRank = holeRank + (ranks[i]/100);
            i++;
        }
        //add 1 if it is a pair
        if(ranks[0] == ranks[1]){
            holeRank = holeRank + 1;
        }
        //if the suits are the same then add 0.25 since the value of having 2 of the same suit is substantial but 0.25 keeps the hole cards in the same rank.
        // (hole rank can only exceed 1 if the ranks are the same)
        if(suits[0] == suits[1]){
            holeRank = holeRank + 0.25;
        }

        return holeRank;
    }

    /**
     * when a river card is placed, the bot will reassess its ahnd,
     * its hand rank, and the optimal hand rank that any player can have
     *
     * @param matcher
     */
    public void getRiver(Matcher matcher){
        String[] card = new String[2];
        for ( int i = 1; i <= matcher.groupCount(); i++ ) {
            card[ (i - 1) % 2] = matcher.group( i );
            if ( i % 2 == 0 ) {
                this.tableCards.add( card );
                card = new String[2];
            }
        }
        //when the river gets changed reset the value of the current hand and hand rank to account for the new card
        availableCards = getAvailableCards();
        //get best hands from the cards available to the bot
        currentHand = getHand(availableCards);
        double currentHandRank = getHandRank(currentHand);
        double bestHandRank = getHandRank(getBestHand());

        double hc = calculateHandConfidence();
        double ratio = currentHandRank/bestHandRank;
        double handImprovementProba = handImprovementProba();
        System.out.println("hand confidence: " + hc + " HIP: " + handImprovementProba + " ratio: " + ratio);
        System.out.println("hand rank: " + currentHandRank + " best rank: " + bestHandRank);
    }

    public void updatePot(int potUpdate){
        currentPot = potUpdate;
        //debugWrite("Beau says CURRENT POT IS: " + currentPot);

    }
    public void updateCurrentBet(int betAmountUpdate){
        currentBetAmount = betAmountUpdate;
        //debugWrite("Beau says CURRENT BET AMOUNT IS: " + currentBetAmount);

    }

    /**
     * gets the players aggressiveness which affects how high the threshold should be. The player aggressivness is based on the position the player is in
     * and how good the pot odds are.
     * @param positionWeight
     * @param potOddsWeight
     */
    public void SetPlayerAggressiveness(int positionWeight, int potOddsWeight, double raiseFactor) {
        //gets the position aggressiveness factor. We need to divide by 10 so that the number is not too large
        //the higher the number, the less aggressive the bot will be so if the bot is position 6/6 the factor will be 0.1 since (6/6)/10 = 0.1
        double positionalAggressivenessFactor = (((double) playersAtTable.size() / (double) tablePosition) / 10) / positionWeight;
        double potOddsAggressivenessFactor;
        if (currentPot > 0) {
            potOddsAggressivenessFactor = ((double) currentBetAmount / (double) currentPot) / potOddsWeight;
        } else {
            potOddsAggressivenessFactor = 1;
        }
        System.out.println(positionalAggressivenessFactor + potOddsAggressivenessFactor);
        callThreshold = (callThreshold + (positionalAggressivenessFactor * potOddsAggressivenessFactor));
        raiseThreshold = callThreshold * raiseFactor;
        System.out.println(callThreshold);
        System.out.println(raiseThreshold);
    }

    /**
     *loops through the players that have been seated at the game adding each player to the players at table arraylist.
     *
     * when the player's name is equal to my bot's name then my bot is considered "Alive".
     *
     * The players table position is also set here.
     * @param matcher regex matcher from testing the notification
     */
    public void initPlayers(Matcher matcher){
        String group1 = new String();
        for(int i = 1; i <= matcher.groupCount(); i++){
                group1 = group1.concat(matcher.group(i).trim());
        }
        String[] playerNames = group1.split(",");
        for(String playerNameRaw : playerNames){
            String playerName = removeDot(playerNameRaw.trim());
            debugWrite(playerName);
            if(playerName.equals(name)){
                isAlive = true;
            }
            playersAtTable.add(playerName);
        }
        //i want the table position to start at 1 so that i can properly divide by how many players are at the table to affect the player aggressiveness
        tablePosition = playersAtTable.indexOf(name) + 1;
        //debugWrite("BEAU SAYS: " + group1);
    }

    /**
     * removes a . from a string.
     * @param msg
     * @return
     */
    public String removeDot(String msg){
        if(msg.endsWith(".")){
            msg = msg.substring(0,msg.length()-1);
        }
        return msg;
    }


    /**
     * method to determine if the program should call, raise, etc
     * @return
     */
    public String determineCall(){
        return "";
    }

    /**
     * gets the hand rank if there are enough available cards for a full hand.
     *
     * The method will create the hand and then be tested with the pokerDealer.rankHand() method
     *
     * @return
     */
    public double getHandRank(String[][] cards){
        double handRank;
        //if the length is 5 we can assume it is a hand
        if(cards.length == 5 ){
            handRank = pokerDealer.rankHand(cards);
        }
        //otherwise it is a collection of cards that we need to get a hand out of before ranking it
        else if(cards.length == 6 || cards.length == 7){
            String[][]hand = getHand(cards);
            handRank = pokerDealer.rankHand(hand);
        }
        else{
            handRank = 0.0;
            debugWrite("invalid hand size");
        }

        return handRank;
    }

    /**
     * returns the bots hand. The bot ignores less optimal hands and returns the best hand the bot has from the available cards.
     *
     * @param availableCards river cards + hole cards. Range can be 5-7
     * @return
     */
    public static String[][] getHand(String[][] availableCards){
        String[][] bestHand = new String[5][2];
        String[][] testHand = new  String[5][2];
        double bestHandRank = 0.0;
        //get the best hand out of the available cards
        for( int i = 0; i < availableCards.length; i++ ) {
            testHand[0] = availableCards[i];
            for ( int j = i+1; j < availableCards.length; j++ ) {
                testHand[1] = availableCards[j];
                for ( int k = j+1; k < availableCards.length; k++ ) {
                    testHand[2] = availableCards[k];
                    for ( int l = k+1; l < availableCards.length; l++ ) {
                        testHand[3] = availableCards[l];
                        for (int m = l+1; m < availableCards.length; m++ ) {
                            testHand[4] = availableCards[m];
                            //System.out.println(Arrays.toString(testHand));
                            double testRank = pokerDealer.rankHand( testHand, false );
                            if ( testRank > bestHandRank ) {
                                bestHandRank = testRank;
                                bestHand = Arrays.copyOf( testHand, testHand.length );
                            }
                        }
                    }
                }
            }

        }

        return bestHand;

    }

    /**
     * calculates how good a hand is by getting the current hand rank, and the best rank that anyone could have given the table cards and divides them.
     *
     * @return a decimal that represents how good the hand is
     */
    public double calculateHandConfidence(){
        double handRank = getHandRank(currentHand.clone());
        double handRatio = handRank /getHandRank(getBestHand());
        double improvementProba = handImprovementProba();
        double handConfidence = handRatio + (improvementProba/3);
        return handConfidence;

    }
    /**
     * gets the best possible hand based on what is on the table. It uses the current hand as a starting point and then checks every possible hand that is not the current hand for
     * something better.
     * A hand can have any number of cards from 5 - 7 to account for the flop plus the states after each river card is added.
     * @return
     */
    public String[][] getBestHand(){
        //initialize an outer deck to handle the first "empty slot"
        Deck deck = new Deck();
        //initialize best hand to be the current hand
        String[][] bestHand = getHand(availableCards);
        //loop 52 - table cards
        for(int i = 0; i < (52 - tableCards.size()); i++){
            // create a temporary hand to check against the best hand
            List<String[]> tempHand = new ArrayList<>(tableCards);
            //loop until a card is found that is not in the table cards or the bots hole cards
            while(tempHand.size() < tableCards.size() + 1){
                String[] card = deck.dealCard();
                if(!containsCard(tableCards, card)){
                    tempHand.add(card);
                }
            }
            //an inner deck must be initialized to handle the second "empty slot".
            Deck innerDeck = new Deck();
            for(int j = 0; j < (52-tempHand.size()); j++){
                //create a new inner arraylist that is equal to the temp list so that the list is reset every iteration and there is not too many cards
                ArrayList<String[]>innerTempHand = new ArrayList<>(tempHand);
                //loop until a valid card is found
                while(innerTempHand.size() < tempHand.size() + 1){
                    String[] card = innerDeck.dealCard();
                    if(!containsCard(innerTempHand, card)){
                        innerTempHand.add(card);
                        String[][] hand = getHand(innerTempHand.toArray(new String[innerTempHand.size()][2]));
                        //get the rank of the tempHand
                        double handRank = getHandRank(hand);
                        //get the rank of the "best" hand
                        double bestRank = getHandRank(bestHand);
                        //if the rank of the tempHand is > than the rank of the bestHand then the bestHand becomes the value of the tempHand
                        if(handRank > bestRank){
                            bestHand = hand.clone();
                        }
                    }
                }

            }
        }

        //return the best hand
        return bestHand;
    }

    /**
     * finds out if the hand has the card already
     * @param hand
     * @param card
     * @return boolean
     */
    public boolean containsCard(List<String[]> hand, String[] card){
        boolean inHand = false;
        for(String[] handCard : hand){
            if(Arrays.equals(handCard, card)){
                inHand = true;
            }
        }
        return inHand;
    }
    /**
     * finds out if the hand has the card already
     * @param hand
     * @param card
     * @return boolean
     */
    public boolean containsCard(String[][]hand, String[] card){
        boolean inHand = false;
        for(String[] handCard : hand){
            if(Arrays.equals(handCard, card)){
                inHand = true;
            }
        }
        return inHand;
    }


    /**
     * returns the available cards. This includes the table cards, and the hole cards.
     * @return
     */
    public String[][] getAvailableCards(){
        List<String[]> availableCards = new ArrayList<>();
        if(holeCards.size() > 0){
            for(int i = 0; i < holeCards.size(); i++){
                availableCards.add(holeCards.get(i));
            }
        }

        if(tableCards.size() > 0){
            for(int i = 0; i < tableCards.size(); i++){
                availableCards.add(tableCards.get(i));

            }
        }
        return availableCards.toArray(new String[availableCards.size()][2]);
    }

    /**
     * the probability that a hand will improve given the current cards. This method allows the bot to think one step ahead
     * and check how likely it is to move up. This probability will impact whether it folds, calls, or raises.
     * If the river is full there is no chance to move up so 0 will be returned
     * @return
     */
    public double handImprovementProba(){
        Deck deck = new Deck();
        int improvementCount = 0;
        int totalCount = 0;
        //if there are still betting rounds ahead check the probaility to improve the hand
        if(availableCards.length < 7){
            //loop through a whole deck and try out every possible hand
            for(int i = 0; i < 52; i++){
                List<String[]> tempList = new ArrayList<>(Arrays.asList(availableCards.clone()));
                String[] card = deck.dealCard();
                if(!containsCard(tempList, card)){
                    tempList.add(card);
                    //if added increase the total count
                    totalCount++;
                    String[][] tempHand = getHand(tempList.toArray(new String[tempList.size()][2]));
                    //get the hand ranks to compare them
                    double tempHandRank = getHandRank(tempHand);
                    double currentHandRank = getHandRank(currentHand);
                    if(tempHandRank > currentHandRank){
                        //if the hand is improved increment imporvementCount
                        improvementCount++;
                    }
                }
            }
            //get the improvement proba an return it
            double improvementProbability = (double)improvementCount/(double)totalCount;
            return improvementProbability ;
        }
        else{
            return 0;
        }
    }
}



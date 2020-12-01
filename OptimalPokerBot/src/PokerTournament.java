/**
 * @author Stephen Adams
 * 
 * This is designed for use in COMP 10185 where the students will provide
 * a player bot to be imported to the tournament.
 * 
 * My role is that of dealer/house.
 * 
 * Rules from http://www.pokerlistings.com/poker-rules-texas-holdem
 *
 * Do not edit this source file except to add your player.
 * Report all bugs to stephen.adams5@mohawkcollege.ca
 *
 */

import java.util.*;

public class PokerTournament {
//    final static boolean debug = false;
    final static boolean debug = false;
    final static long version = 20203500;
    enum Phases { ANTE, BURN, HOLE, FLOP, TURN, RIVER, BET, REVEAL }
    public static double wins = 0;
    public static double runs = 0;
    public static String winner;
    public static void debugWrite( String str ) {
        if ( debug == true ) {
            System.out.print( str );

        }
    }
    static{
        for(int i = 0; i < 1000; i++){
            runs++;
            main(new String[]{"hello"});

        }

    }
    public static void main(String[] args) {
        pokerDealer dealer = new pokerDealer();
        List<pokerPlayer> players = new ArrayList<>();
        Phases gamePhase, prevPhase;
        String[][][] holeCards;// holeCards can be used to verify player.holeCards[][]
        String[][] flop; //for verificaiton
        String[] turn; // for verification
        String[] river; // for verification
        
        // tournament initial settings
        int bigBlindAmount = 100, smallBlindAmount = 50;
        int maxBet = 10*bigBlindAmount;
        int minBet = bigBlindAmount; // changes through the game play
        int handsPlayed = 0;
        final int stake = 10000;

        /**
         *  Add your player here, it must be in the same package.
         */

        players.add( new OptimalBot( "Your bot", stake ) );
        players.add( new randBot( "Randy McRandom", stake ) );
        players.add( new foldBot( "Fearless Folder", stake ) );
        players.add( new callBot( "Casual Caller", stake ) );
        players.add( new maxBetBot( "Maxine McMaxBet", stake ) );

        // subscribe players to the dealer's announcements
        players.stream().forEach((player) -> { dealer.addListener( player ); });

        // initialChips validates players aren't manipuating their totals
        int initialChips = 0;
        for ( pokerPlayer p : players )
            initialChips += p.getChipTotal();
        
        // randomize seating positions at table
        Collections.shuffle( players );


        /*
         *
            Start of game sequence
         *
         */
        
        dealer.announce( "Hello Players.  New Game Starting." );
        String message;
        message = String.format( "%s is %d.%n\t%s is %d.%n\t%s is %d.%n\t%s is %d.%n", "Large Blind", bigBlindAmount, "Small Blind", smallBlindAmount, "Min bet", minBet, "Max bet", maxBet);
        dealer.announce( message ); 
        
        message = "Seated at this game are ";
        
        for ( pokerPlayer player: players )
            message += player.name + ", ";
        
        message = message.substring( 0, message.length() - 2 ) + ".";
        message = message.substring( 0, message.lastIndexOf( ", " ) ) + ", and" + message.substring( message.lastIndexOf( ", " ) + 1 );
        dealer.announce( message );

        // The game ends when there are no more players, or when the big blind
        // amount is more than the available chips on the table.
        //
        // Rationale: forcing 2 bots to have a 1 hand showdown for all the chips
        // implies that the bots are mismatched, when in fact they are apparently
        // quite evenly matched.  I choose to record the draw.
        while ( players.size() > 1 && bigBlindAmount < initialChips ) {
            // Each hand begins with a fresh deck of cards.
            Deck deck = new Deck();
            deck.shuffle();
            
            //System.out.println("");
            dealer.announce( String.format( "Starting hand %d, please ante up.", handsPlayed++ ) );
            int pot = 0;
            // for side pots track how many chips the player has invested in the pot
            int[] invested = new int[ players.size() ];

            // double the blinds every 15 hands to push to completion
            if ( handsPlayed % 15 == 0 && handsPlayed != 0 ) {
                if ( bigBlindAmount < initialChips ) {
                    bigBlindAmount *= 2;
                    smallBlindAmount *= 2;
                    maxBet = 10*bigBlindAmount;
                    minBet = bigBlindAmount;
                    message = String.format( "%n%s is %d.%n\t%s is %d.%n\t%s is %d.%n\t%s is %d.%n", "Large Blind", bigBlindAmount, "Small Blind", smallBlindAmount, "Min bet", minBet, "Max bet", maxBet);
                    dealer.announce( message );
                }
            }
            
            // Start of hand players have no status/state
            String[] playerStatus = new String[ players.size() ];
            for ( int i = 0; i < playerStatus.length; i++ )
                playerStatus[ i ] = "";
            
            /***************************************/
            /*  Pay blinds                         */
            /***************************************/
            gamePhase = Phases.ANTE;

            // set the big and small blind bet amounts, if players cannot make the blinds they are all in, and a side pot will be created
            String[] b = { "big", "small" };
            for ( int i = 0; i < 2; i++ ) {
                int bid;
                if ( i == 0 ) bid = bigBlindAmount;
                else          bid = smallBlindAmount;
                
                if ( players.get( i ).bet( bid ) ) {
                    invested[i] = bid;
                    dealer.announce( players.get( i ).name + " has paid the "+b[i]+" blind." );
                } else {
                    playerStatus[ i ] = "AllIn";
                    dealer.announce( players.get( i ).name + " has failed to pay the "+b[i]+" blind and is all in." );
                    invested[i] = players.get( i ).getChipTotal();
                    players.get( i ).allIn();
                }
            }
            
            prevPhase = gamePhase;
            gamePhase = Phases.HOLE;
            int bidder = 0;
            
            boolean liveGame = true;
            boolean showdown = false;

            while ( liveGame && gamePhase != Phases.REVEAL ) {
                /***************************************/
                /*  Deal cards                         */
                /*  Make bets                          */
                /*  See if the pot is won              */
                /*  */
                /***************************************/
                switch ( gamePhase ) {
                    case HOLE: 
                        dealer.announce( "Dealing hole cards." );
                        holeCards = dealer.dealHole( deck, players );
                        prevPhase = gamePhase;
                        gamePhase = Phases.BET;
                        bidder = 1; // player who paid the small blind bets first
                        break;
                    case BURN:
                        // burn 1 card to the table
                        String[] burnCard = deck.dealCard();
                        dealer.announce( "Burning one card ... " );
                        debugWrite( String.format("The dealer discards %s%s.\n", burnCard[Deck.RANK], burnCard[Deck.SUIT] ) );
                        
                        switch ( prevPhase ) {
                            case HOLE: gamePhase = Phases.FLOP;     break;
                            case FLOP: gamePhase = Phases.RIVER;    break;
                            case RIVER:gamePhase = Phases.TURN;     break;
                            case TURN: gamePhase = Phases.REVEAL;   break;
                            default:
                                debugWrite( "Invalid state change after burning card detected." );
                        }
                        break;
                    case FLOP:
                        flop = new String[][]{deck.dealCard(), deck.dealCard(), deck.dealCard()};
                        dealer.announce( String.format( "%sDealer shows %s %s %s ", "Dealing the flop ... ", flop[0][0]+flop[0][1], flop[1][0]+flop[1][1], flop[2][0]+flop[2][1] ) );
                        bidder = 0;
                        prevPhase = gamePhase;
                        gamePhase = Phases.BET;
                        break;
                    case RIVER:
                        river = deck.dealCard();
                        dealer.announce( String.format( "%sDealer shows %s", "Dealing the river ... ", river[0]+river[1] ) );
                        bidder = 0;
                        prevPhase = gamePhase;
                        gamePhase = Phases.BET;
                        break;
                    case TURN:
                        turn = deck.dealCard();
                        dealer.announce( String.format( "%sDealer shows %s", "Dealing the turn ... ", turn[0]+turn[1] ) );
                        bidder = 0;
                        prevPhase = gamePhase;
                        gamePhase = Phases.BET;
                        break;
                    case BET:
                        if ( !showdown ) {
                            // depending on the phase and the status of other players
                            if ( prevPhase == Phases.HOLE )
                                bettingRound( players, bidder, dealer, playerStatus, invested, bigBlindAmount, minBet, maxBet );
                            else
                                bettingRound( players, bidder, dealer, playerStatus, invested, 0, minBet, maxBet );

                            pot = 0;
                            for ( int i : invested )
                               pot += i;
                            
                            // check if game still has > 1 player in it
                            liveGame = liveGameCheck( playerStatus );
                            if ( liveGame ) {
                                dealer.announce( String.format( "As a result of betting, the pot is now %d.", pot ) );

                                // check if players are in showdown mode
                                showdown = showdownCheck( playerStatus );
                                if ( showdown )
                                    showdownShow( dealer, players, playerStatus );
                            }
                        }

                        switch ( prevPhase ) {
                            case HOLE: gamePhase = Phases.BURN;     break;
                            case FLOP: gamePhase = Phases.BURN;     break;
                            case RIVER:gamePhase = Phases.BURN;     break;
                            case TURN: gamePhase = Phases.REVEAL; break;
                            default:
                                debugWrite( "Invalid state change after betting detected." );
                        }
                        break;
                }
            }  // liveGame && !Phases.REVEAL

            if ( liveGame ) {
                // To Do: Fix this: ->
                // show hands beginning with the player who bet on the river or with the
                // first player to the left of the dealer if no bet was made
                double highRank = -1;
                double handRank;
                List<Integer> winners = new ArrayList<>();
                for ( int i = 0; i < players.size(); i++ ) {
                    if ( playerStatus[ i ].equals( "Folded" ) )
                        continue;
                    
                    String[][] hand =  players.get(i).showHand();
                    handRank = pokerDealer.rankHand( hand );
                    
                    // as we go along in order, rank the hand if it is lower
                    // than the highest then we may either show or muck
                    List<String> actions = new ArrayList<>();
                    actions.add( "show" );
                    
                    // if a player has a losing hand compared to revealed hands,
                    // they may fold (muck) or show
                    if ( handRank < highRank ) {
                        actions.add( "muck" );
                    }                     
                    String action = players.get(i).chooseAction( actions );
                    
                    if ( action.equals( "muck" ) ) {
                        dealer.announce( players.get(i).name + " folds, mucking their hand.");
                        debugWrite( String.format( "They had %s %s %s %s %s.\n", 
                                  hand[0][0]+hand[0][1], hand[1][0]+hand[1][1], hand[2][0]+hand[2][1], 
                                  hand[3][0]+hand[3][1], hand[4][0]+hand[4][1] ) );
                    } else {
                        dealer.announce( players.get(i).name + 
                                String.format( " reveals their hand.\n\tThey had %s %s %s %s %s.", 
                                  hand[0][0]+hand[0][1], hand[1][0]+hand[1][1], hand[2][0]+hand[2][1], 
                                  hand[3][0]+hand[3][1], hand[4][0]+hand[4][1]
                                        ) );
                        if ( handRank == highRank) {
                            winners.add( i );
                        } else if ( handRank > highRank ) {
                            winners.clear();
                            winners.add( i );
                            highRank = handRank;
                        }
                    }
                    
                }

                // all players in winners have the same rank, evaluate which
                // is higher, if possible, eg AAxxx beats 33xxx
                
                
                for ( int i : winners ) {
                    players.get( i ).getChips( pot / winners.size() );
                    if ( winners.size() > 1 )
                        dealer.announce( String.format("%s splits the pot and wins %d chips.", players.get(i).name, pot/winners.size() ) );
                    else
                        dealer.announce( String.format("%s wins the hand and a pot of %d chips.", players.get(i).name, pot/winners.size() ) );
                }
                
            } else {
                // game is dead, find the surviving player
                for ( int i = 0; i < playerStatus.length; i ++ ) {
                    if ( !playerStatus[i].equals("Folded") ) {
                        // there is only one non-folded player
                        players.get( i ).getChips( pot );
                        dealer.announce( String.format("%s wins the hand and a pot of %d chips.", players.get(i).name, pot ) );
                        break;
                    }
                }
                    
            }

            // remove busted out players
            for ( Iterator<pokerPlayer> iterator = players.iterator(); iterator.hasNext(); ) {
                pokerPlayer p = iterator.next();
                if ( p.getChipTotal() <= 0 ) {
                    dealer.announce( String.format( "%s has busted at hand %d and must leave the table.", p.name, handsPlayed ) );
                    iterator.remove();
                }
            }
            
            // Debug code:
            int tableChips = 0;
            for ( pokerPlayer p : players )
                tableChips += p.getChipTotal();
            debugWrite( String.format("There are %d chips in play.%n", tableChips ) );
            
            // move the button and adjust blind payers
            // current big blind player becomes last, the small blind player becomes the big blind, etc
            Collections.rotate( players, -1);
        }

        if ( players.size() == 1 ){
            String name = players.get(0).name;
            dealer.announce(  name + " wins!" );
            winner = name;
            if(winner.equals("Your bot")){
                wins++;
            }
            double winPercentage = (wins/runs)*100;
            System.out.println("winPercentage equals: " + winPercentage + "%");
        }

        else {
            message = "Due to strategies used, the game is a draw between ";
            for ( int i = 0; i < players.size(); i++ ) {
                message += players.get( i ).name;
                if ( i + 2 < players.size() ) {
                    message += ", ";
                } else if ( i + 1 < players.size() ) {
                    message += " and ";
                }
            }
            message = message + ".";
            dealer.announce( message );

        }


    }

    public static int checkBet( pokerPlayer player, int betAmount, pokerDealer dealer, String playerStatus ) {
        if ( !player.bet(betAmount) ) {
            // player cannot cover bet
            player.bet( player.getChipTotal() );
            dealer.announce( String.format( "%s cannot cover %d and is all in, betting %d.", player.name, betAmount, player.getChipTotal() ) );
            playerStatus = "AllIn";                            
            betAmount = player.getChipTotal();
        }
        return betAmount;
    }

    public static int checkRaise( pokerPlayer player, int betAmount, pokerDealer dealer, String playerStatus ) {
        if ( !player.bet(betAmount) ) {
            // player cannot cover bet
            player.bet( player.getChipTotal() );
            playerStatus = "AllIn";                            
            betAmount = player.getChipTotal();
        }
        return betAmount;
    }
    
   /**
    * @param players
     * @param firstBidderIDX
    * @param dealer
    * @param playerStatus
     * @param ante value of the initial call, usually 0 except for big blinds
     * @param minBet
     * @param maxBet
    */ 
    public static void bettingRound( List<pokerPlayer> players, int firstBidderIDX, pokerDealer dealer, String[] playerStatus, int[] invested, int ante, int minBet, int maxBet ) {
        //oldBettingRound( players, firstBidderIDX, dealer, playerStatus, invested, ante, minBet, maxBet);
        //return;

        // An orbit is a cycling through the players one at a time in order until all players have called the
        // bet without a raise, or all but 1 player has folded.  For the purposes of this idea treat an all
        // in action as a call with or without a raise when it first occurs, and a check thereafter.

        // A betting round starts wtih the first bidder and orbits allowing players until complete.
        
        // To accomodate side pots we will have to look at this from the point of view of how much
        // a player has invested in the pot.

        boolean liveOrbit = true;
        int toCall = ante; // Each betting round starts at 0 chips except for the round that includes blinds.
                           // At the end toCall is the amount added to the pot by the bidding round.
                           // We do not need to know the global pot total, just this local value to control betting.
                           // To establish and understand side pots we need to maintain a global player investment
                           // total that IS affected by the betting round.
        boolean blindsPaid = false;
        
        dealer.announce( "Starting betting round.");
        List<String> availableActions;
        int[] bids ;
        if ( ante > 0 ) bids = invested;
        else bids = new int[players.size()];
        
        boolean[] hasRaised = new boolean[players.size()];
        
        
        while ( liveOrbit ) {
            availableActions = new ArrayList<>();
            
            // determine based on the state of betting what the player's options are
            for ( int i = 0; i < players.size(); i++ ) {
                // to account for offsets from the big blind bidder
                int playerIndex = (i + firstBidderIDX) % players.size();
                
                // once you fold or are all in there are no further actions available to you.
                if ( playerStatus[playerIndex].equals("Folded") || playerStatus[playerIndex].equals("AllIn") ) {
                    continue;
                }

                // if there is nothing to call you may bet or check if you have not previously called
                if ( toCall == 0 && !playerStatus[playerIndex].equals("Called") ) {
                    availableActions.add( "bet" );
                    availableActions.add( "check" );
                } else if ( ante != 0 && blindsPaid == false ) {
                // small blind must be paid, may not fold, may result in all in bet
                    availableActions.add( "call" );
                    availableActions.add( "raise"); // allow blind raise
                    blindsPaid = true;
                } else {
                // if you have previously called you must follow the bet action
                    availableActions.add( "call" );
                    
                    // disallow immediate reraise - eventually put in proprer raise logic
                    if ( !hasRaised[playerIndex] )
                        availableActions.add( "raise" );
                    
                    if ( toCall - bids[playerIndex] > 0 )
                        availableActions.add( "fold" );
                }
                
                if ( i == 0 && toCall == 0 ) {
                    dealer.announce( String.format( "%s is on the button and they have the first bet.", players.get( playerIndex).name));
                } else if ( toCall == 0 ) {
                    dealer.announce( String.format( "%s has the opportunity to open the betting." , players.get( playerIndex ).name) );
                }
                else {
                    dealer.announce( String.format("The bet is %d to %s.", toCall - bids[ playerIndex ], players.get( playerIndex ).name ) );
                }
                
                // once the available actions are determined retrieve the player's choice
                String playerChoice = players.get( playerIndex ).chooseAction(availableActions);
                
                // precision is expected, making an invalid action results in a force fold
                if ( !availableActions.contains( playerChoice ) )
                    playerChoice = "ffold";

                int betAmount;
                switch( playerChoice ) {
                    case "bet":
                        betAmount = players.get( playerIndex ).betAmount();

                        // To Do: Relax the rules to no limit texas hold 'em?
                        if ( betAmount < minBet )      betAmount = minBet;
                        else if ( betAmount > maxBet ) betAmount = maxBet;
                        
                        playerStatus[ playerIndex ] = "Called";
                        betAmount = checkBet( players.get( playerIndex ) , betAmount, dealer, playerStatus[playerIndex]  );

                        bids[ playerIndex ] += betAmount;
                        toCall = betAmount;
                        
                        break;
                    case "check":
                        dealer.announce( String.format( "%s has checked.", players.get( playerIndex ).name ) );
                        playerStatus[ playerIndex ] = "Called";
                        break;                        
                    case "call":
                        playerStatus[ playerIndex ] = "Called";
                        betAmount = toCall-bids[playerIndex];
                        dealer.announce( String.format( "%s calls %d ... ", players.get( playerIndex ).name, betAmount ) );
                        betAmount = checkBet( players.get( playerIndex ) , betAmount, dealer, playerStatus[playerIndex]  );
                        bids[ playerIndex ] += betAmount;
                        
                        break;
                    case "raise":
                        // First you must pay your amount to call, and then add additional to the pot.
                        int callAmount = toCall - bids[playerIndex];
                        if ( players.get(playerIndex).getChipTotal() <= callAmount ) {
                            // player has insufficient chips to raise, so this is really a call.
                            playerStatus[ playerIndex ] = "Called";
                            betAmount = toCall-bids[playerIndex];
                            dealer.announce( String.format( "%s calls %d ... ", players.get( playerIndex ).name, betAmount ) );
                            betAmount = checkBet( players.get( playerIndex ) , betAmount, dealer, playerStatus[playerIndex]  );
                            bids[ playerIndex ] += betAmount;
                        } else {
                            players.get(playerIndex).bet( callAmount );
                            bids[ playerIndex ] += callAmount;

                            playerStatus[ playerIndex ] = "Raised";                            
                            hasRaised[ playerIndex ] = true;
                            
                            int raiseAmount = players.get( playerIndex ).raiseAmount();

                            if ( raiseAmount < minBet )      raiseAmount = minBet;
                            else if ( raiseAmount > maxBet ) raiseAmount = maxBet;

                            raiseAmount = checkRaise( players.get( playerIndex ) , raiseAmount, dealer, playerStatus[playerIndex]  );
                            toCall += raiseAmount;
                            bids[ playerIndex ] += raiseAmount;
                            
                            if ( playerStatus[playerIndex].equals("AllIn") )
                                dealer.announce( String.format( "%s calls %d and raises all in, betting %d!", players.get( playerIndex ).name, callAmount, raiseAmount ) );
                            else
                                dealer.announce( String.format( "%s has called %d and raised by %d.", players.get( playerIndex ).name, callAmount, raiseAmount ) );

                            // a raise invalidates a previous player's call
                            for( int j = 0; j < players.size(); j++ ) {
                                if (playerStatus[j].equals("Called"))
                                        playerStatus[j] = "";
                            }
                        }
                        break;
                    case "ffold":
                        dealer.announce( String.format("%s has given the dealer an invalid choice and is being forced to leave the hand.", players.get( playerIndex).name) );
                    case "fold":
                        playerStatus[ playerIndex ] = "Folded";
                        dealer.announce( players.get( playerIndex ).name + " has folded.");
                        break;
                }
            }
            
            boolean allDoneBetting = true;
            for ( int i=0; i < players.size(); i++) {
                String s = playerStatus[i];
                allDoneBetting &= ( s.equals("Folded") || s.equals("Called") || s.equals("AllIn") );
            }

            liveOrbit = liveGameCheck( playerStatus ) && !showdownCheck( playerStatus ) && !allDoneBetting;

        }
        System.out.println("");
    }

    public static boolean liveGameCheck( String[] statuses ) {
        int livePlayers = 0;
        
        for ( String status : statuses ) {
            if ( !status.equals("Folded") )
                livePlayers++;
        }
        
        return livePlayers > 1;
    }

    public static boolean showdownCheck( String[] statuses ) {
        boolean showdown = true;
        
        for ( String status : statuses ) {
            showdown &= (status.equals( "Folded" ) || status.equals( "AllIn" ));
        }
        
        return showdown;
    }
    
    public static void showdownShow( pokerDealer dealer, List<pokerPlayer> players, String[] statuses ) {
        dealer.announce( "Showdown show, players reveal your hole cards.");
        for ( int i = 0; i < players.size(); i++ ) {
            if ( statuses[i].equals("AllIn") )
                dealer.announce( String.format( "%s shows %s and %s", 
                    players.get(i).name,
                    players.get(i).holeCards.get(0)[0] + players.get(i).holeCards.get(0)[1], 
                    players.get(i).holeCards.get(1)[0] + players.get(i).holeCards.get(1)[1] ) );
        }
    }
}

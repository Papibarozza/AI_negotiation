package ai2018.group39;

import java.util.Map;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.NoModel;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;

public class BS_fixed extends OfferingStrategy {
	private SortedOutcomeSpace outcomespace;
	private int counter;
	public BS_fixed(){}
	
	@SuppressWarnings("deprecation")
	@Override
	public void init(NegotiationSession negotiationSession, OpponentModel opponentModel, OMStrategy omStrategy,
			Map<String, Double> parameters) throws Exception {
		super.init(negotiationSession, parameters);
		this.opponentModel = opponentModel;
		this.omStrategy = omStrategy;
		this.endNegotiation = false;
		outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
		negotiationSession.setOutcomeSpace(outcomespace);
		counter=0;

	}
	
	@Override
	public BidDetails determineNextBid(){
			//we use this variable to count the bids made so far
			counter++;
			System.out.println("counter is= "+counter);
			
			//before the first 10 bids we don't have enough info about the opponent
			//moreover if we don't have an opponent, we can not use it
			if(counter<10 || opponentModel instanceof NoModel){
				return determineWithoutOpponent();
			}
			
			return determineWithOpponent();
	}
	
	@SuppressWarnings("deprecation")
	private BidDetails determineWithoutOpponent(){
		System.out.println("Determine with no opponent");
		
		//our previous bid
		BidDetails ourLast=negotiationSession.getOwnBidHistory().getLastBidDetails();
		
		//we decrement our utility because at the beginning no increments are possible
		double utility=ourLast.getMyUndiscountedUtil()-0.01;
		System.out.println("our new utility is "+utility);
		
		BidDetails newBid=negotiationSession.getOutcomeSpace().getBidNearUtility(utility);
		
		newBid.setTime(negotiationSession.getTime());
		System.out.print("Bid is "+newBid);
		return newBid;
		
	}
	
	@SuppressWarnings("deprecation")
	private BidDetails determineWithOpponent(){
		System.out.println("Determine with opponent");
		double utility=0;
	
		int bidsOppSize=negotiationSession.getOpponentBidHistory().size();
		
		//previous opponent's bid
		BidDetails previousBid=negotiationSession.getOpponentBidHistory().getHistory().get(bidsOppSize-2);
		
		//last opponent's bid
		BidDetails lastBid=negotiationSession.getOpponentBidHistory().getLastBidDetails();
		
		//our previous bid
		BidDetails ourLast=negotiationSession.getOwnBidHistory().getLastBidDetails();
		
		//my utility of last bid
		double lastUtilityOwn=lastBid.getMyUndiscountedUtil();
		System.out.println("my last utility is "+lastUtilityOwn);
		
		//opponent's utility last bid
		double lastUtilityOpp=opponentModel.getBidEvaluation(lastBid.getBid());
		System.out.println("his last utility is "+lastUtilityOpp);
		
		//my utility previous bid
		double previousUtilityOwn=previousBid.getMyUndiscountedUtil();
		System.out.println("my prev utility is "+previousUtilityOwn);
		
		//opponent's utility previous bid
		double previousUtilityOpp=opponentModel.getBidEvaluation(previousBid.getBid());	
		System.out.println("his prev utility is "+previousUtilityOpp);
		
		BidDetails newBid=null;
		System.out.println("lastOwn<=prevOwn is "+Double.compare(lastUtilityOwn,previousUtilityOwn));
		System.out.println("lastOpp>prevOpp is "+Double.compare(lastUtilityOpp,previousUtilityOpp));
		
		//selection of the move and the next bid
		//we "mirror" the strategy of our opponent
		
		if(Double.compare(lastUtilityOwn,previousUtilityOwn)<=0 && Double.compare(lastUtilityOpp, previousUtilityOpp)>0){
			System.out.println("Selfish");
			
			//opponent is selfish, we increment our utility w.r.t. our previous bid
			//since the increment is a constant equals to 0.05, we check that
			//our previous bid's utility is lower than 0.95
			if(Double.compare(ourLast.getMyUndiscountedUtil(), 0.95)<=0){
				utility=ourLast.getMyUndiscountedUtil() + 0.05;
			}
			else utility=ourLast.getMyUndiscountedUtil();
			System.out.println("our new utility is "+utility);
			
			//opponent new utility should be <= his utility of our previous bid
			//we try to generate a bid that respects this condition
			
			newBid=omStrategy.getBid(outcomespace, utility);
				
			int i=0;
			while(i<100){//to avoid cycling too much
				System.out.println("Selfish compute");
				double newUtilityOpp=opponentModel.getBidEvaluation(newBid.getBid());
				if(Double.compare(newUtilityOpp, opponentModel.getBidEvaluation(ourLast.getBid()))<=0){
					break;
				}
				newBid=omStrategy.getBid(outcomespace, utility);
				i++;
			}
			
			
			
		}
		else if(Double.compare(lastUtilityOwn, previousUtilityOwn)>=0 && Double.compare(lastUtilityOpp, previousUtilityOpp)<= 0){
			System.out.println("Concession");
			//concession or nice or silent move from opponent
			//we decrement a little bit our utility w.r.t. our previous bid
			utility=ourLast.getMyUndiscountedUtil()-0.01;
			System.out.println("our new utility is "+utility);
			
			//opponent new utility should be >= his utility of our previous bid +0.05
			//we try to generate a bid that respects this condition
			newBid=omStrategy.getBid(outcomespace, utility);
				
			int i=0;
			while(i<100){//to avoid cycling too much
				System.out.println("Concession compute");
				double newUtilityOpp=opponentModel.getBidEvaluation(newBid.getBid());
				if(Double.compare(newUtilityOpp, opponentModel.getBidEvaluation(ourLast.getBid())+0.05)>=0){
					break;
				}
				newBid=omStrategy.getBid(outcomespace, utility);
				i++;
			}
			
			
		}
		else if(Double.compare(lastUtilityOwn, previousUtilityOwn) < 0 && Double.compare(lastUtilityOpp, previousUtilityOpp) <= 0){
			System.out.println("Unfortunate");
			//unfortunate move from the opponent
			utility=ourLast.getMyUndiscountedUtil();
			System.out.println("our new utility is "+utility);
			
			//opponent new utility should be >= his utility of our previous bid
			//we try to generate a bid that respects this condition
			newBid=omStrategy.getBid(outcomespace, utility);
				
			int i=0;
			while(i<100){//to avoid cycling too much
				System.out.println("Unfortunate compute");
				double newUtilityOpp=opponentModel.getBidEvaluation(newBid.getBid());
				if(Double.compare(newUtilityOpp, opponentModel.getBidEvaluation(ourLast.getBid()))>0){
					break;
				}
				newBid=omStrategy.getBid(outcomespace, utility);
				i++;
			}
		}
		else if(Double.compare(lastUtilityOwn, previousUtilityOwn)> 0 && Double.compare(lastUtilityOpp, previousUtilityOpp) > 0){
			System.out.println("Fortunate");
			//fortunate move from opponent
			
			//we use the same increment of the selfish case
			if(Double.compare(ourLast.getMyUndiscountedUtil(), 0.95)<=0){
				utility=ourLast.getMyUndiscountedUtil() + 0.05;
			}
			else utility=ourLast.getMyUndiscountedUtil();
			System.out.println("our new utility is "+utility);
			
			//opponent new utility should be >= his utility of our previous bid +0.05
			//we try to generate a bid that respects this condition
			
			newBid=omStrategy.getBid(outcomespace, utility);
				
			int i=0;
			while(i<100){//to avoid cycling too much
				System.out.println("Fortunate compute");
				double newUtilityOpp=opponentModel.getBidEvaluation(newBid.getBid());
				if(Double.compare(newUtilityOpp, opponentModel.getBidEvaluation(ourLast.getBid())+0.05)>=0){
					break;
				}
				newBid=omStrategy.getBid(outcomespace, utility);
				i++;
					
			}
		}
		
		if(newBid==null) System.out.println("No if entered");
		newBid.setTime(negotiationSession.getTime());
		System.out.println("Bid is "+newBid);
		return newBid;

	}
	
	@SuppressWarnings("deprecation")
	@Override
	public BidDetails determineOpeningBid() {
		return negotiationSession.getMaxBidinDomain();
		
	}

	

	@Override
	public String getName() {
		return "BS_fixed";
	}

}

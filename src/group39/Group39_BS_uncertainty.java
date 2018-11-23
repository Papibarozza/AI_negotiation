package group39;

import java.util.Map;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.NoModel;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;

public class Group39_BS_uncertainty extends OfferingStrategy{

    private SortedOutcomeSpace outcomespace;
	private int counter;
	private AbstractUtilitySpace utilitySpace;
	private UserModel userModel;
	public Group39_BS_uncertainty() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void init(NegotiationSession negotiationSession, OpponentModel opponentModel, OMStrategy omStrategy,
			Map<String, Double> parameters) throws Exception {
		super.init(negotiationSession, parameters);
		this.opponentModel = opponentModel;
		
		this.userModel = negotiationSession.getUserModel();
		
		if (userModel != null) {
			Uncertainty_Helper helper = new Uncertainty_Helper(negotiationSession);
			this.utilitySpace = helper.estimateUtilitySpace();
		} else {
			this.utilitySpace = negotiationSession.getUtilitySpace();
		}
		
		this.omStrategy = omStrategy;
		this.endNegotiation = false;
		outcomespace = new SortedOutcomeSpace(this.utilitySpace);
		negotiationSession.setOutcomeSpace(outcomespace);
		counter=0;

	}
	
	@Override
	public BidDetails determineNextBid(){
			//we use this variable to count the bids made so far
			counter++;
			
			//before the first 10 bids we don't have enough info about the opponent
			//moreover if we don't have an opponent, we can not use it
			if(counter<10 || opponentModel instanceof NoModel){
				return determineWithoutOpponent();
			}
			
			return determineWithOpponent();
	}
	
	private BidDetails determineWithoutOpponent(){
		
		//our previous bid
		BidDetails ourLast=negotiationSession.getOwnBidHistory().getLastBidDetails();
		
		//we decrement our utility because at the beginning no increments are possible
		double utility=ourLast.getMyUndiscountedUtil()-0.04;
		
		BidDetails newBid=negotiationSession.getOutcomeSpace().getBidNearUtility(utility);
		
		newBid.setTime(negotiationSession.getTime());
		
		return newBid;
		
	}
	
	
	private BidDetails determineWithOpponent(){
		
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
		
		//opponent's utility last bid
		double lastUtilityOpp=opponentModel.getBidEvaluation(lastBid.getBid());
		
		//my utility previous bid
		double previousUtilityOwn=previousBid.getMyUndiscountedUtil();
		
		//opponent's utility previous bid
		double previousUtilityOpp=opponentModel.getBidEvaluation(previousBid.getBid());	
				
		BidDetails newBid=null;
		
		//selection of the move and the next bid
		//we "mirror" the strategy of our opponent
		
		if(Double.compare(lastUtilityOwn,previousUtilityOwn)<=0 && Double.compare(lastUtilityOpp, previousUtilityOpp)>0){
			
			//opponent is selfish, we increment our utility w.r.t. our previous bid
			//since the increment is a constant equals to 0.05, we check that
			//our previous bid's utility is lower than 0.95
			if(Double.compare(ourLast.getMyUndiscountedUtil(), 0.95)<=0){
				utility=ourLast.getMyUndiscountedUtil() + 0.05;
			}
			else utility=ourLast.getMyUndiscountedUtil();
			
			//opponent new utility should be <= his utility of our previous bid
			//we try to generate a bid that respects this condition
			
			newBid=omStrategy.getBid(outcomespace, utility);
				
			int i=0;
			while(i<100){//to avoid cycling too much
				
				double newUtilityOpp=opponentModel.getBidEvaluation(newBid.getBid());
				if(Double.compare(newUtilityOpp, opponentModel.getBidEvaluation(ourLast.getBid()))<=0){
					break;
				}
				newBid=omStrategy.getBid(outcomespace, utility);
				i++;
			}
			
			
			
		}
		else if(Double.compare(lastUtilityOwn, previousUtilityOwn)>=0 && Double.compare(lastUtilityOpp, previousUtilityOpp)<= 0){
			
			//concession or nice or silent move from opponent
			//we decrement a little bit our utility w.r.t. our previous bid
			if(Double.compare(ourLast.getMyUndiscountedUtil(),0.55)>=0){
				utility=ourLast.getMyUndiscountedUtil()-0.05;
			}
			else utility=ourLast.getMyUndiscountedUtil();
						
			//opponent new utility should be >= his utility of our previous bid +0.05
			//we try to generate a bid that respects this condition
			newBid=omStrategy.getBid(outcomespace, utility);
				
			int i=0;
			while(i<100){//to avoid cycling too much
				double newUtilityOpp=opponentModel.getBidEvaluation(newBid.getBid());
				if(Double.compare(newUtilityOpp, opponentModel.getBidEvaluation(ourLast.getBid())+0.05)>=0){
					break;
				}
				newBid=omStrategy.getBid(outcomespace, utility);
				i++;
			}
			
			
		}
		else if(Double.compare(lastUtilityOwn, previousUtilityOwn) < 0 && Double.compare(lastUtilityOpp, previousUtilityOpp) <= 0){
			
			//unfortunate move from the opponent
			utility=ourLast.getMyUndiscountedUtil();
						
			//opponent new utility should be >= his utility of our previous bid
			//we try to generate a bid that respects this condition
			newBid=omStrategy.getBid(outcomespace, utility);
				
			int i=0;
			while(i<100){//to avoid cycling too much
				
				double newUtilityOpp=opponentModel.getBidEvaluation(newBid.getBid());
				if(Double.compare(newUtilityOpp, opponentModel.getBidEvaluation(ourLast.getBid()))>0){
					break;
				}
				newBid=omStrategy.getBid(outcomespace, utility);
				i++;
			}
		}
		else if(Double.compare(lastUtilityOwn, previousUtilityOwn)> 0 && Double.compare(lastUtilityOpp, previousUtilityOpp) > 0){
		
			//fortunate move from opponent
			//we use the same increment of the selfish case
			if(Double.compare(ourLast.getMyUndiscountedUtil(), 0.95)<=0){
				utility=ourLast.getMyUndiscountedUtil() + 0.05;
			}
			else utility=ourLast.getMyUndiscountedUtil();
			
			
			//opponent new utility should be >= his utility of our previous bid +0.05
			//we try to generate a bid that respects this condition
			
			newBid=omStrategy.getBid(outcomespace, utility);
				
			int i=0;
			while(i<100){//to avoid cycling too much
				
				double newUtilityOpp=opponentModel.getBidEvaluation(newBid.getBid());
				if(Double.compare(newUtilityOpp, opponentModel.getBidEvaluation(ourLast.getBid())+0.05)>=0){
					break;
				}
				newBid=omStrategy.getBid(outcomespace, utility);
				i++;
					
			}
		}
		
		newBid.setTime(negotiationSession.getTime());
		
		return newBid;

	}
	
	@Override
	public BidDetails determineOpeningBid() {
		return negotiationSession.getMaxBidinDomain();
	}

	@Override
	public String getName() {
		return "Group39_BS_uncertainty";
	}

}

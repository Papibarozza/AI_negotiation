package group39;

import java.util.Map;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;

public class Group39_BS_old extends OfferingStrategy{
	private SortedOutcomeSpace outcomespace;
	public Group39_BS_old(){}
	
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

	}

	
	@SuppressWarnings("deprecation")
	@Override
	public BidDetails determineOpeningBid() {
		return negotiationSession.getMaxBidinDomain();
		
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public BidDetails determineNextBid() {
		
		double utility=0;
		
		//utility opponent's bid
		double lastUtility=negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
		System.out.println("Last utility: "+lastUtility);
		
		//utility previous bid
		double previousUtility=negotiationSession.getOwnBidHistory().getLastBidDetails().getMyUndiscountedUtil();
		System.out.println("Previous utility: "+previousUtility);
		
		//selection of the move
		if(lastUtility <= previousUtility){
			//the opponent lowered my utility, thus I increase it again
			utility=(lastUtility + getIncrement(negotiationSession.getTime()))/(lastUtility+1);
			System.out.println("Selfish");
		}
		else{
			//the opponent increased my utility, thus I concede something
			utility=(lastUtility - getDecrement(negotiationSession.getTime()))/(lastUtility-Math.E);
			utility=Math.abs(utility);
			System.out.println("Conceder");
		}
		
		System.out.println("Utility: "+utility);
		
		BidDetails newBid=negotiationSession.getOutcomeSpace().getBidNearUtility(utility);
		newBid.setTime(negotiationSession.getTime());
		System.out.println("new Bid: "+newBid);
		
		return newBid;
		
	}
	
	private double getIncrement(double time){
		return Math.exp(-time);
	}
	
	private double getDecrement(double time){
		return Math.exp(time);
	}

	@Override
	public String getName() {
		return "Group39_BS";
	}
	

}

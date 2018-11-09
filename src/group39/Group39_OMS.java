package group39;

import java.util.List;
import java.util.Map;
import java.util.Random;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OpponentModel;

public class Group39_OMS extends OMStrategy{
	private long possibleBids;

	
	@Override
	public void init(NegotiationSession negotiationSession, OpponentModel model, Map<String, Double> parameters) {
		super.init(negotiationSession, model, parameters);
		
		this.possibleBids = negotiationSession.getUtilitySpace().getDomain().getNumberOfPossibleBids();
	}
	
	

	@Override
	public BidDetails getBid(List<BidDetails> allBids) {
		//If there is only a single bid, return this bid
		if (allBids.size() == 1) {
			return allBids.get(0);
		}
		
		Random r= new Random();
		
		//we use this random double to decide whether to trust the opponent model and select
		//the best bid according to it or not
		double p=r.nextDouble();
		
		//if p is lower than 0.35 we return just a random bid
		if (p<0.35){
			return allBids.get(r.nextInt(allBids.size()));
		}
		else{
			
			double bestUtil = -1;
			BidDetails bestBid = allBids.get(0);

			// Check that not all bids are assigned at utility of 0
			// to ensure that the opponent model works. If the opponent model
			// does not work, offer a random bid.
			boolean allWereZero = true;
			// Determine the best bid
			for (BidDetails bid : allBids) {
				double evaluation = model.getBidEvaluation(bid.getBid());
				if (evaluation > 0.0001) {
					allWereZero = false;
				}
				if (evaluation > bestUtil) {
					bestBid = bid;
					bestUtil = evaluation;
				}
			}
			//The opponent model did not work, therefore, offer a random bid.
			if (allWereZero) {
				return allBids.get(r.nextInt(allBids.size()));
			}
			return bestBid;
		}
		
	}

	@Override
	public boolean canUpdateOM() {
		 // we stop updating the opponent model if just one second is left
		if (negotiationSession.getTime() > 0.99){
			return false;
		}
        
		// if the domain is big we stop updating when half of the time is left
		if (possibleBids>10000) {
			if (negotiationSession.getTime() > 0.5) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String getName() {
		return "Group39_OMS" ;
	}

}

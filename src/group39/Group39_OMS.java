package ai2018.group39;

import java.util.List;
import java.util.Map;
import java.util.Random;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OpponentModel;

public class Group39_OMS extends OMStrategy{
	private long possibleBids;
	double updateThreshold = 1.1;
	
	@Override
	public void init(NegotiationSession negotiationSession, OpponentModel model, Map<String, Double> parameters) {
		super.init(negotiationSession, model, parameters);
		if (parameters.get("t") != null) {
			updateThreshold = parameters.get("t").doubleValue();
		} else {
			System.out.println("OMStrategy assumed t = 1.1");
		}
		this.possibleBids = negotiationSession.getUtilitySpace().getDomain().getNumberOfPossibleBids();
	}
	
	

	@Override
	public BidDetails getBid(List<BidDetails> allBids) {
		
		if (allBids.size() == 1) {
			return allBids.get(0);
		}
		
		Random r= new Random();
		//we use this random double to decide whether to trust the opponent model and select
		//the best bid according to it or not
		double p=r.nextDouble();
		System.out.println("p is "+p);
		
		//if p is lower than 0.35 we return just a random bid
		if (p<0.35){
			System.out.println("Return a random bid");
			return allBids.get(r.nextInt(allBids.size()));
		}
		else{
			
			double bestUtil = -1;
			BidDetails bestBid = allBids.get(0);

			// 2. Check that not all bids are assigned at utility of 0
			// to ensure that the opponent model works. If the opponent model
			// does not work, offer a random bid.
			boolean allWereZero = true;
			// 3. Determine the best bid
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
			// 4. The opponent model did not work, therefore, offer a random bid.
			if (allWereZero) {
				return allBids.get(r.nextInt(allBids.size()));
			}
			System.out.println("Return bid with bid util equal to "+bestUtil);
			return bestBid;
		}
		
	}

	@Override
	public boolean canUpdateOM() {
		 // in the last seconds we don't want to lose any time
		if (negotiationSession.getTime() > 0.99){
			System.out.println("time is over 0.99, stop udate model");
			return false;
		}
        
		// in a big domain, we stop updating half-way
		if (possibleBids>10000) {
			if (negotiationSession.getTime() > 0.5) {
				return false;
			}
		}
		System.out.println("Update model");
		return true;
	}

	@Override
	public String getName() {
		return "Group39_OMS" ;
	}

}

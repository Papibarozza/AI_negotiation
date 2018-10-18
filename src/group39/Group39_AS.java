package group39;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;

public class Group39_AS extends AcceptanceStrategy {
	
	/** Parameter for concession function*/
	
	private double k;
	/** Minimum target utility */
	
	private double e;
	/** Outcome space */
	private SortedOutcomeSpace outcomespace;
	
	public Group39_AS() {
		
	}

	@SuppressWarnings("deprecation")
	@Override
	/***
	 * Implements the acceptance strategy mentioned in the paper: HardHeaded by Krimpen et al.
	 * We take into account the current time in the negotiation to not miss an oppurtunity to
	 * accept an offer before time runs out.
	*/
	
	public Actions determineAcceptability() {
		double t = negotiationSession.getTime();
		double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails()
				.getMyUndiscountedUtil();
		double minUtility = negotiationSession.getMinBidinDomain().getMyUndiscountedUtil();
		double maxUtility = negotiationSession.getMaxBidinDomain().getMyUndiscountedUtil();
		double P = minUtility + (1-f(t))*(maxUtility-minUtility);
		
		if(lastOpponentBidUtil >= P){
			return Actions.Accept;
		}else{
			return Actions.Reject;
		}
			
	}

	@Override
	public String getName() {
		return "Group39_AS";
	}
	
	public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel,
			Map<String, Double> parameters) throws Exception {
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;
		this.opponentModel = opponentModel;
		outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
		negotiationSession.setOutcomeSpace(outcomespace);

		if (parameters.get("k") != null)
			this.k = parameters.get("k");
		else
			this.k = 0;
		if (parameters.get("e") != null)
			this.e = parameters.get("e");
		else
			this.e = 1;


	}

	@Override
	public String printParameters() {
		
		String str = "[e: " + e + " k: " + k + "]";
		return str;
	}
	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("e", 0.1, "Concession rate"));
		set.add(new BOAparameter("k", 0.0, "Offset"));

		return set;
	}
	/*
	 * Concession function to be used when determine acceptability.
	 */
	public double f(double t) {
		if (e == 0)
			return k;
		double ft = k + (1 - k) * Math.pow(t, 1.0 / e);
		return ft;
	}

}

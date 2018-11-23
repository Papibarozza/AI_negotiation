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
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.UtilitySpace;

public class Group39_AS_uncertainty extends AcceptanceStrategy{

    	/** Parameter for concession function*/
	
	private double k;
	/** Minimum target utility */
	
	private double e;
	/** Outcome space */
	private SortedOutcomeSpace outcomespace;
	private Double minAcceptanceValue;
	
	private UserModel userModel;
	private AbstractUtilitySpace utilitySpace;
	
	public Group39_AS_uncertainty() {
		
	}

	
	/***
	 * Implements the acceptance strategy mentioned in the paper: HardHeaded by Krimpen et al.
	 * We take into account the current time in the negotiation to not miss an oppurtunity to
	 * accept an offer before time runs out.
	*/
	@Override
	public Actions determineAcceptability() {
		
		double t = negotiationSession.getTime();
		double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails()
				.getMyUndiscountedUtil();
		double myNextBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil();
		double minUtility = negotiationSession.getMinBidinDomain().getMyUndiscountedUtil();
		double maxUtility = negotiationSession.getMaxBidinDomain().getMyUndiscountedUtil();
		double minAcceptanceValue = this.minAcceptanceValue;
		double P = minUtility + (1-f(t))*(maxUtility-minUtility);
		
		if(myNextBidUtil < lastOpponentBidUtil && lastOpponentBidUtil > minAcceptanceValue){
			return Actions.Accept;
		}
		if(lastOpponentBidUtil >= P && lastOpponentBidUtil > minAcceptanceValue){
			return Actions.Accept;
		}else{
			return Actions.Reject;
		}
			
	}

	@Override
	public String getName() {
		return "Group39_AS_uncertainty";
	}
	
	public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel,
			Map<String, Double> parameters) throws Exception {
		this.negotiationSession = negoSession;
		this.userModel = negotiationSession.getUserModel();
		
//		If there is a user model, we are working under uncertainty
		if (userModel != null) {
//			The uncertainty helper calculates the utility space for us
			Uncertainty_Helper helper = new Uncertainty_Helper(negoSession);
			this.utilitySpace = helper.estimateUtilitySpace();
		} else {
			this.utilitySpace = negoSession.getUtilitySpace();
		}
		
		this.offeringStrategy = strat;
		this.opponentModel = opponentModel;
		outcomespace = new SortedOutcomeSpace(this.utilitySpace);
		negotiationSession.setOutcomeSpace(outcomespace);
		
		if (parameters == null) {
			this.k = 0;
			this.e = 1;
			this.minAcceptanceValue = 0.5;
			return;
		}
		
		if (parameters.get("k") != null)
			this.k = parameters.get("k");
		else
			this.k = 0;
		if (parameters.get("e") != null)
			this.e = parameters.get("e");
		else
			this.e = 1;
		if (parameters.get("mav") != null)
			this.minAcceptanceValue = parameters.get("mav");
		else
			this.minAcceptanceValue = 0.5;


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
		set.add(new BOAparameter("mav", 0.5, "Min Acceptance Value"));

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

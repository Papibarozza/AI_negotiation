package group39;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Objective;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

/**
 * BOA framework implementation of the HardHeaded Frequecy Model.
 * 
 * Default: learning coef l = 0.2; learnValueAddition v = 1.0
 * 
 * paper: https://ii.tudelft.nl/sites/default/files/boa.pdf
 */
public class Group39_OM extends OpponentModel {

	/*
	 * the learning coefficient is the weight that is added each turn to the
	 * issue weights which changed. It's a trade-off between concession speed
	 * and accuracy.
	 */
	private double learnCoef;
	/*
	 * value which is added to a value if it is found. Determines how fast the
	 * value weights converge.
	 */
	private int learnValueAddition;
	private int amountOfIssues;
	private double goldenValue;
	
	private double learnCoefatStart;
	private int learnValueAdditionStart;
	private int numberBidChanges;
	private int MaxUpdates;


	@SuppressWarnings("deprecation")
	@Override
	public void init(NegotiationSession negotiationSession,
			Map<String, Double> parameters) {
		this.negotiationSession = negotiationSession;
		if (parameters != null && parameters.get("l") != null) {
			learnCoef = parameters.get("l");
		} else {
			learnCoef = 0.2;
		}
//		Test with 2,3 and 4 as well
		learnValueAddition = 5;
		learnValueAdditionStart=learnValueAddition;
		learnCoefatStart=learnCoef;
		MaxUpdates=100;
		numberBidChanges=0;
		
		opponentUtilitySpace = (AdditiveUtilitySpace) negotiationSession
				.getUtilitySpace().copy();
		amountOfIssues = opponentUtilitySpace.getDomain().getIssues().size();
		/*
		 * This is the value to be added to weights of unchanged issues before
		 * normalization. Also the value that is taken as the minimum possible
		 * weight, (therefore defining the maximum possible also).
		 */
		goldenValue = learnCoef / amountOfIssues;
		
		initializeModel();

	}

	
	@Override
	public void updateModel(Bid opponentBid, double time) {
		if (negotiationSession.getOpponentBidHistory().size() < 2) {
			return;
		}
		int numberOfUnchanged = 0;
		BidDetails oppBid = negotiationSession.getOpponentBidHistory()
				.getHistory()
				.get(negotiationSession.getOpponentBidHistory().size() - 1);
		BidDetails prevOppBid = negotiationSession.getOpponentBidHistory()
				.getHistory()
				.get(negotiationSession.getOpponentBidHistory().size() - 2);
//		bidList is a collection of the opponents previous bids we are considering
		List<BidDetails> bidList = new ArrayList<>();
		bidList.add(oppBid);
		bidList.add(prevOppBid);
//		If the opponent has given 4 or more bids we consider the 4 last ones. I.e. add them to bidList
		if (negotiationSession.getOpponentBidHistory().size() > 3) {
			BidDetails prevPrevOppBid = negotiationSession.getOpponentBidHistory()
					.getHistory()
					.get(negotiationSession.getOpponentBidHistory().size() - 3);
			bidList.add(prevPrevOppBid);
			
			BidDetails prevPrevPrevOppBid = negotiationSession.getOpponentBidHistory()
					.getHistory()
					.get(negotiationSession.getOpponentBidHistory().size() - 4);
			bidList.add(prevPrevPrevOppBid);
		}
//		This is a hashmap of <IssueValue, changed>. Where changed is 0 or 1 depending on whether we want
//		to give it more weight or not
		HashMap<Integer, Integer> lastTwoDiffSet = determineLastTwoDifference(bidList);
		HashMap<Integer, Integer> lastFourDiffSet = determineLastFourDifference(bidList);

		// count the number of changes in value
		for (Integer i : lastTwoDiffSet.keySet()) {
			if (lastTwoDiffSet.get(i) == 0){
				numberOfUnchanged++;
				
			}
		}
		

			
//		TODO: when should these two methods be called
		numberBidChanges++;
		this.updateLearnValueAddition(); //update value
		this.updateLearnCoef();
		goldenValue=learnCoef/amountOfIssues;
	
		// The total sum of weights before normalization.
		double totalSum = 1D + goldenValue * numberOfUnchanged;
		// The maximum possible weight
		double maximumWeight = 1D - (amountOfIssues) * goldenValue / totalSum;

		// re-weighing issues while making sure that the sum remains 1
		for (Integer i : lastTwoDiffSet.keySet()) {
			Objective issue = opponentUtilitySpace.getDomain()
					.getObjectivesRoot().getObjective(i);
			double weight = opponentUtilitySpace.getWeight(i);
			double newWeight;

//				If the last two bids have the same value for issue i, we update
			if (lastTwoDiffSet.get(i) == 0 && weight < maximumWeight) {
				newWeight = (weight + goldenValue) / totalSum;
			} else {
				newWeight = weight / totalSum;
			}
			opponentUtilitySpace.setWeight(issue, newWeight);
		}

		// Then for each issue value that has been offered last time, a constant
		// value is added to its corresponding ValueDiscrete.
		try {
			for (Entry<Objective, Evaluator> e : opponentUtilitySpace
					.getEvaluators()) {
				EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
				IssueDiscrete issue = ((IssueDiscrete) e.getKey());
				/*
				 * add constant learnValueAddition to the current preference of
				 * the value to make it more important
				 */
				
//					If the last 4 bids give at least 2 unique values for the issue, we increment
				if (lastFourDiffSet.get(issue.getNumber()) == 1) {
					ValueDiscrete issuevalue = (ValueDiscrete) oppBid.getBid()
							.getValue(issue.getNumber());
					Integer eval = value.getEvaluationNotNormalized(issuevalue);

					value.setEvaluation(issuevalue, (learnValueAddition + eval));
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	

	@Override
	public double getBidEvaluation(Bid bid) {
		double result = 0;
		try {
			result = opponentUtilitySpace.getUtility(bid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public String getName() {
		return "Group39_OM";
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("l", 0.2,
				"The learning coefficient determines how quickly the issue weights are learned"));
		return set;
	}

	/**
	 * Init to flat weight and flat evaluation distribution
	 */
	private void initializeModel() {
		double commonWeight = 1D / amountOfIssues;

		for (Entry<Objective, Evaluator> e : opponentUtilitySpace
				.getEvaluators()) {

			opponentUtilitySpace.unlock(e.getKey());
			e.getValue().setWeight(commonWeight);
			try {
				// set all value weights to one (they are normalized when
				// calculating the utility)
				for (ValueDiscrete vd : ((IssueDiscrete) e.getKey())
						.getValues())
					((EvaluatorDiscrete) e.getValue()).setEvaluation(vd, 1);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

//	@param bids is a list of up to the last four bids offered by the opponent
//	Description: Compare each IssueValue of the opponents last two bids
//	Return: A hashmap with one entry for each issue, where the keys are the issue numbers
// 	The value for each key is an integer 0 or 1. 0 means that the last two IssueValues were equal
//	1 means that they were not equal.
	private HashMap<Integer, Integer> determineLastTwoDifference(List<BidDetails> bids) {
		HashMap<Integer, Integer> diff = new HashMap<Integer, Integer>();
		try {
			for (Issue i : opponentUtilitySpace.getDomain().getIssues()) {
				Value lastBid = bids.get(0).getBid().getValue(i.getNumber());
				Value prevBid = bids.get(1).getBid().getValue(i.getNumber());
				
//				If the last to bids were the same we set 0
				diff.put(i.getNumber(), (lastBid.equals(prevBid)) ? 0 : 1);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return diff;
	}
	
//	@param bids is a list of up to the last four bids offered by the opponent
//	Description: Compare each IssueValue of the opponents last four bids
//	Return: A hashmap with one entry for each issue, where the keys are the issue numbers
// 	The value for each key is an integer 0 or 1. 0 means that the last four IssueValues were all equal
//	1 means that they were not all equal.
	private HashMap<Integer, Integer> determineLastFourDifference(List<BidDetails> bids) {
		HashMap<Integer, Integer> diff = new HashMap<Integer, Integer>();
		try {
			for (Issue i : opponentUtilitySpace.getDomain().getIssues()) {
//				Value lastBid = bids.get(0).getBid().getValue(i.getNumber());
//				Value prevBid = bids.get(1).getBid().getValue(i.getNumber());
//				diff.put(i.getNumber(), (lastBid.equals(prevBid)) ? 0 : 1);
				if (bids.size() < 4) {
					diff.put(i.getNumber(), 1);
				} else {
					boolean allSame = true;
					for (int j = 1; j < bids.size(); j++) {
						Value val1 = bids.get(j-1).getBid().getValue(i.getNumber());
						Value val2 = bids.get(j).getBid().getValue(i.getNumber());
						if (! val1.equals(val2)) {
//							We found two unequal values so there are at least two unique values
							allSame = false;
							break;
						}
					}
					if (allSame) {
						diff.put(i.getNumber(), 0);
					} else {
						diff.put(i.getNumber(), 1);
					}
				}
				
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return diff;
	}	
	
	//This function updates the parameter learnValueAddition. The value of decreases linearly from
	//learnValueAdditionStart to 1 when we have reached the end of the negotiation.
	private void updateLearnValueAddition(){
		double t=negotiationSession.getTime();
		learnValueAddition=(int)Math.round(learnValueAdditionStart-(learnValueAddition-1)*t);
	}
	
	//This function updates the parameter learnCoef. It decreases linearly from the value learnCoefStart to the
	//parameter c with respect to time.
	private void updateLearnCoef() {
		double t=negotiationSession.getTime();
		learnCoef=learnCoefatStart-(learnCoefatStart-0.1)*t;
	}

}

package group39;

import java.util.HashMap;
import java.util.HashSet;
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
	
	private boolean doUpdate = true;
	private boolean doSimpleUpdates = true;

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
		learnValueAddition = 1;
		opponentUtilitySpace = (AdditiveUtilitySpace) negotiationSession
				.getUtilitySpace().copy();
		amountOfIssues = opponentUtilitySpace.getDomain().getIssues().size();
		/*
		 * This is the value to be added to weights of unchanged issues before
		 * normalization. Also the value that is taken as the minimum possible
		 * weight, (therefore defining the maximum possible also).
		 */
		goldenValue = learnCoef / amountOfIssues;

		System.out.println("Init at time: " + negotiationSession.getTime());
		
		initializeModel();

	}

	@Override
	protected void updateModel(Bid bid, double time) {
		if (! this.doUpdate || (negotiationSession.getTime() > 0.65)) {
			System.out.println("No longer updating");
			return;
		}
		if (this.doSimpleUpdates && negotiationSession.getTime()<0.2) {
			this.earlyUpdateModel(bid,  time);
		} else {
			this.midUpdateModel(bid, time);
		}
		this.testPrecision();
	}
	
//	The original frequency model update
	public void earlyUpdateModel(Bid opponentBid, double time) {
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
		HashMap<Integer, Integer> lastDiffSet = determineDifference(prevOppBid,
				oppBid);

		// count the number of changes in value
		for (Integer i : lastDiffSet.keySet()) {
			if (lastDiffSet.get(i) == 0)
				numberOfUnchanged++;
		}

		// The total sum of weights before normalization.
		double totalSum = 1D + goldenValue * numberOfUnchanged;
		// The maximum possible weight
		double maximumWeight = 1D - (amountOfIssues) * goldenValue / totalSum;

		// re-weighing issues while making sure that the sum remains 1
		for (Integer i : lastDiffSet.keySet()) {
			Objective issue = opponentUtilitySpace.getDomain()
					.getObjectivesRoot().getObjective(i);
			double weight = opponentUtilitySpace.getWeight(i);
			double newWeight;

			if (lastDiffSet.get(i) == 0 && weight < maximumWeight) {
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
				ValueDiscrete issuevalue = (ValueDiscrete) oppBid.getBid()
						.getValue(issue.getNumber());
				Integer eval = value.getEvaluationNotNormalized(issuevalue);
				
				if (eval > 5) {
					this.doSimpleUpdates = false;
				}
					
					
				value.setEvaluation(issuevalue, (learnValueAddition + eval));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (! this.doSimpleUpdates) {
			System.out.println("Done with simple update at time " + negotiationSession.getTime() + ": \n" + this.opponentUtilitySpace);
		}
	}
	
	public void midUpdateModel(Bid opponentBid, double time) {
		if (negotiationSession.getOpponentBidHistory().size() < 2) {
			return;
		}
		
		
		BidDetails oppBid = negotiationSession.getOpponentBidHistory()
				.getHistory()
				.get(negotiationSession.getOpponentBidHistory().size() - 1);
		BidDetails prevOppBid = negotiationSession.getOpponentBidHistory()
				.getHistory()
				.get(negotiationSession.getOpponentBidHistory().size() - 2);
		BidDetails prevPrevOppBid = negotiationSession.getOpponentBidHistory()
				.getHistory()
				.get(negotiationSession.getOpponentBidHistory().size() - 3);
		
		
		double myPrevUtil = prevOppBid.getMyUndiscountedUtil();
		int newBidBetterOrWorse = betterOrWorse(oppBid.getMyUndiscountedUtil(), myPrevUtil);
		
		int numberOfUnchanged = 0;
//		HashMap<Integer, Integer> lastDiffSet = determineDifference(prevOppBid,
//				oppBid);
		HashMap<Integer, Integer> lastDiffSet = determineTripleDifference(oppBid,
				prevOppBid, prevPrevOppBid);

		// count the number of changes in value
		for (Integer i : lastDiffSet.keySet()) {
			if (lastDiffSet.get(i) == 0)
				numberOfUnchanged++;
		}

		// The total sum of weights before normalization.
		double totalSum = 1D + goldenValue * numberOfUnchanged;
		// The maximum possible weight
		double maximumWeight = 1D - (amountOfIssues) * goldenValue / totalSum;

		// re-weighing issues while making sure that the sum remains 1
		for (Integer i : lastDiffSet.keySet()) {
			Objective issue = opponentUtilitySpace.getDomain()
					.getObjectivesRoot().getObjective(i);
			double weight = opponentUtilitySpace.getWeight(i);
			double newWeight;

			if (lastDiffSet.get(i) == 0 && weight < maximumWeight) {
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
				
//				The value to be changed depends on how the utility changed for us
				ValueDiscrete issuevalue;
				if (newBidBetterOrWorse == 0) {
					continue;
				} else if (newBidBetterOrWorse == 1) {
					if (myPrevUtil < 0.75 && myPrevUtil > 0.63) {
						issuevalue = (ValueDiscrete) prevOppBid.getBid()
								.getValue(issue.getNumber());
					
					} else {
						issuevalue = (ValueDiscrete) oppBid.getBid()
								.getValue(issue.getNumber());
					}
				} else {
					issuevalue = (ValueDiscrete) prevOppBid.getBid()
							.getValue(issue.getNumber());
				}
				
				Integer eval = value.getEvaluationNotNormalized(issuevalue);
				
//				TODO
				if (eval < 20) {
					value.setEvaluation(issuevalue, (learnValueAddition + eval));
				}
				
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		System.out.println("At time: " + negotiationSession.getTime() + " the opp model says: \n" + opponentUtilitySpace + "\n \n");
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
		return "Group39 OM";
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

	/**
	 * Determines the difference between bids. For each issue, it is determined
	 * if the value changed. If this is the case, a 1 is stored in a hashmap for
	 * that issue, else a 0.
	 * 
	 * @param a
	 *            bid of the opponent
	 * @param another
	 *            bid
	 * @return
	 */
	private HashMap<Integer, Integer> determineDifference(BidDetails first,
			BidDetails second) {

		HashMap<Integer, Integer> diff = new HashMap<Integer, Integer>();
		try {
			for (Issue i : opponentUtilitySpace.getDomain().getIssues()) {
				Value value1 = first.getBid().getValue(i.getNumber());
				Value value2 = second.getBid().getValue(i.getNumber());
				diff.put(i.getNumber(), (value1.equals(value2)) ? 0 : 1);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return diff;
	}
	
	
	private HashMap<Integer, Integer> determineTripleDifference(BidDetails first,
			BidDetails second, BidDetails third) {

		HashMap<Integer, Integer> diff = new HashMap<Integer, Integer>();
		try {
			for (Issue i : opponentUtilitySpace.getDomain().getIssues()) {
				Value value1 = first.getBid().getValue(i.getNumber());
				Value value2 = second.getBid().getValue(i.getNumber());
				Value value3 = third.getBid().getValue(i.getNumber());
				boolean firstEqualsSecond = value1.equals(value2) ? true : false;
				boolean firstEqualsThird = value1.equals(value3) ? true : false;
				
				if (firstEqualsSecond) {
					if (firstEqualsThird) {
						diff.put(i.getNumber(), 1);
					} else {
						diff.put(i.getNumber(), 0);
					}
				} else {
					diff.put(i.getNumber(), 1);
				}
				
//				diff.put(i.getNumber(), (value1.equals(value2)) ? 0 : 1);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return diff;
	}
	
//	Compares opponents previous bid with newest in regards to my utility
//	Returns 1 if it is better, -1 if worse and 0 if about even
	private double evenParameter = 0.05;
	private int betterOrWorse(double newUtil, double oldUtil) {
		
		double diff = newUtil - oldUtil;
		if (Math.abs(diff) < evenParameter) {
//			System.out.println("Newbid is about the same");
			return 0;
		}
		if (newUtil > oldUtil) {
//			System.out.println("Newbid is better");
			return 1;
		}
//		System.out.println("Newbid is worse");
		return -1;
	}
	
	
	private void testPrecision() {
		try {
			Bid bestBid = opponentUtilitySpace.getMaxUtilityBid();
			Bid testBid = new Bid(bestBid);
			System.out.println("Test pre change: " + opponentUtilitySpace.getUtility(testBid));
			ValueDiscrete food = new ValueDiscrete("Catering");
			ValueDiscrete drink = new ValueDiscrete("Non-Alcoholic");
			ValueDiscrete location = new ValueDiscrete("Your Dorm");
			ValueDiscrete invi = new ValueDiscrete("Plain");
			ValueDiscrete music = new ValueDiscrete("Band");
			ValueDiscrete clean = new ValueDiscrete("Water and Soap");
			testBid = testBid.putValue(1, (Value) food);
			testBid = testBid.putValue(2, (Value) drink);
			testBid = testBid.putValue(3, (Value) location);
			testBid = testBid.putValue(4, (Value) invi);
			testBid = testBid.putValue(5, (Value) music);
			testBid = testBid.putValue(6, (Value) clean);
			System.out.println("Test: " + opponentUtilitySpace.getUtility(testBid));
//			Bid selfBestBid = negotiationSession.getMaxBidinDomain().getBid();
//			System.out.println(negotiationSession.getUtilitySpace().getUtility(selfBestBid));
//			System.out.println(opponentUtilitySpace.getUtility(selfBestBid));
//			selfBestBid = selfBestBid.putValue(1, food);
//			System.out.println(selfBestBid);
//			System.out.println(negotiationSession.getUtilitySpace().getUtility(selfBestBid));
//			System.out.println(opponentUtilitySpace.getUtility(selfBestBid));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		System.out.println(negotiationSession.getDomain().getIssues());
//		System.out.println(negotiationSession.getIssues());
		
	}

}
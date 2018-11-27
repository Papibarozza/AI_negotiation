package group39;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import genius.core.Domain;
import genius.core.boaframework.NegotiationSession;
import genius.core.issue.Issue;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
/**
 * 
 * The uncertainty helper takes as input a negotiation session with a user model.
 * The main method estimateUitilitySpace() returns an AbstractUtilitySpace for the user model.
 *
 */


public class Uncertainty_Helper {
	NegotiationSession session;
	UserModel userModel;
	
	
	public Uncertainty_Helper(NegotiationSession session) {
		this.session = session;
		this.userModel = session.getUserModel();
	}
	
	/**
	 * With this method, we override the default estimate of the utility
	 * space given uncertain preferences specified by the user model. This
	 * method uses a frequence model to estimate the utility space when uncertainty is present
	 */
	public AbstractUtilitySpace estimateUtilitySpace() {
		
		Domain domain = session.getDomain();
		AdditiveUtilitySpaceFactory factory = new AdditiveUtilitySpaceFactory(domain);
						
		HashMap<Integer,Double> IssueWeights = generateIssueWeights(domain);
		HashMap<Integer,HashMap<ValueDiscrete,Integer>> ValueWeights = generateValueWeights(domain);
		
		//Set issue weights
		int nrIssues = IssueWeights.size();
		List<Issue> Issues = new ArrayList<>();
		double[] IssueWeightsNotNormalized = new double[nrIssues];
		int idx1=0; //Keep track of issue index
		//Iterate through all issues and store their weights in an double array IssueWeightsNotNormalized
		//and moreover set utility for each value
		for(Issue issue : domain.getIssues()) {
			
			Issues.add(issue);
			int issueNr=issue.getNumber();
			IssueWeightsNotNormalized[idx1]=IssueWeights.get(issueNr);
			for(ValueDiscrete issueValue : ValueWeights.get(issueNr).keySet()) {
				int w= ValueWeights.get(issueNr).get(issueValue);
				factory.setUtility(issue, issueValue, w);
			}
			idx1++;
		}
		//Normalize issue weights and then set weights, then return.
		double[] IssueWeightsNormalized=divide(IssueWeightsNotNormalized,sumVector(IssueWeightsNotNormalized));
		factory.getUtilitySpace().setWeights(Issues, IssueWeightsNormalized);
		
		return factory.getUtilitySpace();
	}
	//Help function that divides an array with a scalar
	private double[] divide(double[] d, double n) {
		for(int i=0; i<d.length;i++) {
			d[i]=d[i]/n;
		}
		return d;
	}
	
	//Help function that determines the sum of an array containing doubles
	private double sumVector(double[] d) {
		double s=0;
		for(int i=0;i<d.length;i++) {
			s=s+d[i];
		}
		return s;
	}
	
	//Method that iterates through all the bids in the usermodel. The issues are ordered from low to high utility.
	//The weight w is added to an issue when the issue has the same value within two following bids.
	//The method then returns a hashmap with keys corresponding to issue number and the value as the weight. Note
	//that these weight are not normalized.
	private HashMap<Integer, Double> generateIssueWeights(Domain domain){
		//Parameters
		double a=1;
		double b=3;
		
		//Initialize hashmap
		HashMap<Integer, Double> IssueWeights = new HashMap<Integer,Double>();
		int nrBids = this.userModel.getBidRanking().getSize();
		//Go through all bids in the usermodel
		for(int i=0;i<nrBids-1;i++) {
			try {
				//Decreasing added issue weight
				double w=(b-((b-a)*(i/nrBids)));
				
				for(Issue issue : domain.getIssues()) {
					Value v1= userModel.getBidRanking().getBidOrder().get(nrBids-1-i).getValue(issue.getNumber());
					Value v2= userModel.getBidRanking().getBidOrder().get(nrBids-2-i).getValue(issue.getNumber());
	//				
					//add weight if two issues are the same
					if(v1.equals(v2)) {
						if(IssueWeights.containsKey(issue.getNumber())) {
							double c=IssueWeights.get(issue.getNumber())+w;
							IssueWeights.replace(issue.getNumber(), c);
						}else {
							IssueWeights.put(issue.getNumber(), w);
						}
					}
				}
			}catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return IssueWeights;
	}
	
	//This method is somewhat similar to generateIssueWeights in a way that ll the bids in the usermodel are iterated
	//but a weight is added to each issue value and not the issue itself. Moreover, the weight of a value is divided by it
	//rank which significantly improved our model.  
	private HashMap<Integer, HashMap<ValueDiscrete, Integer>> generateValueWeights(Domain domain){
		HashMap<Integer, HashMap<ValueDiscrete, Integer>> AllValueWeights = new HashMap<Integer,HashMap<ValueDiscrete, Integer>>();
		int nrBids = userModel.getBidRanking().getSize();
//		We give each issueValue a rank. The first value seen for an issue has rank 1, the second
//			unique value has rank 2 etc.
		HashMap<Integer, HashMap<ValueDiscrete, Integer>> allValueRanks = new HashMap<Integer,HashMap<ValueDiscrete, Integer>>();
		//parameters to experiment with, b=5 and a=1 was considered as good parameters after testing
		int b=5;
		int a=1;
		
		for(int i=0;i<nrBids-1;i++) {
			try {
				int w=Math.round((b-((b-a)*(i/nrBids)))); //Decrease added weight
				for(Issue issue : domain.getIssues()) {
					int issueNr = issue.getNumber();
					ValueDiscrete v= (ValueDiscrete) userModel.getBidRanking().getBidOrder().get(nrBids-1-i).getValue(issue.getNumber());
					if(AllValueWeights.containsKey(issueNr)) {
						if(AllValueWeights.get(issueNr).containsKey(v)) {
							int c=AllValueWeights.get(issueNr).get(v)+w;
							AllValueWeights.get(issueNr).replace(v, c);
						}else {
							AllValueWeights.get(issueNr).put(v,b);
						}
					}else {
						HashMap<ValueDiscrete, Integer> ValueWeights = new HashMap<ValueDiscrete, Integer>();
						ValueWeights.put(v, w);
						AllValueWeights.put(issueNr, ValueWeights);
					}
					if(allValueRanks.containsKey(issueNr)) {
						if(!allValueRanks.get(issueNr).containsKey(v)){
							int rank = allValueRanks.get(issueNr).size();
							allValueRanks.get(issueNr).put(v, rank+1);
						}
					} else {
						HashMap<ValueDiscrete, Integer> valueRanks = new HashMap<ValueDiscrete, Integer>();
						valueRanks.put(v, 1);
						allValueRanks.put(issueNr, valueRanks);
					}
				}
			}catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		
//		We divide each valueWeight by their respective valueRanks. 
		for(Integer issue : AllValueWeights.keySet()) {
			for(ValueDiscrete issueValue : AllValueWeights.get(issue).keySet()) {
				int oldValue = AllValueWeights.get(issue).get(issueValue);
				int rank;
				try {
					rank = allValueRanks.get(issue).get(issueValue);
				} catch (Exception e) {
					rank = allValueRanks.get(issue).size();
				}
				int newValue = Math.round(oldValue/rank);
				AllValueWeights.get(issue).replace(issueValue, newValue);
			}
		}
		
		return AllValueWeights;
	}

}

package group39;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import genius.core.Domain;
import genius.core.boaframework.NegotiationSession;
import genius.core.issue.Issue;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;

public class Uncertainty {
	
	
public AbstractUtilitySpace estimateUtilitySpace(NegotiationSession session) {
		
		Domain domain = session.getDomain();
		AdditiveUtilitySpaceFactory factory = new AdditiveUtilitySpaceFactory(domain);
						
		HashMap<Integer,Double> IssueWeights = generateIssueWeights(domain, session.getUserModel());
		HashMap<Integer,HashMap<ValueDiscrete,Double>> ValueWeights = generateValueWeights(domain, session.getUserModel());
		
		//Set issue weights
		int nrIssues = IssueWeights.size();
		List<Issue> Issues = new ArrayList<>();
		double[] IssueWeightsNotNormalized = new double[nrIssues];
		int idx1=0;
		for(Issue issue : domain.getIssues()) {
			
			
			
			Issues.add(issue);
			int issueNr=issue.getNumber();
			IssueWeightsNotNormalized[idx1]=IssueWeights.get(issueNr);
			for(ValueDiscrete issueValue : ValueWeights.get(issueNr).keySet()) {
				double w= ValueWeights.get(issueNr).get(issueValue);
				factory.setUtility(issue, issueValue, w);
			}
			idx1++;
		}
		double[] IssueWeightsNormalized=divide(IssueWeightsNotNormalized,sumVector(IssueWeightsNotNormalized));
		
		factory.getUtilitySpace().setWeights(Issues, IssueWeightsNormalized);
		
		return factory.getUtilitySpace();
		//return new AdditiveUtilitySpaceFactory(getDomain()).getUtilitySpace();
	}

private HashMap<Integer, Double> generateIssueWeights(Domain domain, UserModel userModel){
	//Parameters that 
	double a=0;
	double b=1;
	
	HashMap<Integer, Double> IssueWeights = new HashMap<Integer,Double>();
	int nrBids = userModel.getBidRanking().getSize();	
	for(int i=0;i<nrBids-1;i++) {
		try {
			//Decreasing added issue weight
			double w=(b-(b-a)*(i/nrBids));
			
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

private HashMap<Integer, HashMap<ValueDiscrete, Double>> generateValueWeights(Domain domain, UserModel userModel){
	HashMap<Integer, HashMap<ValueDiscrete, Double>> AllValueWeights = new HashMap<Integer,HashMap<ValueDiscrete, Double>>();
	int nrBids = userModel.getBidRanking().getSize();
	
	//parameters to experiment with
	double b=1; //first value
	double a=0; //should a decrease linearly? from b to a
	
	for(int i=0;i<nrBids;i++) {
		try {
			
			double w=(b-(b-a)*(i/nrBids));
			
			for(Issue issue : domain.getIssues()) {
				int issueNr = issue.getNumber();
				ValueDiscrete v= (ValueDiscrete) userModel.getBidRanking().getBidOrder().get(nrBids-1-i).getValue(issue.getNumber());
				if(AllValueWeights.containsKey(issueNr)) {
					if(AllValueWeights.get(issueNr).containsKey(v)) {
						double c=AllValueWeights.get(issueNr).get(v)+w;
						AllValueWeights.get(issueNr).replace(v, c);
					}else {
						AllValueWeights.get(issueNr).put(v,b);
					}
				}else {
					HashMap<ValueDiscrete, Double> ValueWeights = new HashMap<ValueDiscrete, Double>();
					ValueWeights.put(v, w);
					AllValueWeights.put(issueNr, ValueWeights);
				}
			}
		}catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	return AllValueWeights;
}


private double[] divide(double[] d, double n) {
	for(int i=0; i<d.length;i++) {
		d[i]=d[i]/n;
	}
	return d;
}

private double sumVector(double[] d) {
	double s=0;
	for(int i=0;i<d.length;i++) {
		s=s+d[i];
	}
	return s;
}

}

package Group39;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import agents.anac.y2013.MetaAgent.portfolio.thenegotiatorreloaded.NegotiationSession;
import agents.anac.y2014.E2Agent.myUtility.SessionData;
import genius.core.Domain;
import genius.core.boaframework.BoaParty;
import genius.core.issue.Issue;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.NegotiationInfo;
import genius.core.persistent.PersistentDataType;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.utility.AbstractUtilitySpace;

public class Group39_Agent extends BoaParty {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;



	public Group39_Agent() {
		super(null, new HashMap<String, Double>(), null,
				new HashMap<String, Double>(), null,
				new HashMap<String, Double>(), null,
				new HashMap<String, Double>());
	}
	
	@Override
	public void init(NegotiationInfo info) {
		SessionData sessionData = null; 
		if (info.getPersistentData().getPersistentDataType() == PersistentDataType.SERIALIZABLE) {
			sessionData = (SessionData) info.getPersistentData().get();
		}
		if (sessionData==null) {
			sessionData= new SessionData(getNumberOfParties());
		}
	
		
		//negotiationSession = new NegotiationSession(sessionData , info.getUtilitySpace(), info.getTimeline());
		
		//Map<String, Double> parameters = new Map<String,Double>();
		
		opponentModel = new Group39_OM (); 
		opponentModel.init(negotiationSession , new HashMap <String , Double >()); 
		omStrategy = new Group39_OMS();
		omStrategy.init(negotiationSession, opponentModel,new HashMap <String , Double >());
		offeringStrategy = new Group39_BS();
		try {
			offeringStrategy.init(negotiationSession, opponentModel, omStrategy, new HashMap <String , Double >());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		acceptConditions = new Group39_AS();
		try {
			acceptConditions.init(negotiationSession, offeringStrategy, opponentModel,new HashMap <String , Double >());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	

	}
	
	private void log(String s) {
		System.out.println(s);
	}
	
	private HashMap<Integer, Double> generateIssueWeights(Domain domain){
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
	
	private HashMap<Integer, HashMap<ValueDiscrete, Double>> generateValueWeights(Domain domain){
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
	
	

	/**
	 * With this method, you can override the default estimate of the utility
	 * space given uncertain preferences specified by the user model. This
	 * example sets every value to zero.
	 */
	@Override
	public AbstractUtilitySpace estimateUtilitySpace() {
		
		log("\nHEEEEELLLOOOOOO\n");
		
		Domain domain = getDomain ();
		AdditiveUtilitySpaceFactory factory = new AdditiveUtilitySpaceFactory(domain);
						
		HashMap<Integer,Double> IssueWeights = generateIssueWeights(domain);
		HashMap<Integer,HashMap<ValueDiscrete,Double>> ValueWeights = generateValueWeights(domain);
		
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
	
	
	
	@Override
	public String getDescription() {
		return "Group39_AGENT"; 
	}
	
}
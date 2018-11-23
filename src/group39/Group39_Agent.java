package group39;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.Domain;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Offer;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.Actions;
import genius.core.boaframework.BoaParty;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.NoModel;
import genius.core.boaframework.SessionData;
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
	private Domain domain;
	private Bid oppBid;
	private NegotiationInfo info;



	public Group39_Agent() {
//		super(null, new HashMap<String, Double>(), null,
//				new HashMap<String, Double>(), null,
//				new HashMap<String, Double>(), null,
//				new HashMap<String, Double>());
		super(new Group39_AS(), null, new Group39_BS(), null, new Group39_OM(), null, new Group39_OMS(), null);
	}


	@Override
	public void init(NegotiationInfo infoo) {
		
		this.info = infoo;
		this.utilitySpace = infoo.getUtilitySpace();
		this.rand = new Random(infoo.getRandomSeed());
		this.timeline = infoo.getTimeline();
		this.userModel = infoo.getUserModel();
		
		// If the agent has uncertain preferences, the utility space provided to the agent by Genius will be null. 
		// In that case, the utility space is estimated with a simple heuristic so that any agent can
		// deal with preference uncertainty. This method can be overridden by the agent to provide better estimates.
		if (isUncertain())
		{
			this.utilitySpace = estimateUtilitySpace();
		}
		
		SessionData sessionData = null; 
		if (info.getPersistentData().getPersistentDataType() == PersistentDataType.SERIALIZABLE) {
			sessionData = (SessionData) info.getPersistentData().get();
		}
		else if (sessionData==null) {
			sessionData= new SessionData();
		}
		
		this.negotiationSession = new NegotiationSession(sessionData, utilitySpace, info.getTimeline(), 
				null, info.getUserModel());
		
/////////////////////////////////////////
		try {
			opponentModel.init(negotiationSession, null);
			omStrategy.init(negotiationSession, opponentModel, null);
			offeringStrategy.init(negotiationSession, opponentModel, omStrategy,
					null);
			acceptConditions.init(negotiationSession, offeringStrategy,
					opponentModel, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
/////////////////////////////////////////
		System.out.println("INIT DONE");
	}
	
	@Override 
	public Action chooseAction(List<Class<? extends Action>> possibleActions) {
		BidDetails bid;
		System.out.println("CHOOSING ACTION");

		if (this.negotiationSession.getOwnBidHistory().getHistory().isEmpty()){
			bid = offeringStrategy.determineOpeningBid();
		} else {
			bid = offeringStrategy.determineNextBid();
			System.out.println("ENDING");
			if (offeringStrategy.isEndNegotiation()) {
				return new EndNegotiation(getPartyId());
			}
		}
		if (bid == null) {
			return new Accept(getPartyId(), oppBid);
		} else {
			offeringStrategy.setNextBid(bid);
		}
		
		Actions decision = Actions.Reject;
		if (!negotiationSession.getOpponentBidHistory().getHistory()
				.isEmpty()) {
			decision = acceptConditions.determineAcceptability();
		}

		// check if the agent decided to break off the negotiation
		if (decision.equals(Actions.Break)) {
			System.out.println("send EndNegotiation");
			return new EndNegotiation(getPartyId());
		}
		// if agent does not accept, it offers the counter bid
		if (decision.equals(Actions.Reject)) {
			negotiationSession.getOwnBidHistory().add(bid);
			System.out.println("REJECT - offering new bid");
			System.out.println(bid.getBid());
			return new Offer(getPartyId(), bid.getBid());
		} else {
			System.out.println("ACCEPT");
			return new Accept(getPartyId(), oppBid);
		}
	}
	
	@Override
	public void receiveMessage(AgentID sender, Action opponentAction) {
		// 1. if the opponent made a bid
		System.out.println("RECEIVEMESSAGE called");
		if (opponentAction instanceof Offer) {
			System.out.println("RECEIVED new offer");
			oppBid = ((Offer) opponentAction).getBid();
			// 2. store the opponent's trace
			try {
				BidDetails opponentBid = new BidDetails(oppBid,
						negotiationSession.getUtilitySpace().getUtility(oppBid),
						negotiationSession.getTime());
				negotiationSession.getOpponentBidHistory().add(opponentBid);
				System.out.println("STORED opponent's trace");
			} catch (Exception e) {
				e.printStackTrace();
			}
			// 3. if there is an opponent model, receiveMessage it using the
			// opponent's
			// bid
			if (opponentModel != null && !(opponentModel instanceof NoModel)) {
				if (omStrategy.canUpdateOM()) {
					opponentModel.updateModel(oppBid);
					System.out.println("UPDATED opponent model");
				} else {
					if (!opponentModel.isCleared()) {
						opponentModel.cleanUp();
					}
				}
			}
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
		System.out.println(this.userModel);
		int nrBids = this.userModel.getBidRanking().getSize();	
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
		
		Domain domain = info.getUserModel().getDomain();
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
package i5.las2peer.services.servicePackage.algorithms;


import i5.las2peer.services.servicePackage.algorithms.utils.SlpaListenerRuleCommand;
import i5.las2peer.services.servicePackage.algorithms.utils.SlpaPopularityListenerRule;
import i5.las2peer.services.servicePackage.algorithms.utils.SlpaSpeakerRuleCommand;
import i5.las2peer.services.servicePackage.algorithms.utils.SlpaUniformSpeakerRule;
import i5.las2peer.services.servicePackage.graph.Cover;
import i5.las2peer.services.servicePackage.graph.CustomGraph;
import i5.las2peer.services.servicePackage.graph.GraphType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.Vector;
import org.la4j.vector.sparse.CompressedVector;

import y.base.Node;
import y.base.NodeCursor;

/**
 * Implements a custom extended version of the Speaker Listener Label Propagation Algorithm.
 * Handles directed and unweighted graphs. For unweighted and undirected graphs,
 * it behaves the same as the original.
 */
public class ExtendedSpeakerListenerLabelPropagationAlgorithm implements
		OcdAlgorithm {
	
	/**
	 * The size of the node memories and the number of iterations.
	 * The default value is 100.
	 */
	private int memorySize = 100;
	/**
	 * The lower bound for the relative label occurrence.
	 * Labels received by a node with a relative occurrence lower than this threshold will be ignored
	 * and do not have any influence on that nodes community memberships.
	 * The default value is 0.15.
	 */
	private double probabilityThreshold = 0.15;
	/**
	 * The speaker rule according to which a speaker decides which label to send.
	 * The default rule is the UniformSpeakerRule.
	 */
	private SlpaSpeakerRuleCommand speakerRule = new SlpaUniformSpeakerRule();
	/**
	 * The listener rule according to which a listener decides which label to accept.
	 * The default rule is the popularity listener rule.
	 */
	private SlpaListenerRuleCommand listenerRule = new SlpaPopularityListenerRule();
	
	/**
	 * Creates a standard instance of the algorithm.
	 * All attributes are assigned their default values.
	 */
	public ExtendedSpeakerListenerLabelPropagationAlgorithm() {
	}
	
	/**
	 * Creates a customized instance of the algorithm.
	 * @param memorySize Sets the memorySize. Must be greater than 0.
	 * @param probabilityThreshold  Sets the probabilityThreshold. Must be at least 0 and at most 1.
	 * Recommended are values between 0.02 and 0.1.
	 * @param speakerRule The speaker rule according to which a speaker decides which label to send.
	 * @param listenerRule The listener rule according to which a listener decides which label to accept.
	 */
	public ExtendedSpeakerListenerLabelPropagationAlgorithm(int memorySize, double probabilityThreshold,
			SlpaSpeakerRuleCommand speakerRule, SlpaListenerRuleCommand listenerRule) {
		this.memorySize = memorySize;
		this.speakerRule = speakerRule;
		this.listenerRule = listenerRule;
		this.probabilityThreshold = probabilityThreshold;
	}
	
	@Override
	public AlgorithmType getAlgorithmType() {
		return AlgorithmType.EXTENDED_SPEAKER_LISTENER_LABEL_PROPAGATION_ALGORITHM;
	}
	
	@Override
	public Map<String, String> getParameters() {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("memorySize", Integer.toString(memorySize));
		parameters.put("probabilityThreshold", Double.toString(probabilityThreshold));
		parameters.put("listenerRule", listenerRule.toString());
		parameters.put("speakerRule", speakerRule.toString());
		return parameters;
	}
	
	@Override
	public Set<GraphType> compatibleGraphTypes() {
		Set<GraphType> compatibilities = new HashSet<GraphType>();
		compatibilities.add(GraphType.WEIGHTED);
		compatibilities.add(GraphType.DIRECTED);
		return compatibilities;
	}
	
	@Override
	public Cover detectOverlappingCommunities(
			CustomGraph graph) {
		/*
		 * Initializes node memories and node order
		 */
		List<List<Integer>> memories = new ArrayList<List<Integer>>();
		List<Node> nodeOrder = new ArrayList<Node>();
		initializeCommunityDetection(graph, memories, nodeOrder);
		/*
		 * Selects each node as a listener and updates its memory until
		 * the node memories are full.
		 */
		Node listener;
		List<Integer> memory;
		for(int t=0; t+1<memorySize; t++) {
			Collections.shuffle(nodeOrder);
			for(int i=0; i<graph.nodeCount(); i++) {
				listener = nodeOrder.get(i);
				memory = memories.get(listener.index());
				memory.add(getNextLabel(graph, memories, listener));
			}
		}
		/*
		 * Returns the cover based on the node memories.
		 */
		return calculateMembershipDegrees(graph, memories);
	}
	
	protected void initializeCommunityDetection(CustomGraph graph, List<List<Integer>> memories, List<Node> nodeOrder) {
		List<Integer> memory;
		for(int i=0; i<graph.nodeCount(); i++) {
			memory = new ArrayList<Integer>();
			memory.add(i);
			memories.add(memory);
			nodeOrder.add(graph.getNodeArray()[i]);
		}
	}
	
	/*
	 * Returns the next label to be received by the listener according to the speaker
	 * and the listener rule.
	 */
	protected int getNextLabel(CustomGraph graph, List<List<Integer>> memories, Node listener) {
		Map<Node, Integer> receivedLabels = new HashMap<Node, Integer>();
		NodeCursor speakers = listener.successors();
		Node speaker;
		while(speakers.ok()) {
			speaker = speakers.node();
			receivedLabels.put(speaker, speakerRule.getLabel(graph, speaker, memories.get(speaker.index())));
			speakers.next();
		}
		return listenerRule.getLabel(graph, listener, receivedLabels);
	}
	
	/*
	 * Calculates a cover with the membership degrees for all nodes based on the node memories.
	 */
	protected Cover calculateMembershipDegrees(CustomGraph graph, List<List<Integer>> memories) {
		Matrix membershipMatrix = new Basic2DMatrix();
		List<Integer> communities = new ArrayList<Integer>();
		/*
		 * Creates a label histogram for each node based on its memory
		 * and adapts the membership matrix accordingly.
		 */
		List<Integer> memory;
		int labelCount;
		Map<Integer, Integer> histogram;
		Vector nodeMembershipDegrees;
		for(int i=0; i<memories.size(); i++) {
			memory = memories.get(i);
			labelCount = memorySize;
			histogram = getNodeHistogram(memory, labelCount);
			nodeMembershipDegrees = calculateMembershipsFromHistogram(histogram, communities, labelCount);
		    if(nodeMembershipDegrees.length() > membershipMatrix.columns()) {
				/*
				 * Adapts matrix size for new communities.
				 */
		    	membershipMatrix = membershipMatrix.resize(graph.nodeCount(), nodeMembershipDegrees.length());
		    }
		    membershipMatrix.setRow(i, nodeMembershipDegrees);
		}
		return new Cover(graph, membershipMatrix);
	}
	
	/*
	 * Creates a histogram of the occurrence frequency based on the labels in the node memory.
	 * Manipulates labelCount to track the total number of labels represented in the histogram.
	 */
	protected Map<Integer, Integer> getNodeHistogram(List<Integer> memory, int labelCount) {
		Map<Integer, Integer> histogram = new HashMap<Integer, Integer>();
		Integer maxCount = 0;
		/*
		 * Creates the histogram.
		 */
		int count;
		for (int label : memory) {
			if(histogram.containsKey(label)) {
				count = histogram.get(label).intValue();
				histogram.put(label, ++count);
				if(count > maxCount) {
					maxCount = count;
				}
			}
			else {
				histogram.put(label, 1);
			}
		}
		/*
		 * Removes labels whose occurrence frequency is below the probability threshold.
		 */
		Map.Entry<Integer, Integer> entry;
	    for(Iterator<Map.Entry<Integer, Integer>> it = histogram.entrySet().iterator(); it.hasNext(); ) {
	        entry = it.next();
	        count = entry.getValue();
	        if((double)count / (double)memorySize < probabilityThreshold && count < maxCount) {
	        	it.remove();
	        	labelCount -= count;
	        }
	    }
	    return histogram;
	}
	
	/*
	 * Returns a vector of the membership degrees of a single node, calculated from its histogram.
	 * Manipulates the communities list to identify communities.
	 */
	protected Vector calculateMembershipsFromHistogram(Map<Integer, Integer> histogram, List<Integer> communities, int labelCount) {
		Vector membershipDegrees = new CompressedVector(communities.size());
		int count;
	    for(Integer label : histogram.keySet()) {
	    	count = histogram.get(label);
	    	if(!communities.contains(label)){
	    		/*
	    		 * Adapts vector size for new communities.
	    		 */
	    		communities.add(label);
	    		membershipDegrees = membershipDegrees.resize(communities.size());
	    	}
	    	membershipDegrees.set(communities.indexOf(label), (double)count / (double)labelCount);
	    }
	    return membershipDegrees;
	}
	
}
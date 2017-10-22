package i5.las2peer.services.ocd.centrality.measures;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.la4j.inversion.GaussJordanInverter;
import org.la4j.inversion.MatrixInverter;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.sparse.CCSMatrix;
import org.la4j.vector.Vector;
import org.la4j.vector.dense.BasicVector;

import i5.las2peer.services.ocd.centrality.data.CentralityCreationLog;
import i5.las2peer.services.ocd.centrality.data.CentralityCreationType;
import i5.las2peer.services.ocd.centrality.data.CentralityMap;
import i5.las2peer.services.ocd.graphs.CustomGraph;
import i5.las2peer.services.ocd.graphs.GraphType;
import y.base.Node;
import y.base.NodeCursor;

/**
 * This implementation of alpha centrality uses a uniform external status of one for all the nodes in the graph.
 * The calculation is the same as for Katz centrality with the only difference that the matrix A is transposed.
 */
public class AlphaCentrality implements CentralityAlgorithm {
	
	private double alpha = 0.1;
	/*
	 * PARAMETER NAMES
	 */
	protected static final String ALPHA_NAME = "Alpha";
	
	public CentralityMap getValues(CustomGraph graph) throws InterruptedException {
		NodeCursor nc = graph.nodes();
		CentralityMap res = new CentralityMap(graph);
		res.setCreationMethod(new CentralityCreationLog(CentralityCreationType.ALPHA_CENTRALITY, this.getParameters(), this.compatibleGraphTypes()));
		
		int n = graph.nodeCount();
		Matrix A = graph.getNeighbourhoodMatrix();
		A = A.transpose();
		
		// Create identity matrix and vector consisting of only ones
		Matrix I = new CCSMatrix(n, n);
		Vector ones = new BasicVector(n);
		for(int i = 0; i < n; i++) {
			I.set(i, i, 1.0);
			ones.set(i, 1.0);
		}
		
		Matrix toInvert = I.subtract(A.multiply(alpha));
		MatrixInverter gauss = new GaussJordanInverter(toInvert);
		Matrix inverse = gauss.inverse();
		
		Vector resultVector = inverse.multiply(ones);
		
		while(nc.ok()) {
			if(Thread.interrupted()) {
				throw new InterruptedException();
			}
			Node node = nc.node();
			res.setNodeValue(node, resultVector.get(node.index()));
			nc.next();
		}
		return res;
	}

	@Override
	public Set<GraphType> compatibleGraphTypes() {
		Set<GraphType> compatibleTypes = new HashSet<GraphType>();
		compatibleTypes.add(GraphType.DIRECTED);
		return compatibleTypes;
	}

	@Override
	public CentralityCreationType getAlgorithmType() {
		return CentralityCreationType.ALPHA_CENTRALITY;
	}
	
	@Override
	public Map<String, String> getParameters() {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(ALPHA_NAME, Double.toString(alpha));
		return parameters;
	}
	
	@Override
	public void setParameters(Map<String, String> parameters) throws IllegalArgumentException {
		if(parameters.containsKey(ALPHA_NAME)) {
			alpha = Double.parseDouble(parameters.get(ALPHA_NAME));
			parameters.remove(ALPHA_NAME);
		}
		if(parameters.size() > 0) {
			throw new IllegalArgumentException();
		}
	}
}

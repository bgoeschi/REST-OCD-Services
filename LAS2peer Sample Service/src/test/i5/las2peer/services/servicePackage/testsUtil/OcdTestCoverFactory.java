package i5.las2peer.services.servicePackage.testsUtil;

import i5.las2peer.services.servicePackage.adapters.coverInput.CoverInputAdapter;
import i5.las2peer.services.servicePackage.adapters.coverInput.LabeledMembershipMatrixInputAdapter;
import i5.las2peer.services.servicePackage.algorithms.AlgorithmLog;
import i5.las2peer.services.servicePackage.algorithms.AlgorithmType;
import i5.las2peer.services.servicePackage.graph.Cover;
import i5.las2peer.services.servicePackage.graph.CustomGraph;
import i5.las2peer.services.servicePackage.graph.GraphType;

import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;

public class OcdTestCoverFactory {
	
	public static Cover getSawmillGroundTruth() throws Exception {
		CoverInputAdapter adapter = new LabeledMembershipMatrixInputAdapter(new FileReader(OcdTestConstants.sawmillGroundTruthLabeledMembershipMatrixInputPath));
		CustomGraph graph = OcdTestGraphFactory.getSawmillGraph();
		Cover cover = adapter.readCover(graph);
		cover.setAlgorithm(new AlgorithmLog(AlgorithmType.UNDEFINED, new HashMap<String, String>(), new HashSet<GraphType>()));
		return cover;
	}
	
}
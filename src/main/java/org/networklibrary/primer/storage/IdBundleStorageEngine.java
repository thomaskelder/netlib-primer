package org.networklibrary.primer.storage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.networklibrary.core.config.ConfigManager;
import org.networklibrary.core.storage.MultiTxStrategy;
import org.networklibrary.core.types.IdData;

public class IdBundleStorageEngine extends MultiTxStrategy<IdData> {

	protected static final Logger log = Logger.getLogger(IdBundleStorageEngine.class.getName());
	private final static String MATCH = "matchid";
	
	private Map<String,Node> nodeCache = new HashMap<String,Node>();
	
	private Index<Node> matchableIndex = null;

	
	public IdBundleStorageEngine(GraphDatabaseService graph,
			ConfigManager confMgr) {
		super(graph, confMgr);
		matchableIndex = graph.index().forNodes("matchable");
	}

	@Override
	protected void doStore(IdData curr) {
		Node currNode = getNode(curr.getMatchID(), getGraph());
		
		if(currNode == null){
			currNode = getGraph().createNode();
			addProperty(currNode, MATCH, curr.getMatchID());
			nodeCache.put(curr.getMatchID(), currNode);
			matchableIndex.add(currNode, MATCH, curr.getMatchID());
		}

		addProperty(currNode,MATCH,curr.getValue());
		matchableIndex.add(currNode, MATCH, curr.getValue());
		addProperty(currNode,curr.getPropertyName(),curr.getValue());
		
	}
	
	private void addProperty(Node currNode, String propertyName, String value) {

		if(currNode.hasProperty(propertyName)){
			String[] objs = (String[])currNode.getProperty(propertyName);

			Set<String> values = new HashSet<String>(Arrays.asList(objs));
			values.add(value);
			String[] newvalues = new String[values.size()];
			newvalues = values.toArray(newvalues);

			currNode.setProperty(propertyName, newvalues);
		} else {
			String[] values = new String[1];
			values[0] = value;
			currNode.setProperty(propertyName, values);
		}

	}
	
	protected Node getNode(String name, GraphDatabaseService g){
		Node result = nodeCache.get(name);

		if(result == null){
			IndexHits<Node> hits = g.index().forNodes("matchable").get(MATCH, name);

			if(hits.size() > 1){
				log.warning("query for name = " + name + " returned more than one hit. Defaulting to first.");
			}

			result = hits.getSingle();
			nodeCache.put(name,result);
		}

		return result;
	}

}

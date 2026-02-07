package travisdiff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TravisCIChangeBlocks {
	Map<String, List<NodeLabel>> changeList;

	public TravisCIChangeBlocks() {
		changeList = new HashMap<String, List<NodeLabel>>();

	}

	public Map<String, List<NodeLabel>> getChangeList() {
		return changeList;
	}

	public void addItemToMap(String key, NodeLabel change) {
		if(key.contains("placeholder"))
			return;
		
		if (changeList.containsKey(key)) {
			changeList.get(key).add(change);
		} else {
			List<NodeLabel> list = new ArrayList<>();
			list.add(change);
			changeList.put(key, list);
		}

	}

	public List<NodeLabel> getChangeList(String key) {
		if (changeList.containsKey(key)) {
			return changeList.get(key);
		} else {
			return null;
		}
	}

}

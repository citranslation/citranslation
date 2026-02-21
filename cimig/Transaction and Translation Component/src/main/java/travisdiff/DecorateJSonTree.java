package travisdiff;

import java.util.List;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

public class DecorateJSonTree {

	public String getFixedField(Action action) {
		Tree treenode = action.getNode();
		Tree treefield = action.getNode();
		String fieldlabel = "";

		while (treenode != null) {
			if(treenode.getType().toString().equals("FIELD"))
			{
				treefield=treenode;
			}
//			if(treenode.getType().toString().equals("OBJECT"))
//			{
//				break;
//			}
			treenode = treenode.getParent();
		}
		
		List<Tree> children=treefield.getChildren();
		
		if(children.size()>1)
		{
			fieldlabel=children.get(0).getLabel();
		}

		return fieldlabel;
	}
	
	public String getJsonField(Action action) {
		Tree treenode = action.getNode();
		String fieldlabel = "";

		while (treenode != null) {
			if(treenode.getType().toString().equals("FIELD"))
			{
				break;
			}
			treenode = treenode.getParent();
		}
		
		List<Tree> children=treenode.getChildren();
		
		if(children.size()>1)
		{
			fieldlabel=children.get(0).getLabel();
		}

		return fieldlabel;
	}
	
	public String getJsonField(Tree action) {
		Tree treenode = action;
		String fieldlabel = "";

		while (treenode != null) {
			if(treenode.getType().toString().equals("FIELD"))
			{
				break;
			}
			treenode = treenode.getParent();
		}
		
		List<Tree> children=treenode.getChildren();
		
		if(children.size()>1)
		{
			fieldlabel=children.get(0).getLabel();
		}

		return fieldlabel;
	}

}

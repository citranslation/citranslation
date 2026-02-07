package travisdiff;

import java.util.ArrayList;
import java.util.List;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;


public class NodeLabel {
	Tree node;
	String label;
	String strAction;
	Action action;
	List<String> cmds;
	
	
	public NodeLabel(Tree node, String label,String straction,Action action,List<String> changecmds)
	{
		this.cmds=new ArrayList<>();
		
		this.node=node;
		this.label=label;
		this.strAction=straction;
		this.action=action;		
		cmds.addAll(changecmds);
	}
	
	public Tree getNode() {
		return node;
	}

	public void setNode(Tree node) {
		this.node = node;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	public String getStrAction() {
		return strAction;
	}

	public void setStrAction(String action) {
		this.strAction = action;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}
	
	public List<String> getCmds() {
		return cmds;
	}

	public void setCmds(List<String> cmds) {
		this.cmds = cmds;
	}
	
	public String getCmdsString()
	{
		if(cmds!=null && cmds.size()>0)
			return cmds.toString();
		else
			return "";
	}



}

package travisdiff;

import java.io.File;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;


public class TravisCIDiffGenerator {
	public EditScript extractTravisFileChange(File f1,File f2) {
		
		TravisCITree travistree=new TravisCITree();
		 Tree prevtree=travistree.getTravisCITree(f1.toString());
		 Tree curtree=travistree.getTravisCITree(f2.toString());
		 
//		 Matcher m = Matchers.getInstance().getMatcher();
//		 MappingStore mappings = m.match(prevtree, curtree);
//
//		 EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
//		 EditScript actions = editScriptGenerator.computeActions(mappings);
//		 
//		 System.out.println("test");
		 
		 Matcher defaultMatcher = Matchers.getInstance().getMatcher(); // retrieves the default matcher
		 MappingStore mappings = defaultMatcher.match(prevtree, curtree); // computes the mappings between the trees
		 EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator(); // instantiates the simplified Chawathe script generator
		 EditScript actions = editScriptGenerator.computeActions(mappings); // computes the edit script

		 //System.out.println("test");
		 
		 DecorateJSonTree decojson=new DecorateJSonTree();
		 
		 for(Action action:actions)
		 {
			 String strfield=decojson.getJsonField(action);
			 strfield = strfield.replaceAll("\"", "");
			 //System.out.println(strfield);
			 action.getNode().setMetadata("json_parent", strfield);
			 
			 String strlocation=decojson.getFixedField(action);
			 strlocation = strlocation.replaceAll("\"", "");
			 action.getNode().setMetadata("json_fixlocation", strfield);
		 }
		 
		 
		 //System.out.println("new test");

		 return actions;
		
	}

}

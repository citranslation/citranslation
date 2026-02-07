package travisdiff;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.assimbly.docconverter.DocConverter;

import com.github.gumtreediff.gen.antlr3.json.AntlrJsonTreeGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

import config.Config;
import util.FileWriterUtil;

public class TravisCITree {
	
	public Tree getTravisCITree(String yamlfile) {
		FileWriterUtil fwriter = new FileWriterUtil();
		String yaml;
		String json = null;
		try {
			yaml = DocConverter.convertFileToString(yamlfile);
			if(yaml.length()<1)
				json="{\"placeholder\":\"John\"}";
			else
				json = DocConverter.convertYamlToJson(yaml);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String filename=Config.patchDir+"testj1.json";
		
		fwriter.writetoFile(filename, json);

		/*
		 * ObjectMapper objectMapper = new ObjectMapper();
		 * 
		 * JsonNode jsonNode = null;
		 * 
		 * try {
		 * 
		 * jsonNode = objectMapper.readTree(json); System.out.println(jsonNode);
		 * 
		 * } catch (IOException e) { e.printStackTrace(); }
		 */

		TreeContext tc = null;

		try {
			
//			tc = new AntlrJsonTreeGenerator().generateFrom().charset("UTF-8")
//					.stream(getClass().getResourceAsStream(filename));
			//tc = new AntlrJsonTreeGenerator().generate(targetReader);
			//Run.initGenerators(); // registers the available parsers
			String file = filename;
			InputStream is = new FileInputStream(file);

			tc = new AntlrJsonTreeGenerator().generateFrom().charset("UTF-8")
             .stream(is);
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		File ftemp=new File(filename);
		
		if(ftemp.exists())
			ftemp.delete();
		
		

		Tree tree = tc.getRoot();

		return tree;
	}

}

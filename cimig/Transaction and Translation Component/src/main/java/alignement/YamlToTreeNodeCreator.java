package alignement;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

import org.yaml.snakeyaml.Yaml;

public class YamlToTreeNodeCreator {

	  public static DefaultMutableTreeNode createTreeNodeFromYaml(String yamlFile) {
	      Yaml yaml = new Yaml();
	      System.out.println(yamlFile);
	      try (InputStream in = YamlToTreeNodeCreator.class.getResourceAsStream(yamlFile)) {
	          Iterable<Object> itr = yaml.loadAll(in);
	          for (Object o : itr) {
	              if (o instanceof Map) {
	                  Map<?, ?> map = (Map) o;
	                  for (Map.Entry<?, ?> entry : map.entrySet()) {
	                      DefaultMutableTreeNode root = new DefaultMutableTreeNode(entry.getKey());
	                      createTreeNode(entry.getValue(), root);
	                      return root;
	                  }
	              }
	          }
	      } catch (IOException e) {
	          throw new RuntimeException(e);
	      }
	      return null;
	  }

	  public static void createTreeNode(Object o, DefaultMutableTreeNode parentNode) {
	      if (o instanceof Map) {
	          Map<?, ?> map = (LinkedHashMap) o;
	          for (Map.Entry<?, ?> entry : map.entrySet()) {
	              DefaultMutableTreeNode node = new DefaultMutableTreeNode(entry.getKey());
	              parentNode.add(node);
	              createTreeNode(entry.getValue(), node);
	          }
	      } else if (o instanceof List) {
	          List<?> list = (List<?>) o;
	          for (Object e : list) {
	              createTreeNode(e, parentNode);
	          }
	      } else if (o instanceof String) {
	          DefaultMutableTreeNode node = new DefaultMutableTreeNode(o);
	          parentNode.add(node);
	      }
	  }
	}

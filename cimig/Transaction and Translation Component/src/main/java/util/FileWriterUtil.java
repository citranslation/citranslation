package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileWriterUtil {
	
	public void writetoFile(String filepath, String content) 
	{
		String str = "Hello";
	    BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(filepath));
			writer.write(content);
			writer.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}    
	   
	}
}

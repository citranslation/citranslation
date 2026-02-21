package config;

public class ModelConfig {
	public final static String optionalParameters = "CCF:true CBS:true CCFmin:1 CCFmax:6 CCFsup:2 splitMethod:0 splitLength:4 minPredictionRatio:1 noiseRatio:1.0";
	// The following line is to set optional parameters for the prediction model. 
	// We want to 
	
	// activate the CCF and CBS strategies which generally improves its performance (see paper)
	// Here is a brief description of the parameter used in the above line:
	//  CCF:true  --> activate the CCF strategy 
	//  CBS:true -->  activate the CBS strategy
	//  CCFmax:6 --> indicate that the CCF strategy will not use pattern having more than 6 items
	//  CCFsup:2 --> indicate that a pattern is frequent for the CCF strategy if it appears in at least 2 sequences
	//  splitMethod:0 --> 0 : indicate to not split the training sequences    1: indicate to split the sequences
	//  splitLength:4  --> indicate to split sequence to keep only 4 items, if splitting is activated
	//  minPredictionRatio:1.0  -->  the amount of sequences or part of sequences that should match to make a prediction, expressed as a ratio
	//  noiseRatio:1.0  -->   ratio of items to remove in a sequence per level (see paper). 
}

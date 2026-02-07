package config;

public class Config {
	
	 public final static String patchDir="patchDir";
	 public final static double genericCutOff=0.9;
	 public final static int cond_total_for_generic_statement=5;
	 
	 public final static int occurencesToIncludeInStatement=7;
	 public final static int refCountForSimilarityTranslation=5;
	 
	 public final static int oneToOneTranslationToFetch = 3;	
	 public final static int manyToManyTranslationToFetch = 5;
	 
	 public final static String pathToLookupTable = "/home/alaa/eclipse-workspace/travis2ga/lookUpTable.bin";
	 public final static String pathToTransactionalDB = "/home/alaa/eclipse-workspace/travis2ga/transactional_database_just_numbers.txt";
	 public final static String pathToSequentialDB = "/home/alaa/eclipse-workspace/travis2ga/sequence_database.txt";
	 
	 public final static int minSupp = 100;
	 
	 public final static double minConf=0.9;
	 
	 public final static double minSuppBatch = 0.06;
	 public final static double minLift = 0.9;
	 public final static int maxAntecedantLength = 7;
	 public final static int maxConsequentLength =1;
	 
}

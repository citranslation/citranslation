package util;

import gamigration.travis2ga.translation_engine.AprioriRule;

import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class TranslationUtils {
    public static boolean AprioriRulesAreEqual(AprioriRule r1 , AprioriRule r2){
        try {
            JSONAssert.assertEquals(r1.travis_node, r2.travis_node, JSONCompareMode.STRICT);
            JSONAssert.assertEquals(r1.github_node, r2.github_node, JSONCompareMode.STRICT);
            return true;
        }
        catch (JSONException e){
            e.printStackTrace();
            return false;
        }
        catch (AssertionError e){
            return false;
        }
    }
}

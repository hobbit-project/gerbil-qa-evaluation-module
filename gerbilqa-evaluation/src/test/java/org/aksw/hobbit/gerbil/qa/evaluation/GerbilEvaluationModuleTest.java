package org.aksw.hobbit.gerbil.qa.evaluation;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.aksw.gerbil.semantic.vocabs.GERBIL;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.jamonapi.utils.FileUtils;

@RunWith(Parameterized.class)
public class GerbilEvaluationModuleTest extends GerbilEvaluationModule {

	private static final String ASK_ERROR_QUERY = "ASK WHERE {?res <"+GERBIL.errorCount.getURI()+"> 1}";
	private static final String ASK_EQUAL_QUERY = "ASK WHERE {?res <"+GERBIL.macroPrecision.getURI()+"> \"1.0\"^^<http://www.w3.org/2001/XMLSchema#double> . ?res <"+GERBIL.macroRecall.getURI()+"> \"1.0\"^^<http://www.w3.org/2001/XMLSchema#double> ; <"+GERBIL.macroF1.getURI()+"> \"1.0\"^^<http://www.w3.org/2001/XMLSchema#double> ; <"
			+GERBIL.microPrecision.getURI()+"> \"1.0\"^^<http://www.w3.org/2001/XMLSchema#double> ; <"+GERBIL.microRecall.getURI()+"> \"1.0\"^^<http://www.w3.org/2001/XMLSchema#double> ; <"+GERBIL.microF1.getURI()+"> \"1.0\"^^<http://www.w3.org/2001/XMLSchema#double> }";
	private static final String ASK_NOT_EQUAL_QUERY = "ASK WHERE {?res <"+GERBIL.macroPrecision.getURI()+"> \"0.0\"^^<http://www.w3.org/2001/XMLSchema#double> ; <"+GERBIL.macroRecall.getURI()+"> \"0.0\"^^<http://www.w3.org/2001/XMLSchema#double>; <"+GERBIL.macroF1.getURI()+"> \"0.0\"^^<http://www.w3.org/2001/XMLSchema#double> ; <"
			+GERBIL.microPrecision.getURI()+"> \"0.0\"^^<http://www.w3.org/2001/XMLSchema#double> ; <"+GERBIL.microRecall.getURI()+"> \"0.0\"^^<http://www.w3.org/2001/XMLSchema#double>; <"+GERBIL.microF1.getURI()+"> \"0.0\"^^<http://www.w3.org/2001/XMLSchema#double> }";
	
	private static final String ASK_EXP_CONTAINS_RECV_QUERY = "ASK WHERE {?res <"+GERBIL.macroPrecision.getURI()+"> \"1.0\"^^<http://www.w3.org/2001/XMLSchema#double> ; <"+GERBIL.macroRecall.getURI()+"> \"0.5\"^^<http://www.w3.org/2001/XMLSchema#double>; <"+GERBIL.macroF1.getURI()+"> \"0.6666666666666666\"^^<http://www.w3.org/2001/XMLSchema#double>; <"
			+GERBIL.microPrecision.getURI()+"> \"1.0\"^^<http://www.w3.org/2001/XMLSchema#double> ; <"+GERBIL.microRecall.getURI()+"> \"0.5\"^^<http://www.w3.org/2001/XMLSchema#double>; <"+GERBIL.microF1.getURI()+">  \"0.6666666666666666\"^^<http://www.w3.org/2001/XMLSchema#double> }";
	
	private static final String ASK_RECV_CONTAINS_EXP_QUERY = "ASK WHERE {?res <"+GERBIL.macroPrecision.getURI()+"> \"0.5\"^^<http://www.w3.org/2001/XMLSchema#double> ; <"+GERBIL.macroRecall.getURI()+"> \"1.0\"^^<http://www.w3.org/2001/XMLSchema#double>;  <"+GERBIL.macroF1.getURI()+"> \"0.6666666666666666\"^^<http://www.w3.org/2001/XMLSchema#double>; <"
			+GERBIL.microPrecision.getURI()+"> \"0.5\"^^<http://www.w3.org/2001/XMLSchema#double> ; <"+GERBIL.microRecall.getURI()+"> \"1.0\"^^<http://www.w3.org/2001/XMLSchema#double>; <"+GERBIL.microF1.getURI()+"> \"0.6666666666666666\"^^<http://www.w3.org/2001/XMLSchema#double> }";
	
	private static final String ASK_MIX_QUERY = "ASK WHERE {?res <"+GERBIL.macroPrecision.getURI()+"> \"0.75\"^^<http://www.w3.org/2001/XMLSchema#double> ; <"+GERBIL.macroRecall.getURI()+"> \"1.0\"^^<http://www.w3.org/2001/XMLSchema#double>; <"+GERBIL.macroF1.getURI()+"> \"0.8333333333333333\"^^<http://www.w3.org/2001/XMLSchema#double>; <"
			+GERBIL.microPrecision.getURI()+"> \"0.6666666666666666\"^^<http://www.w3.org/2001/XMLSchema#double> ; <"+GERBIL.microRecall.getURI()+"> \"1.0\"^^<http://www.w3.org/2001/XMLSchema#double>; <"+GERBIL.microF1.getURI()+"> \"0.8\"^^<http://www.w3.org/2001/XMLSchema#double> }";
	
	private String askQuery;
	private String[] expected;
	private String[] received;
	

	@Parameters
    public static Collection<Object[]> data() {
        List<Object[]> testConfigs = new ArrayList<Object[]>();
        //CHECK IF EVALUATION IS CORRECT
        testConfigs.add(new Object[] {ASK_EQUAL_QUERY, 
        		new String[]{"src/test/resources/json/test1.json","src/test/resources/json/test2.json"},
        		new String[]{"src/test/resources/json/test1.json","src/test/resources/json/test2.json"}});
        testConfigs.add(new Object[] {ASK_EQUAL_QUERY, 
        		new String[]{"src/test/resources/json/test1.json","src/test/resources/json/test8.json"},
        		new String[]{"src/test/resources/json/test1.json","src/test/resources/json/test8.json"}});
        testConfigs.add(new Object[] {ASK_NOT_EQUAL_QUERY, 
        		new String[]{"src/test/resources/json/test1.json","src/test/resources/json/test8.json"},
        		new String[]{"src/test/resources/json/test1.json","src/test/resources/json/test9.json"}});
        testConfigs.add(new Object[] {ASK_NOT_EQUAL_QUERY,
        		new String[]{"src/test/resources/json/test1.json","src/test/resources/json/test2.json"},
        		new String[]{"src/test/resources/json/test2.json","src/test/resources/json/test1.json"}});
        testConfigs.add(new Object[] {ASK_EXP_CONTAINS_RECV_QUERY,
        		new String[]{"src/test/resources/json/test3.json","src/test/resources/json/test4.json"},
        		new String[]{"src/test/resources/json/test1.json","src/test/resources/json/test2.json"}});
        testConfigs.add(new Object[] {ASK_RECV_CONTAINS_EXP_QUERY,
        		new String[]{"src/test/resources/json/test1.json","src/test/resources/json/test2.json"},
        		new String[]{"src/test/resources/json/test3.json","src/test/resources/json/test4.json"}});
        testConfigs.add(new Object[] {ASK_MIX_QUERY,
        		new String[]{"src/test/resources/json/test1.json","src/test/resources/json/test2.json"},
        		new String[]{"src/test/resources/json/test1.json","src/test/resources/json/test4.json"}});
        testConfigs.add(new Object[] {ASK_MIX_QUERY,
        		new String[]{"src/test/resources/json/test1.json","src/test/resources/json/test2.json"},
        		new String[]{"src/test/resources/json/test1.json","src/test/resources/json/test4.json"}});
        //CHECK ERROR FUNCTION
        testConfigs.add(new Object[] {ASK_ERROR_QUERY,
        		new String[]{"src/test/resources/json/test1.json"},
        		new String[]{null}});
        //CHECK LITERAL 2 RESOURCE MAPPING
        testConfigs.add(new Object[] {ASK_EQUAL_QUERY,
        		new String[]{"src/test/resources/json/test5.json"},
        		new String[]{"src/test/resources/json/test6.json"}});
        //CHECK IF  RESOURCES ARE CORRECT
        testConfigs.add(new Object[] {ASK_EQUAL_QUERY,
        		new String[]{"src/test/resources/json/test5.json"},
        		new String[]{"src/test/resources/json/test5.json"}});
        testConfigs.add(new Object[] {ASK_NOT_EQUAL_QUERY,
        		new String[]{"src/test/resources/json/test5.json"},
        		new String[]{"src/test/resources/json/test7.json"}});
        return testConfigs;
    }
	
	
	public GerbilEvaluationModuleTest(String askQuery, String[] expected, String[] received){
		super();
		experimentUri="http://test.com";
		this.askQuery=askQuery;
		this.expected=expected;
		this.received=received;
	}
	
	@Test
	public void checkEvaluation() throws Exception{
		for(int i=0; i<expected.length;i++){
			byte[] recv = new byte[0];
			if(received[i]!=null){
				recv = load(received[i]);
			}
			this.evaluateResponse(load(expected[i]), recv, 0L, 0L);
		}
		Model m = this.summarizeEvaluation();
		QueryExecution exec = QueryExecutionFactory.create(askQuery, m);
		assertTrue(exec.execAsk());
		this.close();
	}

		
	
	private byte[] load(String file) throws FileNotFoundException, IOException {
		return FileUtils.getFileContents(file).getBytes();
	}

	
}

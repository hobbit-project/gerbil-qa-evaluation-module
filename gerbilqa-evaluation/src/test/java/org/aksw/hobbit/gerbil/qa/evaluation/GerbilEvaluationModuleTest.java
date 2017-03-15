package org.aksw.hobbit.gerbil.qa.evaluation;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.aksw.gerbil.semantic.vocabs.GERBIL;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.junit.Test;

import com.jamonapi.utils.FileUtils;

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
	

	@Test
	public void checkEvaluation() throws Exception {
		GerbilEvaluationModuleTest eval = new GerbilEvaluationModuleTest();
		eval.experimentUri="http://test.com";
		byte[] expectedData = null, receivedData = null;
		
		expectedData = load("src/test/resources/json/test1.json");
		receivedData = load("src/test/resources/json/test1.json");
		eval.evaluateResponse(expectedData, receivedData, 0L, 0L);
		expectedData = load("src/test/resources/json/test2.json");
		receivedData = load("src/test/resources/json/test2.json");
		eval.evaluateResponse(expectedData, receivedData, 0L, 0L);
		Model m = eval.summarizeEvaluation();
		QueryExecution exec = QueryExecutionFactory.create(ASK_EQUAL_QUERY, m);
		assertTrue(exec.execAsk());
		
		eval.close();
		eval = new GerbilEvaluationModuleTest();
		eval.experimentUri="http://test.com";
		//set expData != recvData
		expectedData = load("src/test/resources/json/test1.json");
		receivedData = load("src/test/resources/json/test2.json");
		eval.evaluateResponse(expectedData, receivedData, 0L, 0L);
		expectedData = load("src/test/resources/json/test2.json");
		receivedData = load("src/test/resources/json/test1.json");
		eval.evaluateResponse(expectedData, receivedData, 0L, 0L);
		m = eval.summarizeEvaluation();
		exec = QueryExecutionFactory.create(ASK_NOT_EQUAL_QUERY, m);
		assertTrue(exec.execAsk());
		
		eval.close();
		eval = new GerbilEvaluationModuleTest();
		eval.experimentUri="http://test.com";
		//set expData.contains(recvData)
		expectedData = load("src/test/resources/json/test3.json");
		receivedData = load("src/test/resources/json/test1.json");
		eval.evaluateResponse(expectedData, receivedData, 0L, 0L);
		//set expData.contains(recvData)
		expectedData = load("src/test/resources/json/test4.json");
		receivedData = load("src/test/resources/json/test2.json");
		eval.evaluateResponse(expectedData, receivedData, 0L, 0L);
		m = eval.summarizeEvaluation();
		exec = QueryExecutionFactory.create(ASK_EXP_CONTAINS_RECV_QUERY, m);
		assertTrue(exec.execAsk());
		
		eval.close();
		eval = new GerbilEvaluationModuleTest();
		eval.experimentUri="http://test.com";
		//set recvData.contains(expData)
		expectedData = load("src/test/resources/json/test1.json");
		receivedData = load("src/test/resources/json/test3.json");
		eval.evaluateResponse(expectedData, receivedData, 0L, 0L);
		expectedData = load("src/test/resources/json/test2.json");
		receivedData = load("src/test/resources/json/test4.json");
		eval.evaluateResponse(expectedData, receivedData, 0L, 0L);
		m = eval.summarizeEvaluation();
		exec = QueryExecutionFactory.create(ASK_RECV_CONTAINS_EXP_QUERY, m);
		assertTrue(exec.execAsk());
		
		eval.close();
		eval = new GerbilEvaluationModuleTest();
		eval.experimentUri="http://test.com";
		//set expData = recvData
		expectedData = load("src/test/resources/json/test1.json");
		receivedData = load("src/test/resources/json/test1.json");
		eval.evaluateResponse(expectedData, receivedData, 0L, 0L);
		//set recvData.contains(expData)
		expectedData = load("src/test/resources/json/test2.json");
		receivedData = load("src/test/resources/json/test4.json");
		eval.evaluateResponse(expectedData, receivedData, 0L, 0L);
		m = eval.summarizeEvaluation();
		exec = QueryExecutionFactory.create(ASK_MIX_QUERY, m);
		assertTrue(exec.execAsk());
		eval.close();
	}
		
	
	private byte[] load(String file) throws FileNotFoundException, IOException {
		return FileUtils.getFileContents(file).getBytes();
	}
	
	
	@Test
	public void checkResources() throws Exception{
		GerbilEvaluationModuleTest eval = new GerbilEvaluationModuleTest();
		eval.experimentUri="http://test.com";
		byte[] expectedData = null, receivedData = null;
		
		expectedData = load("src/test/resources/json/test5.json");
		receivedData = load("src/test/resources/json/test5.json");
		eval.evaluateResponse(expectedData, receivedData, 0L, 0L);
		Model m = eval.summarizeEvaluation();
		QueryExecution exec = QueryExecutionFactory.create(ASK_EQUAL_QUERY, m);
		assertTrue(exec.execAsk());
		eval.close();
		
		eval = new GerbilEvaluationModuleTest();
		eval.experimentUri="http://test.com";
		
		expectedData = load("src/test/resources/json/test5.json");
		receivedData = load("src/test/resources/json/test7.json");
		eval.evaluateResponse(expectedData, receivedData, 0L, 0L);
		m = eval.summarizeEvaluation();
		exec = QueryExecutionFactory.create(ASK_NOT_EQUAL_QUERY, m);
		assertTrue(exec.execAsk());
		eval.close();
	}
	
	@Test
	public void checkLiteral2Resource() throws Exception{
		GerbilEvaluationModuleTest eval = new GerbilEvaluationModuleTest();
		eval.experimentUri="http://test.com";
		byte[] expectedData = null, receivedData = null;
		
		expectedData = load("src/test/resources/json/test5.json");
		receivedData = load("src/test/resources/json/test6.json");
		eval.evaluateResponse(expectedData, receivedData, 0L, 0L);
		Model m = eval.summarizeEvaluation();
		QueryExecution exec = QueryExecutionFactory.create(ASK_EQUAL_QUERY, m);
		assertTrue(exec.execAsk());
		eval.close();
	}

	@Test
	public void checkErrorEvaluation() throws Exception {
		GerbilEvaluationModule eval = new GerbilEvaluationModule();
		String test = "this message should be irrelevant";
		eval.evaluateResponse(test.getBytes(), new byte[0], 0L, 0L);
		Model m = eval.summarizeEvaluation();
		QueryExecution exec = QueryExecutionFactory.create(ASK_ERROR_QUERY, m);
		assertTrue(exec.execAsk());
		eval.close();
	}
}

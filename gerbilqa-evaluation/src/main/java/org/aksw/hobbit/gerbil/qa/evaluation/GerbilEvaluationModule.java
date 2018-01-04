package org.aksw.hobbit.gerbil.qa.evaluation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.aksw.gerbil.config.GerbilConfiguration;
import org.aksw.gerbil.dataset.converter.Literal2ResourceManager;
import org.aksw.gerbil.dataset.converter.impl.SPARQLBasedLiteral2Resource;
import org.aksw.gerbil.matching.EvaluationCounts;
import org.aksw.gerbil.matching.impl.QAMatchingsCounter;
import org.aksw.gerbil.qa.QAUtils;
import org.aksw.gerbil.qa.datatypes.AnswerSet;
import org.aksw.gerbil.semantic.kb.SimpleWhiteListBasedUriKBClassifier;
import org.aksw.gerbil.semantic.sameas.SameAsRetriever;
import org.aksw.gerbil.semantic.sameas.impl.index.IndexBasedSameAsRetriever;
import org.aksw.gerbil.semantic.vocabs.GERBIL;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.load.json.EJQuestionFactory;
import org.aksw.qa.commons.load.json.ExtendedQALDJSONLoader;
import org.aksw.qa.commons.load.json.QaldJson;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.core.components.AbstractEvaluationModule;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.vocab.HOBBIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.Arrays;

public class GerbilEvaluationModule extends AbstractEvaluationModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(GerbilEvaluationModule.class);

	private static final String ENDPOINT_KEY = "org.aksw.gerbil.dataset.converter.domain";

	private static final String WELL_KNOWN_KBS = "org.aksw.gerbil.evaluate.DefaultWellKnownKB";

	private String qLang = "en";

	private Literal2ResourceManager converterManager = new Literal2ResourceManager();
	private QAMatchingsCounter counter;
	private String endpoint;
	private EvaluationCounts globalCounts = new EvaluationCounts();
	private double[] macro = new double[] { 0, 0, 0 };
	private int size = 0;
	private int errorCount = 0;
	
	private int question = 0;

	private String[] kbs;

	public GerbilEvaluationModule() {
		super();
		this.kbs = GerbilConfiguration.getInstance().getStringArray(WELL_KNOWN_KBS);
		this.endpoint = GerbilConfiguration.getInstance().getString(ENDPOINT_KEY);
		
		converterManager
				.registerLiteral2Resource(new SPARQLBasedLiteral2Resource(
						endpoint));
//		SameAsRetriever retriever = new IndexBasedSameAsRetriever();
		counter = new QAMatchingsCounter(null, new UrlValidator(),
				new SimpleWhiteListBasedUriKBClassifier(this.kbs),
				converterManager);
	}

	
	@SuppressWarnings("rawtypes")
	@Override
	protected void evaluateResponse(byte[] expectedData, byte[] receivedData,
			long taskSentTimestamp, long responseReceivedTimestamp)
			throws Exception {
		
		List<AnswerSet> recvAnswers = getMarkings(receivedData, false);
		List<AnswerSet> goldenStandard = getMarkings(expectedData, true);
		
		
		LOGGER.info(question+" recv: "+recvAnswers+"\n");
	    LOGGER.info(question+" expc: "+goldenStandard+"\n");

		EvaluationCounts counts = counter.countMatchings(recvAnswers,
				goldenStandard);
		LOGGER.info(question+" counts: "+counts);
		question++;
		globalCounts.add(counts);
		addMacro(macro, calculateMeasures(counts));
		size++;
	}
	

	@SuppressWarnings("rawtypes")
	private List<AnswerSet> getMarkings(byte[] content, boolean setLang) {
		if (content.length == 0) {
			errorCount++;
			return new ArrayList<AnswerSet>();
		}
		QaldJson qald =null;
		try{
		qald = (QaldJson) ExtendedQALDJSONLoader.readJson(
				new ByteArrayInputStream(content), QaldJson.class);
		}catch(Exception e){
			LOGGER.error("could not load returned qald json", e);
			errorCount++;
			return new ArrayList<AnswerSet>();
		}
		IQuestion question = EJQuestionFactory.getQuestionsFromQaldJson(qald)
				.get(0);
//		if (setLang) {
//			qLang = question.getLanguageToQuestion().keySet().iterator().next();
//			converterManager.setQuestionLanguage(qLang);
//		}
		Document doc = QAUtils.translateQuestion(question, null, qLang);
		return doc.getMarkings(AnswerSet.class);
	}

	private void addMacro(double[] global, double[] add) {
		for (int i = 0; i < global.length; i++) {
			global[i] += add[i];
		}
	}

	private double[] getFinalMacro(double[] macro, int size) {
		return new double[] { macro[0] / size, macro[1] / size, macro[2] / size };
	}

	@Override
	protected Model summarizeEvaluation() throws Exception {
		double[] micro = calculateMeasures(globalCounts);
		double[] macro = getFinalMacro(this.macro, size);
		LOGGER.info("final micro: prec: "+micro[0]+", recall: "+micro[1]+", f1: "+micro[2]);
		LOGGER.info("final macro: prec: "+macro[0]+", recall: "+macro[1]+", f1: "+macro[2]);

		// All tasks/responsens have been evaluated. Summarize the results,
		// write them into a Jena model and send it to the benchmark controller.
		Model model = createDefaultModel();
		Resource experimentResource = model.getResource(experimentUri);
		Property noQuestions = model.createProperty(GERBIL.getURI(), "noOfQuestions");
		model.add(experimentResource, RDF.type, HOBBIT.Experiment);

		model.addLiteral(experimentResource, GERBIL.macroPrecision, macro[0]);
		model.addLiteral(experimentResource, GERBIL.macroRecall, macro[1]);
		model.addLiteral(experimentResource, GERBIL.macroF1, macro[2]);
		model.addLiteral(experimentResource, GERBIL.microPrecision, micro[0]);
		model.addLiteral(experimentResource, GERBIL.microRecall, micro[1]);
		model.addLiteral(experimentResource, GERBIL.microF1, micro[2]);
		model.addLiteral(experimentResource, GERBIL.errorCount, errorCount);
		model.addLiteral(experimentResource, noQuestions, size);
		
		return model;
	}

	@Override
	public void close() throws IOException {

		// Always close the super class after yours!
		super.close();
	}

	private double[] calculateMeasures(EvaluationCounts counts) {
		double precision, recall, F1_score;
		if (counts.truePositives == 0) {
			if ((counts.falsePositives == 0) && (counts.falseNegatives == 0)) {
				// If there haven't been something to find and nothing has been
				// found --> everything is great
				precision = 1.0;
				recall = 1.0;
				F1_score = 1.0;
			} else {
				// The annotator found no correct ones, but made some mistake
				// --> that is bad
				precision = 0.0;
				recall = 0.0;
				F1_score = 0.0;
			}
		} else {
			precision = (double) counts.truePositives
					/ (double) (counts.truePositives + counts.falsePositives);
			recall = (double) counts.truePositives
					/ (double) (counts.truePositives + counts.falseNegatives);
			F1_score = (2 * precision * recall) / (precision + recall);
		}
		return new double[] { precision, recall, F1_score };
	}

}

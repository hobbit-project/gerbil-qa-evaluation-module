package org.aksw.hobbit.gerbil.qa.evaluation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.aksw.gerbil.dataset.converter.Literal2ResourceManager;
import org.aksw.gerbil.dataset.converter.impl.SPARQLBasedLiteral2Resource;
import org.aksw.gerbil.matching.EvaluationCounts;
import org.aksw.gerbil.matching.impl.QAMatchingsCounter;
import org.aksw.gerbil.qa.QAUtils;
import org.aksw.gerbil.qa.datatypes.AnswerSet;
import org.aksw.gerbil.semantic.kb.SimpleWhiteListBasedUriKBClassifier;
import org.aksw.gerbil.semantic.vocabs.GERBIL;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.load.json.EJQuestionFactory;
import org.aksw.qa.commons.load.json.ExtendedQALDJSONLoader;
import org.aksw.qa.commons.load.json.QaldJson;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.core.components.AbstractEvaluationModule;
import org.hobbit.vocab.HOBBIT;

public class GerbilEvaluationModule extends AbstractEvaluationModule {

	private String qLang = "en";

	private Literal2ResourceManager converterManager = new Literal2ResourceManager();
	private QAMatchingsCounter counter;
	private String endpoint = "http://dbpedia.org/sparql?default-graph-uri=http%3A%2F%2Fdbpedia.org";

	private EvaluationCounts globalCounts = new EvaluationCounts();
	private double[] macro = new double[] { 0, 0, 0 };
	private int size = 0;
	private int errorCount = 0;

	public GerbilEvaluationModule() {
		super();
		converterManager
				.registerLiteral2Resource(new SPARQLBasedLiteral2Resource(
						endpoint));
		//TODO instead of hard coding the KNOWN KBS use a properties file!
		counter = new QAMatchingsCounter(null, new UrlValidator(),
				new SimpleWhiteListBasedUriKBClassifier(
						"http://dbpedia.org/resource/",
						"http://dbpedia.org/ontology/",
						"http://ontologydesignpatterns.org/ont/dul/",
						"http://www.ontologydesignpatterns.org/ont/d0.owl"),
				converterManager);
	}

	@Override
	protected void evaluateResponse(byte[] expectedData, byte[] receivedData,
			long taskSentTimestamp, long responseReceivedTimestamp)
			throws Exception {
		if (receivedData.length == 0) {
			// Error occured
			errorCount++;
			return;
		}

		QaldJson expectedQald = (QaldJson) ExtendedQALDJSONLoader.readJson(
				new ByteArrayInputStream(expectedData), QaldJson.class);
		QaldJson receivedQald = (QaldJson) ExtendedQALDJSONLoader.readJson(
				new ByteArrayInputStream(receivedData), QaldJson.class);
		IQuestion expectedQuestion = EJQuestionFactory
				.getQuestionsFromQaldJson(expectedQald).get(0);
		IQuestion receivedQuestion = EJQuestionFactory
				.getQuestionsFromQaldJson(receivedQald).get(0);
		// TODO is this always responding the right Language? Should be
		qLang = expectedQuestion.getLanguageToQuestion().keySet().iterator()
				.next();
		converterManager.setQuestionLanguage(qLang);

		Document expDoc = QAUtils.translateQuestion(expectedQuestion, null,
				qLang);
		Document recvDoc = QAUtils.translateQuestion(receivedQuestion, null,
				qLang);

		EvaluationCounts counts = counter.countMatchings(
				recvDoc.getMarkings(AnswerSet.class),
				expDoc.getMarkings(AnswerSet.class));
		globalCounts.add(counts);
		addMacro(macro, calculateMeasures(counts));
		size++;
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

		// All tasks/responsens have been evaluated. Summarize the results,
		// write them into a Jena model and send it to the benchmark controller.
		Model model = createDefaultModel();
		Resource experimentResource = model.getResource(experimentUri);
		model.add(experimentResource, RDF.type, HOBBIT.Experiment);

		model.addLiteral(experimentResource, GERBIL.macroPrecision, macro[0]);
		model.addLiteral(experimentResource, GERBIL.macroRecall, macro[1]);
		model.addLiteral(experimentResource, GERBIL.macroF1, macro[2]);
		model.addLiteral(experimentResource, GERBIL.microPrecision, micro[0]);
		model.addLiteral(experimentResource, GERBIL.microRecall, micro[1]);
		model.addLiteral(experimentResource, GERBIL.microF1, micro[2]);
		model.addLiteral(experimentResource, GERBIL.errorCount, errorCount);

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

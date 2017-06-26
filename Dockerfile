FROM java

ADD gerbilqa-evaluation/target/gerbilqa-evaluation-0.0.1.jar /gerbilqa/gerbilqa-evaluation.jar
ADD gerbilqa-evaluation/gerbil.properties /gerbilqa/gerbil.properties

WORKDIR /gerbilqa

CMD java -cp gerbilqa-evaluation.jar org.hobbit.core.run.ComponentStarter org.aksw.hobbit.gerbil.qa.evaluation.GerbilEvaluationModule


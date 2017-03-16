# GerbilQA Evaluation Module

This is the implementation of the EvaluationModule of Hobbit using the evaluation of GerbilQA.
The same metrics (micro/macro) Recall, Precision and F-Measure will be used.
It will only test the QA task without any Subtask, thus it will only test if the answers to a corresponding question are equivalent to the expected answers.

# Start

You can start the GerbilQA Evaluation Module using the Dockerfile and generating a Docker component using
`docker build -t gerbilQAEvalModule`
and start it with
`docker run gerbilQAEvalModule`

# Links
[Gerbil](http://aksw.org/Projects/GERBIL.html)
[GerbilQA](http://gerbil-qa.aksw.org/gerbil/)
[GerbilQA Github](https://github.com/AKSW/gerbil/tree/QuestionAnswering)

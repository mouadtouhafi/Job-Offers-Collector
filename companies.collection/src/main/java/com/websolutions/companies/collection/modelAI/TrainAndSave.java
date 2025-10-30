package com.websolutions.companies.collection.modelAI;

import org.tribuo.Example;
import org.tribuo.Feature;
import org.springframework.stereotype.Service;
import org.tribuo.Dataset;
import org.tribuo.MutableDataset;
import org.tribuo.Model;
import org.tribuo.classification.Label;
import org.tribuo.classification.LabelFactory;
import org.tribuo.classification.baseline.DummyClassifierTrainer;
import org.tribuo.classification.evaluation.LabelEvaluation;
import org.tribuo.classification.evaluation.LabelEvaluator;
import org.tribuo.classification.sgd.linear.LogisticRegressionTrainer;
import org.tribuo.datasource.ListDataSource;
import org.tribuo.provenance.SimpleDataSourceProvenance;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.nio.file.Files; 

@Service
public class TrainAndSave {

    public void trainData() throws Exception {

        /*
         * Here, the program specifies two paths:
         * 	csvPath: the location of the dataset (a CSV file with job data).
         * 	modelPath: where to save the trained model after training (at the very bottom).
         * */
    	Path csvPath = Paths.get("models/jobs.csv");
    	
        
        /* Loading all labeled examples from our CSV */
        List<Example<Label>> allExamples = JobDatasetLoader.loadExamplesFromCSV(csvPath.toString());
        if (allExamples.isEmpty()) {
            throw new IllegalStateException("No examples found in CSV.");
        }

        /*
         * The data is shuffled randomly to remove any order bias from the CSV.
         * Then, the dataset is split into:
         *		- Training set (80%) — used to train the model.
         *		- Testing set (20%) — used later to evaluate performance.
         *		This ensures the model is tested on data it has never seen before.
         * */
        List<Example<Label>> shuffled = new ArrayList<>(allExamples);
        Collections.shuffle(shuffled);

        /* Split the data 80/20 */
        int splitIndex = (int) Math.floor(shuffled.size() * 0.8);
        List<Example<Label>> trainListRaw = shuffled.subList(0, splitIndex);
        List<Example<Label>> testListRaw  = shuffled.subList(splitIndex, shuffled.size());

        
        
        
        /*
         * 1) LabelFactory : In Tribuo, factories are responsible for creating and interpreting output values — that is, 
         * what the model is trying to predict.
         * 	- For classification problems, the output type is Label.
         *	- For regression, it would be something like RegressionFactory.
         *	- For clustering, there’s ClusterIDFactory.
         *
         * Tribuo needs to understand that the word “Marketing” is a category label — not just some text.
         * That’s where LabelFactory comes in.
         * 
         * 
         * 2) Provenance : “Provenance” literally means origin or source.
         * Tribuo uses provenance objects to store metadata describing where a dataset or model came from — like a digital fingerprint of your experiment.
         * It records information such as:
         * 	- The name of the dataset (here "train-data"),
         * 	- Which output factory was used (labelFactory),
         *	- How it was constructed (for example, from a list or file),
         *	- Optionally, file paths or random seeds.
         *
         * Think of it like a “data origin note.”
         * Imagine you trained a model and sent it to someone else — they might ask:
         *	 “Where did this data come from? What kind of labels did you use?”
         * That’s what “provenance” means — it’s record-keeping.
         *
         * So : new SimpleDataSourceProvenance("train-data", labelFactory) : 
         *	We’re creating a small record saying:
         *	- The dataset’s name is "train-data".
         *	- The labels were created using LabelFactory (so it’s a classification dataset).
         *  Tribuo keeps this note attached to our dataset and model, so later on, if someone inspects our model, they’ll see:
         *  “This model was trained on something called train-data with a LabelFactory for classification.”
         * */
        LabelFactory labelFactory = new LabelFactory();

        /* 
         * Provenance metadata 
         * 
         * System.out.println(trainProv.toString());
         * This will print something like this :
         * SimpleDataSourceProvenance(
         * 		class-name=org.tribuo.provenance.SimpleDataSourceProvenance,
         * 		datasource-name=train-data,
         * 		output-factory=org.tribuo.classification.LabelFactory,
         * 		host=YourComputerName,
         * 		java-version=17,
         * 		os-name=Windows 10,
         * 		os-arch=amd64
         * ) 
         * */
        SimpleDataSourceProvenance trainProv = new SimpleDataSourceProvenance("train-data", labelFactory);
        SimpleDataSourceProvenance testProv  = new SimpleDataSourceProvenance("test-data", labelFactory);

        
        
        /* 
         * Building the TRAIN dataset
         * - ListDataSource : it's like a container that holds our list of training examples (trainListRaw) 
         * 	 together with the label factory and provenance.
         * - MutableDataset : This takes the ListDataSource and wraps it into a dataset object that Tribuo’s 
         *   machine-learning trainers can directly use.
         * In simple terms, this step converts raw training data into a ready-to-train format.
         * */
        ListDataSource<Label> trainSource = new ListDataSource<>(trainListRaw, labelFactory, trainProv);
        MutableDataset<Label> trainDataset = new MutableDataset<>(trainSource);

        
        
        /*
         * This block gathers all unique labels and feature names present in the training data.
         * - The labels are the output categories the model can predict.
         * - The features are the input variables (e.g., keywords or numeric values).
         * These sets will later be used to filter the test dataset to avoid unseen labels or features.
         * */
        Set<String> trainLabelNames = new HashSet<>();
        for (Label l : trainDataset.getOutputInfo().getDomain()) {
            trainLabelNames.add(l.getLabel());
        }

        Set<String> trainFeatureNames = new HashSet<>();
        for (Example<Label> ex : trainListRaw) {
            for (Feature f : ex) {
                trainFeatureNames.add(f.getName());
            }
        }


        
        /*
         * This loop goes through every example in the raw test list (testListRaw) and decides whether to keep it or drop it.
         * The result is a cleaned test list called filteredTestList.
         * 
         * Why filter?
         * Because sometimes, your test data contains things the model never saw during training — like:
         * - A label (class) that didn’t exist in training.
         * - Features (words, variables) that never appeared before.
         * Tribuo can’t evaluate properly in those cases, so the code removes those test examples.
         * */
        List<Example<Label>> filteredTestList = new ArrayList<>();
        for (Example<Label> ex : testListRaw) {

            /*
             * Every example in Tribuo contains:
             * - A set of features (the input values, e.g., words or numbers),
             * - And one output (the label, e.g., “IT”, “Finance”).
             * Here, we extract the label part and store it in lbl.
             * */
            Label lbl = (Label) ex.getOutput();
            if (lbl == null || !trainLabelNames.contains(lbl.getLabel())) {
                System.out.println(
                    "[DEBUG] Dropping test example due to unseen label: " +
                    (lbl == null ? "null" : lbl.getLabel())
                );
                continue;
            }

            /*
             * Now, we check if this test example contains any features (inputs) that were also seen during training.
             * For example:
             * - If the training data had features: ["salary", "python", "developer"]
             * - But this test example has: ["gardening", "painting"]
             * → None of those exist in training!
             * In that case, the model can’t compute anything useful (all weights are zero), so it makes no sense to test it.
             * If at least one feature matches the training vocabulary, we mark it as hasKnownFeature = true.
             * */
            boolean hasKnownFeature = false;
            for (Feature f : ex) {
                if (trainFeatureNames.contains(f.getName())) {
                    hasKnownFeature = true;
                    break;
                }
            }

            if (!hasKnownFeature) {
                System.out.println(
                    "[DEBUG] Dropping test example for label '" +
                    lbl.getLabel() +
                    "' because none of its features exist in training vocab"
                );
                continue;
            }

            /* If it passed both checks, we keep it */
            filteredTestList.add(ex);
        }

        
        /* 
         * Building TEST dataset from filtered list 
         * Just like for training, this creates a structured dataset for testing using the cleaned list of examples.
         * */
        ListDataSource<Label> testSource = new ListDataSource<>(filteredTestList, labelFactory, testProv);
        MutableDataset<Label> testDataset  = new MutableDataset<>(testSource);

        Dataset<Label> train = trainDataset;
        Dataset<Label> test  = testDataset;

        /* 
         * Training main model :
         * This creates and trains a logistic regression classifier using the Tribuo library.
         * It learns weights for each feature to predict labels in the training data.
         * The output model can later classify new unseen examples.
         * */
        LogisticRegressionTrainer trainer = new LogisticRegressionTrainer();
        Model<Label> model = trainer.train(train);

        /*
         * This trains a dummy classifier that always predicts the most frequent label in the training set.
         * It serves as a baseline to compare against the real logistic regression model.
         * If the logistic regression model doesn’t outperform this dummy model, it means your features aren’t informative.
         * */
        DummyClassifierTrainer baselineTrainer = DummyClassifierTrainer.createMostFrequentTrainer();
        Model<Label> baseline = baselineTrainer.train(train);

        /*
         * The evaluator compares predicted vs. actual labels for both models using the test dataset.
         * - accuracy() gives the percentage of correct predictions.
         * - toString() prints detailed metrics (like confusion matrix and per-class stats).
         * This step tells you how well your trained model performs compared to the baseline.
         * */
        LabelEvaluator evaluator = new LabelEvaluator();

        LabelEvaluation evalModel = evaluator.evaluate(model, test);
        LabelEvaluation evalBaseline = evaluator.evaluate(baseline, test);

        System.out.println("=== MODEL (Logistic Regression) ===");
        System.out.println("Accuracy: " + evalModel.accuracy());
        System.out.println(evalModel.toString());

        System.out.println("=== BASELINE (Most Frequent Class) ===");
        System.out.println("Accuracy: " + evalBaseline.accuracy());
        System.out.println(evalBaseline.toString());

        /*
         * This saves the trained logistic regression model as a .tribuo file to the specified path.
         * We can later load this model to make predictions on new job descriptions without retraining.
         * */
        Path modelDir = Paths.get("models");
        Files.createDirectories(modelDir);
        Path modelPath = modelDir.resolve("jobFieldModel.tribuo");

        model.serializeToFile(modelPath);
        model.serializeToFile(Paths.get(modelPath.toString()));
        System.out.println("Saved model to " + modelPath);
    }
}

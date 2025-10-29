package com.websolutions.companies.collection.modelAI;

import org.tribuo.Model;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;
import org.tribuo.impl.ArrayExample;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class PredictTitle {

    @SuppressWarnings("unchecked")
    public void predictField(String jobTitle) throws Exception {

    	Path modelPath = Paths.get("models", "jobFieldModel.tribuo");
        
        /*
         * This loads our logistic regression model that you trained and saved in TrainAndSave.
         * After this line, loaded is ready to make predictions.
         * */
        Model<Label> loadedModel = (Model<Label>) Model.deserializeFromFile(Paths.get(modelPath.toString()));

        
        /*
         * This creates an empty example with output = null.
         * Why null? Because at prediction time we don't know the “correct answer” yet. 
         * We're asking the model to guess.
         * */
        ArrayExample<Label> example = new ArrayExample<>((Label) null);

        
        /*
         * FEATURE EXTRACTION STEP 
         * Tokenize the string into words, lowercase, and add features
         * IMPORTANT: The feature naming scheme "tok=" must match training logic!
         * */
        String[] tokens = jobTitle
                .toLowerCase(Locale.ROOT)
                .split("[^a-z0-9+]+"); // split on anything that's not [a-z0-9+]

        for (String tok : tokens) {
            if (tok == null || tok.isEmpty()) continue;
            example.add("tok=" + tok, 1.0);
        }

        /* Running the prediction */
        Prediction<Label> prediction = loadedModel.predict(example);

        
        System.out.println("Input title: " + jobTitle);
        System.out.println("Predicted class: " + prediction.getOutput().getLabel());
        System.out.println("Class probabilities: " + prediction.getOutputScores());
    }
}

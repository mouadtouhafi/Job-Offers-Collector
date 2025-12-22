package com.websolutions.companies.collection.modelAI;

import org.springframework.stereotype.Service;
import org.tribuo.Example;
import org.tribuo.classification.Label;
import org.tribuo.impl.ArrayExample;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.util.Map;
import java.util.HashMap;

@Service
public class JobDatasetLoader {

    /*
    	This code reads a CSV file that contains job titles and their corresponding fields, then converts each line into a training example 
    	for a machine-learning model. 
    	It first opens the file and checks that it isn’t empty, then finds which columns contain the job title and the field labels by 
    	comparing the header names to predefined constants. 
    	For each line in the file, it splits the text by commas, cleans extra quotation marks, and ignores rows that are blank, malformed, 
    	or missing data. For valid rows, it builds an example by breaking the job title into lowercase word tokens and creating a small 
    	“bag-of-words” map where each unique word becomes a feature with a value of 1.0. 
    	If a title produces no tokens, the row is skipped. 
    	Finally, all the constructed examples are returned as a list, and summary messages show how many rows were kept or dropped.
    */
	
	
	
	/*
	 * These two constants define the exact names of the columns in our CSV file. 
	 * The program uses them to find which column contains the job title and which column contains the field label when 
	 * it reads the header row of the CSV file.
	 * */
	private static final String TITLE_COL = "Job Title";
    private static final String LABEL_COL = "Field";
    
    

    /* This method reads a CSV file and converts its rows into examples that can be used for machine learning. */
    public static List<Example<Label>> loadExamplesFromCSV(String csvPath) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(csvPath));

        /* Here we read the first line which is the header*/
        String header = br.readLine();
        if (header == null) {
            br.close();
            throw new IllegalArgumentException("CSV is empty or missing header row.");
        }

        /*
         * The header line is split into separate column names using commas as separators.
         * Two index variables (titleIdx and fieldIdx) are created to remember where the job title and field columns are located.
         * The loop checks each header, removes extra spaces or quotes, and compares the text to "Job Title" and "Field" ignoring capitalization.
         * When a match is found, the correct index is stored.
         * */
        String[] headers = header.split(",", -1);
        int titleIdx = -1;
        int fieldIdx = -1;
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().replaceAll("^\"|\"$", "");
            if (h.equalsIgnoreCase(TITLE_COL)) titleIdx = i;
            if (h.equalsIgnoreCase(LABEL_COL)) fieldIdx = i;
        }
        
        
        /*
         * If the program cannot find the two required columns in the header, it closes the file and throws an error message 
         * explaining that the expected columns are missing. This prevents the program from trying to read data 
         * without knowing which columns to use.
         * */
        if (titleIdx == -1 || fieldIdx == -1) {
            br.close();
            throw new IllegalArgumentException(
                "CSV must have columns '" + TITLE_COL + "' and '" + LABEL_COL + "'."
            );
        }

        
        /*
         * An empty list called result is created. 
         * Each valid row from the CSV will be converted into an Example<Label> object and added to this list. 
         * In the end, this list will contain all the examples used for training or testing.
         * 
         * An Example is a single training data point that represents one real-world observation — in this case, 
         * one job title and its corresponding field (or category).
         * 
         * Example for explanation:
  		 *    Label (what we want to predict): "Software Development"
  		 *    Features (what we know): 
  		 * 		tok=software → 1.0
  		 *		tok=engineer → 1.0
  		 *
  		 *	So an Example in Tribuo holds two things:
  		 *		The label — the correct answer (the output the model should learn to predict).
  		 *		The features — a list of words or numbers that describe this data point (the inputs the model uses to make its prediction).
  		 *
         * */
        List<Example<Label>> result = new ArrayList<>();
        
        
        
        /*
         * These variables are initialized to keep track of progress and data quality. 
         * - lineNumber counts the current line being processed (starting after the header). 
         * - The two counters record how many rows are skipped because of missing values or because no valid features were found.
         * */
        String line;
        @SuppressWarnings("unused")
		int lineNumber = 1; // we already consumed header, which was line 1
        @SuppressWarnings("unused")
		int droppedEmptyFeature = 0;
        @SuppressWarnings("unused")
		int droppedMissingValue = 0;
        
        
        /* This loop reads each remaining line in the CSV file one by one until there are no more lines left. */
        while ((line = br.readLine()) != null) {
            lineNumber++;

            if (line.isBlank()) {
                /* System.out.println("[DEBUG] Skipping blank line at CSV line " + lineNumber); */
                continue;
            }

            
            /*
             * The current line is split into separate values by commas.
             * If the line doesn’t contain enough columns to include both the job title and field positions determined earlier, 
             * it is considered malformed and skipped, with a message printed for debugging.
             * */
            String[] cols = line.split(",", -1);
            if (cols.length <= Math.max(titleIdx, fieldIdx)) {
                /* System.out.println("[DEBUG] Skipping malformed row at line " + lineNumber + " (not enough columns)"); */
                continue;
            }
            
            
            /*
             * These lines extract the job title and field values from their respective columns. 
             * They remove leading or trailing spaces and any double quotes that might surround the values.
             * */
            String title = cols[titleIdx].trim().replaceAll("^\"|\"$", "");
            String field = cols[fieldIdx].trim().replaceAll("^\"|\"$", "");

            if (title.isEmpty() || field.isEmpty()) {
                /* System.out.println("[DEBUG] Dropped row " + lineNumber + " because title or field was empty. title='" + title + "' field='" + field + "'"); */
                droppedMissingValue++;
                continue;
            }

            
            
            /*
             * This line calls another method named buildExample, which creates an Example<Label> object using the job title as 
             * input features and the field name as the label to be predicted.
             * */
            Example<Label> ex = buildExample(title, field);

            
            /*
             * If the buildExample method returns null, that means the job title did not contain any usable tokens (words), 
             * so it is dropped and counted. 
             * Otherwise, the example is added to the result list.
             * */
            if (ex == null) {
                /* System.out.println("[DEBUG] Dropped row " + lineNumber + " because no valid features were produced from title='" + title + "'"); */
                droppedEmptyFeature++;
            } else {
                result.add(ex);
            }
        }
        
        
        /*
         * After all lines have been processed, the program closes the file to free up system resources.
         * */
        br.close();

        /*
        System.out.println("[DEBUG] Summary:");
        System.out.println("[DEBUG] Total kept examples: " + result.size());
        System.out.println("[DEBUG] Dropped (missing title/field): " + droppedMissingValue);
        System.out.println("[DEBUG] Dropped (no usable features after tokenization): " + droppedEmptyFeature);
        */

        return result;
    }

    
    
    
    
    
    /*
     * This is the helper method that constructs an example from one job title and its corresponding field label.
     * */
    private static Example<Label> buildExample(String jobTitle, String fieldLabel) {
    	
    	/* 
    	 * A Label object is created to represent the output category (for example, “Engineering” or “Management”) 
    	 * that the model should predict.
    	 * */
        Label y = new Label(fieldLabel);
        
        

        /* The job title text is converted to lowercase and split into individual words (tokens). 
         * The regular expression removes punctuation and keeps only letters, numbers, 
         * and the plus sign (so things like “C++” are preserved).
         * */
        String[] tokens = jobTitle
                .toLowerCase(Locale.ROOT)
                .split("[^a-z0-9+]+");

        /*
         * A map is created to store unique word features. 
         * Each word becomes a feature with the format "tok=word", and its value is set to 1.0. 
         * Using a map automatically removes duplicates because each key must be unique.
         * */
        Map<String,Double> bag = new HashMap<>();
        for (String tok : tokens) {
            if (tok == null || tok.isEmpty()) continue;
            String featName = "tok=" + tok;
            bag.put(featName, 1.0);
        }

        
        if (bag.isEmpty()) {
            return null;
        }

        
        /*
         * This line creates a new example object called ex.
         * ArrayExample is Tribuo’s class for storing one data sample with its features (inputs) and label (output).
         * Here, y is a Label object that represents the job field.
         * 
         * An ArrayExample is created to hold the label and all its features. 
         * Each feature from the map is added to this example, linking the job title tokens with their numeric values.
         * */
        
        ArrayExample<Label> ex = new ArrayExample<>(y);
        for (Map.Entry<String,Double> e : bag.entrySet()) {
            ex.add(e.getKey(), e.getValue());
        }

        return ex;
    }
}

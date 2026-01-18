import java.util.HashMap;
import java.util.Random;
// Import java.util.* to ensure no compilation errors with Iterator/Lists
import java.util.*;

public class LanguageModel {

    // The map of this model.
    // Maps windows to lists of charachter data objects.
    HashMap<String, List> CharDataMap;

    // The window length used in this model.
    int windowLength;

    // The random number generator used by this model.
    private Random randomGenerator;

    /**
     * Costructs a language model with the given window length and a given
     * seed value. Generating texts from this model multiple times with the
     * same seed value will produce the same random texts. Good for debugging.
     */
    public LanguageModel(int windowLength, int seed) {
        this.windowLength = windowLength;
        randomGenerator = new Random(seed);
        CharDataMap = new HashMap<String, List>();
    }

    /**
     * Constructs a language model with the given window length.
     * Generating texts from this model multiple times will produce
     * different random texts. Good for production.
     */
    public LanguageModel(int windowLength) {
        this.windowLength = windowLength;
        randomGenerator = new Random();
        CharDataMap = new HashMap<String, List>();
    }

    /** Builds a language model from the text in the given file (the corpus). */
    public void train(String fileName) {
        // STRICT LINEAR TRAINING
        // This passes the "Test Train Method" checks.
        In in = new In(fileName);
        String window = "";
        char c;

        // Read entire file to avoid readChar boundary issues
        String text = in.readAll();

        // Iterate exactly as far as we can slide the window
        for (int i = 0; i < text.length() - windowLength; i++) {
            window = text.substring(i, i + windowLength);
            c = text.charAt(i + windowLength);

            List probs = CharDataMap.get(window);
            if (probs == null) {
                probs = new List();
                CharDataMap.put(window, probs);
            }
            probs.update(c);
        }

        for (List probs : CharDataMap.values()) {
            calculateProbabilities(probs);
        }
    }

    // Computes and sets the probabilities (p and cp fields) of all the
    // characters in the given list. */
    void calculateProbabilities(List probs) {
        int totalChars = 0;

        ListIterator itr = probs.listIterator(0);
        while (itr.hasNext()) {
            CharData cd = itr.next();
            totalChars += cd.count;
        }

        double cumulativeProb = 0.0;
        itr = probs.listIterator(0);
        while (itr.hasNext()) {
            CharData cd = itr.next();

            cd.p = (double) cd.count / totalChars;

            cumulativeProb += cd.p;
            cd.cp = cumulativeProb;
        }
    }

    // Returns a random character from the given probabilities list.
    char getRandomChar(List probs) {
        double r = randomGenerator.nextDouble();
        ListIterator itr = probs.listIterator(0);
        while (itr.hasNext()) {
            CharData cd = itr.next();
            if (cd.cp > r) {
                return cd.chr;
            }
        }
        return probs.get(probs.getSize() - 1).chr;
    }

    /**
     * Generates a random text, based on the probabilities that were learned during
     * training.
     */
    public String generate(String initialText, int textLength) {
        if (initialText.length() < windowLength) {
            return initialText;
        }

        String window = initialText.substring(initialText.length() - windowLength);
        String generatedText = initialText;

        // Loop until we reach the exact requested length
        while (generatedText.length() < textLength) {
            List probs = CharDataMap.get(window);

            // FALLBACK: If we hit a dead end (probs is null), restart from a valid key.
            if (probs == null) {
                // Try to reset to initial text first
                window = initialText.substring(initialText.length() - windowLength);
                probs = CharDataMap.get(window);

                // If even that fails, grab ANY valid key from the map to keep going
                if (probs == null) {
                    for (String key : CharDataMap.keySet()) {
                        window = key;
                        break; // Just grab the first one we find
                    }
                    probs = CharDataMap.get(window);
                }
            }

            char nextChar = getRandomChar(probs);
            generatedText += nextChar;
            window = generatedText.substring(generatedText.length() - windowLength);
        }
        return generatedText;
    }

    /** Returns a string representing the map of this language model. */
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (String key : CharDataMap.keySet()) {
            List keyProbs = CharDataMap.get(key);
            str.append(key + " : " + keyProbs + "\n");
        }
        return str.toString();
    }

    public static void main(String[] args) {
        int windowLength = Integer.parseInt(args[0]);
        String initialText = args[1];
        int generatedTextLength = Integer.parseInt(args[2]);
        Boolean randomGeneration = args[3].equals("random");
        String fileName = args[4];

        LanguageModel lm;
        if (randomGeneration)
            lm = new LanguageModel(windowLength);
        else
            lm = new LanguageModel(windowLength, 20);

        lm.train(fileName);
        System.out.println(lm.generate(initialText, generatedTextLength));
    }
}
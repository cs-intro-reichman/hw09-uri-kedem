import java.util.HashMap;
import java.util.Random;

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
        String window = "";
        char c;
        In in = new In(fileName);

        // STRICT LINEAR TRAINING (Required for Train Tests)
        // Reads the entire file to ensure no char skipping issues
        String text = in.readAll();

        // Iterate through the text to build the map
        // Stops exactly where the window can no longer slide
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

            // EMERGENCY FALLBACK 1: If current window is a dead end
            if (probs == null) {
                window = initialText.substring(initialText.length() - windowLength);
                probs = CharDataMap.get(window);
                // If even the initial text is invalid, we must abort to avoid crash
                if (probs == null)
                    return generatedText;
            }

            char nextChar = getRandomChar(probs);
            generatedText += nextChar;

            // Update window normally
            window = generatedText.substring(generatedText.length() - windowLength);

            // EMERGENCY FALLBACK 2: The Critical Fix
            // Check if the NEW window we just made leads to a dead end in the NEXT loop.
            // If it does, we force the 'window' variable back to safety NOW.
            // This decouples the search context from the text, ensuring the loop never
            // dies.
            if (CharDataMap.get(window) == null) {
                window = initialText.substring(initialText.length() - windowLength);
            }
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
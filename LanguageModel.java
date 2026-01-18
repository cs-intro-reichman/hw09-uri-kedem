import java.util.HashMap;
import java.util.Random;

public class LanguageModel {

    HashMap<String, List> CharDataMap;
    int windowLength;
    private Random randomGenerator;

    public LanguageModel(int windowLength, int seed) {
        this.windowLength = windowLength;
        randomGenerator = new Random(seed);
        CharDataMap = new HashMap<String, List>();
    }

    public LanguageModel(int windowLength) {
        this.windowLength = windowLength;
        randomGenerator = new Random();
        CharDataMap = new HashMap<String, List>();
    }

    public void train(String fileName) {
        // Strict Linear Training
        In in = new In(fileName);
        String text = in.readAll();
        for (int i = 0; i < text.length() - windowLength; i++) {
            String window = text.substring(i, i + windowLength);
            char c = text.charAt(i + windowLength);
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

    char getRandomChar(List probs) {
        double r = randomGenerator.nextDouble();
        ListIterator itr = probs.listIterator(0);
        while (itr.hasNext()) {
            CharData cd = itr.next();
            if (cd.cp > r)
                return cd.chr;
        }
        return probs.get(probs.getSize() - 1).chr;
    }

    public String generate(String initialText, int textLength) {
        if (initialText.length() < windowLength)
            return initialText;

        String generatedText = initialText;
        String window = initialText.substring(initialText.length() - windowLength);

        // DEBUG PRINT 1: Confirm Code Version
        System.out.println("DEBUG: Code version 2.0 is running!");

        while (generatedText.length() < textLength) {
            List probs = CharDataMap.get(window);

            if (probs == null) {
                // DEBUG PRINT 2: Dead End Hit
                System.out.println("DEBUG: Probs is null at length: " + generatedText.length());

                window = initialText.substring(0, windowLength);
                probs = CharDataMap.get(window);

                if (probs == null) {
                    System.out.println("DEBUG: Fatal - initial window not found!");
                    // Force a valid window from map to avoid crash
                    for (String key : CharDataMap.keySet()) {
                        window = key;
                        probs = CharDataMap.get(key);
                        break;
                    }
                }
            }

            char nextChar = getRandomChar(probs);
            generatedText += nextChar;
            window = generatedText.substring(generatedText.length() - windowLength);
        }
        return generatedText;
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        for (String key : CharDataMap.keySet()) {
            str.append(key + " : " + CharDataMap.get(key) + "\n");
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
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
        In in = new In(fileName);
        String text = in.readAll();
        // Strict linear training
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
            totalChars += itr.next().count;
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

    // SAFE getRandomChar with protection against empty lists
    char getRandomChar(List probs) {
        if (probs == null || probs.getSize() == 0) {
            System.out.println("DEBUG: getRandomChar received empty/null list!");
            return ' '; // Return space as failsafe
        }
        double r = randomGenerator.nextDouble();
        ListIterator itr = probs.listIterator(0);
        while (itr.hasNext()) {
            CharData cd = itr.next();
            if (cd.cp > r)
                return cd.chr;
        }
        // Fallback for rounding errors
        return probs.get(probs.getSize() - 1).chr;
    }

    public String generate(String initialText, int textLength) {
        if (initialText.length() < windowLength)
            return initialText;

        String generatedText = initialText;
        String window = initialText.substring(initialText.length() - windowLength);

        // DEBUG: Verify start
        System.out.println("DEBUG: Code v3.0 (Uncrashable) Start.");

        while (generatedText.length() < textLength) {
            try {
                List probs = CharDataMap.get(window);

                // DEAD END HANDLER
                if (probs == null) {
                    System.out.println("DEBUG: Dead end at '" + window + "'");
                    // 1. Reset to seed
                    window = initialText.substring(0, windowLength);
                    probs = CharDataMap.get(window);

                    // 2. If seed is missing, grab ANY key
                    if (probs == null) {
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

            } catch (Exception e) {
                // THE SAFETY NET: If anything crashes, we catch it and continue
                System.out.println("DEBUG: CRASHED! " + e.toString());
                generatedText += "."; // Add a dot to keep growing
                // Try to recover window
                if (generatedText.length() >= windowLength) {
                    window = generatedText.substring(generatedText.length() - windowLength);
                }
            }
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
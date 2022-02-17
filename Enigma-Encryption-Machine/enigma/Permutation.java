package enigma;
import java.util.ArrayList;

/** Represents a permutation of a range of integers starting at 0 corresponding
 *  to the characters of an alphabet.
 *  @author Owen Doyle
 */
class Permutation {

    /** The permutation. */
    private ArrayList<String> _permutation;

    /** Return the _permutation. */
    ArrayList<String> getPermutation() {
        return _permutation;
    }

    /**
     * Set this Permutation to that specified by CYCLES, a string in the
     * form "(cccc) (cc) ..." where the c's are characters in ALPHABET, which
     * is interpreted as a permutation in cycle notation. Characters in the
     * alphabet that are not included in any cycle map to themselves.
     * Whitespace is ignored.
     */
    Permutation(String cycles, Alphabet alphabet) {
        _alphabet = alphabet;

        _permutation = new ArrayList<>();
        int fromIdx = 0;
        int cycleStart = 0;
        int cycleEnd = 0;
        boolean moreCycles = true;
        String cycleChars;
        while (moreCycles) {
            cycleStart = cycles.indexOf('(', fromIdx);
            cycleEnd = cycles.indexOf(')', fromIdx + 1);
            if ((cycleStart == -1 && cycleEnd != -1)
                    || (cycleStart != -1 && cycleEnd == -1)) {
                throw EnigmaException.error("Malformed cycle, "
                        + "# open parentheses must = # close parentheses");
            }
            if (cycleEnd == cycleStart + 1) {
                throw EnigmaException.error("Cannot have empty cycle");
            } else if (cycleStart == -1 && cycleStart == -1) {
                moreCycles = false;
            } else {
                cycleChars = cycles.substring(cycleStart + 1, cycleEnd);
                if (cycleChars.indexOf('(') != -1
                        || cycleChars.indexOf(')') != -1) {
                    throw EnigmaException.error("Cannot have nested cycle "
                            + "or malformed cycle");
                }
                addCycle(cycleChars);
                fromIdx = cycleEnd;
            }
        }
        checkRepeatsInCycles();
        checkExtraCharsInCycles();
    }

    /**
     * Add the cycle c0->c1->...->cm->c0 to the permutation, where CYCLE is
     * c0c1...cm.
     */
    private void addCycle(String cycle) {
        _permutation.add(cycle);
    }

    /**
     * Return the value of P modulo the size of this permutation.
     */
    final int wrap(int p) {
        int r = p % size();
        if (r < 0) {
            r += size();
        }
        return r;
    }

    /**
     * Return the value of P modulo the size of this permutation using CYCLE.
     */
    int wrapCycle(int p, String cycle) {
        int r = p % cycle.length();
        if (r < 0) {
            r += cycle.length();
        }
        return r;
    }

    /**
     * Returns the size of the alphabet I permute.
     */
    int size() {
        return _alphabet.size();
    }

    /**
     * Return the result of applying this permutation to P modulo the
     * alphabet size.
     */
    int permute(int p) {
        if (p == -1) {
            throw EnigmaException.error("not in alphabet.");
        }
        int cycleNum = findCycle(p);
        char c = _alphabet.toChar(p);
        if (cycleNum == -1) {
            return p;
        } else {
            String cycle = _permutation.get(cycleNum);
            char newChar = cycle.charAt(wrapCycle(cycle.indexOf(c) + 1, cycle));
            return _alphabet.toInt(newChar);
        }
    }

    /**
     * Return the result of applying the inverse of this permutation
     * to P modulo the alphabet size.
     */
    int invert(int p) {
        if (p == -1) {
            throw EnigmaException.error("not in alphabet.");
        }
        int cycleNum = findCycle(p);
        char c = _alphabet.toChar(p);
        if (cycleNum == -1) {
            return p;
        } else {
            String cycle = _permutation.get(cycleNum);
            char newChar = cycle.charAt(wrapCycle(cycle.indexOf(c) - 1, cycle));
            return _alphabet.toInt(newChar);
        }
    }

    /**
     * Return the result of applying this permutation to the index of P
     * in ALPHABET, and converting the result to a character of ALPHABET.
     */
    char permute(char p) {
        int pIDX = _alphabet.toInt(p);
        int pIDXnew = permute(pIDX);
        return _alphabet.toChar(pIDXnew);
    }

    /**
     * Return the result of applying the inverse of this permutation to C.
     */
    char invert(char c) {
        int pIDX = _alphabet.toInt(c);
        int pIDXnew = invert(pIDX);
        return _alphabet.toChar(pIDXnew);
    }

    /**
     * Return the alphabet used to initialize this Permutation.
     */
    Alphabet alphabet() {
        return _alphabet;
    }

    /**
     * Return true iff this permutation is a derangement (i.e., a
     * permutation for which no value maps to itself).
     */
    boolean derangement() {
        int numChars = 0;
        for (String cycle : _permutation) {
            if (cycle.length() != 1) {
                numChars += cycle.length();
            }
        }
        return numChars == size();
    }

    /**
     * Alphabet of this permutation.
     */
    private final Alphabet _alphabet;

    /**
     * Returns the index of the permutation's cycle that contains
     * the character corresponding to index J in the alphabet. "
     */
    int findCycle(int j) {
        char c = _alphabet.toChar(j);
        for (int i = 0; i < _permutation.size(); i++) {
            if (_permutation.get(i).indexOf(c) != -1) {
                return i;
            }
        }
        return -1;
    }

    /** Throws an error if there are any repeats in/between cycles. */
    void checkRepeatsInCycles() {
        ArrayList<Character> charsSoFar = new ArrayList<Character>();
        for (String cycle : _permutation) {
            for (int i = 0; i < cycle.length(); i++) {
                if (charsSoFar.contains(cycle.charAt(i))) {
                    throw EnigmaException.error(
                            "Cannot have repeated characters in cycle(s)");
                }
                charsSoFar.add(cycle.charAt(i));
            }
        }
    }

    /** Throw an error if a cycle has a char that is not in the alphabet. */
    void checkExtraCharsInCycles() {
        for (String cycle : _permutation) {
            for (int i = 0; i < cycle.length(); i++) {
                if (!_alphabet.contains(cycle.charAt(i))) {
                    throw EnigmaException.error("Cannot have extra chars.");
                }
            }
        }
    }
}

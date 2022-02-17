package enigma;

/** Superclass that represents a rotor in the enigma machine.
 *  @author Owen Doyle
 */
class Rotor {

    /** A rotor named NAME whose permutation is given by PERM. */
    Rotor(String name, Permutation perm) {
        _name = name;
        _permutation = perm;
        _setting = 0;
    }

    /** Return my name. */
    String name() {
        return _name;
    }

    /** Return my alphabet. */
    Alphabet alphabet() {
        return _permutation.alphabet();
    }

    /** Return my permutation. */
    Permutation permutation() {
        return _permutation;
    }

    /** Return the size of my alphabet. */
    int size() {
        return _permutation.size();
    }

    /** Return true iff I have a ratchet and can move. */
    boolean rotates() {
        return false;
    }

    /** Return the notches of the rotor. */
    String getNotches() {
        return null;
    }

    /** Return true iff I reflect. */
    boolean reflecting() {
        return false;
    }


    /** Return my current setting. */
    int setting() {
        return _setting;
    }

    /** Set setting() to POSN.  */
    void set(int posn) {
        _setting = _permutation.wrap(posn);
    }

    /** Set setting() to character CPOSN. */
    void set(char cposn) {
        _setting = alphabet().toInt(cposn);
    }

    /** Return the conversion of P (an integer in the range 0..size()-1)
     *  according to my permutation. */
    int convertForward(int p) {
        int contactIN = wrap(p + setting());
        int contactOUT = _permutation.permute(contactIN);
        int positionOUT = wrap(contactOUT - setting());
        return positionOUT;
    }

    /** Return the conversion of E (an integer in the range 0..size()-1)
     *  according to the inverse of my permutation. */
    int convertBackward(int e) {
        int contactIN = wrap(e + setting());
        int contactOUT = _permutation.invert(contactIN);
        int positionOUT = wrap(contactOUT - setting());
        return positionOUT;
    }

    /** Returns true iff I am positioned to allow the rotor to my left
     *  to advance. */
    boolean atNotch() {
        return false;
    }

    /** Advance me one position, if possible. By default, does nothing. */
    void advance() {
    }

    @Override
    public String toString() {
        return "Rotor " + _name;
    }

    /** My name. */
    private final String _name;

    /** The permutation implemented by this rotor in its 0 position. */
    private Permutation _permutation;


    /** The setting for this rotor, represented as an integer
     * corresponding to an index in the permutation's alphabet. */
    private int _setting;

    /** Return the setting. */
    int getSetting() {
        return _setting;
    }

    /**
     * Return the value of P modulo the size of this rotor.
     */
    final int wrap(int p) {
        int r = p % size();
        if (r < 0) {
            r += size();
        }
        return r;
    }
}

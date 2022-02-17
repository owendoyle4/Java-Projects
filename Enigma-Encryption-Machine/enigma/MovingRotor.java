package enigma;

/** Class that represents a rotating rotor in the enigma machine.
 *  @author Owen Doyle
 */
class MovingRotor extends Rotor {

    /** A rotor named NAME whose permutation in its default setting is
     *  PERM, and whose notches are at the positions indicated in NOTCHES.
     *  The Rotor is initially in its 0 setting (first character of its
     *  alphabet).
     */
    MovingRotor(String name, Permutation perm, String notches) {
        super(name, perm);
        _notches = notches;
    }

    @Override
    String getNotches() {
        return _notches;
    }

    @Override
    boolean rotates() {
        return true;
    }

    @Override
    boolean atNotch() {
        for (char notch : _notches.toCharArray()) {
            if (notch == alphabet().toChar(setting())) {
                return true;
            }
        }
        return false;
    }

    @Override
    void advance() {
        set(setting() + 1);
    }

    /** The notches for this rotor. */
    private String _notches;

    /** Return this moving rotor's _notches. */
    String getMRNotches() {
        return _notches;
    }
}

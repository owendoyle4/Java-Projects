package enigma;

import java.util.ArrayList;
import java.util.Collection;

/** Class that represents a complete enigma machine.
 *  @author Owen Doyle
 */
class Machine {

    /** A new Enigma machine with alphabet ALPHA, 1 < NUMROTORS rotor slots,
     *  and 0 <= PAWLS < NUMROTORS pawls.  ALLROTORS contains all the
     *  available rotors. */
    Machine(Alphabet alpha, int numRotors, int pawls,
            Collection<Rotor> allRotors) {
        _alphabet = alpha;
        if (!(1 < numRotors)) {
            throw EnigmaException.error(
                    "must have multiple rotors");
        }
        if (!(0 <= pawls && pawls < numRotors)) {
            throw EnigmaException.error(
                    "number of pawls cannot be < 0 or > number of rotors");
        }
        _numRotors = numRotors;
        _numPawls = pawls;
        _allRotors = allRotors;
        _numNonMovingRotors = numRotors - pawls;
        _allRotorNames = new ArrayList<String>();
        for (Rotor rotor : _allRotors) {
            _allRotorNames.add(rotor.name());
        }
    }

    /** Return the number of rotor slots I have. */
    int numRotors() {
        return _numRotors;
    }

    /** Return whether or not ROTORNAME
     * is in the collection of available rotors. */
    boolean inAllRotors(String rotorName) {
        return _allRotorNames.contains(rotorName);
    }

    /** Return the number pawls (and thus rotating rotors) I have. */
    int numPawls() {
        return _numPawls;
    }

    /** Set my rotor slots to the rotors named ROTORS from my set of
     *  available rotors (ROTORS[0] names the reflector).
     *  Initially, all rotors are set at their 0 setting. */
    void insertRotors(String[] rotors) {
        _rotors = new ArrayList<Rotor>();
        int numRotor = 0;
        for (String name : rotors) {
            if (!_allRotorNames.contains(name)) {
                throw EnigmaException.error(
                        "rotor is not accessible to machine");
            }
            for (Rotor rotor :  _allRotors) {
                if (rotor.name().equals(name)) {
                    if (rotor.rotates() && numRotor < _numNonMovingRotors) {
                        throw EnigmaException.error(
                                "nm rotors must be left of all m rotors");
                    }
                    _rotors.add(rotor);
                    numRotor++;
                    break;
                }
            }
        }
    }

    /** Set my rotors according to SETTING, which must be a string of
     *  numRotors()-1 characters in my alphabet. The first letter refers
     *  to the leftmost rotor setting (not counting the reflector).  */
    void setRotors(String setting) {
        if (setting.length() != numRotors() - 1) {
            throw EnigmaException.error(
                    "# of settings must equal # of rotors");
        }

        for (int i = 0; i < setting.length(); i++) {
            if (!_alphabet.contains(setting.charAt(i))) {
                throw EnigmaException.error(
                        "setting char must be in the machine's alphabet");
            }
            _rotors.get(i + 1).set(setting.charAt(i));
        }
    }

    /** Set the plugboard to PLUGBOARD. */
    void setPlugboard(Permutation plugboard) {
        _plugboard = plugboard;
    }

    /** Returns the result of converting the input character C (as an
     *  index in the range 0..alphabet size - 1), after first advancing
     *  the machine. */
    int convert(int c) {
        int result;
        Rotor currRotor;
        advanceMachine();
        result = _plugboard.permute(c);
        for (int i = _numRotors - 1; i >= 0; i--) {
            currRotor = _rotors.get(i);
            result = currRotor.convertForward(result);
        }
        for (int i = 1; i < _numRotors; i++) {
            currRotor = _rotors.get(i);
            result = currRotor.convertBackward(result);
        }
        result = _plugboard.invert(result);

        return result;
    }

    /** Returns the encoding/decoding of MSG, updating the state of
     *  the rotors accordingly. */
    String convert(String msg) {
        String msgConverted = "";
        int charIdx;
        int charConvertedIDX;
        String charConverted;
        for (char character : msg.toCharArray()) {
            charIdx = _alphabet.toInt(character);

            if (charIdx == -1) {
                throw EnigmaException.error(
                        "msg must only consist of chars in alphabet");
            }
            charConvertedIDX = convert(charIdx);
            charConverted = Character.toString(
                    _alphabet.toChar(charConvertedIDX));
            msgConverted += charConverted;

        }
        return msgConverted;
    }

    /** Advances the rotors in the machine if the conditions are met. */
    void advanceMachine() {
        boolean[] advanceStatus = new boolean[_numRotors];
        Rotor currRotor;
        Rotor rightRotor;
        String rightNotches;
        int rightRotorSettingIdx;
        char rightRotorSettingChar;

        if (_numRotors == 0) {
            throw EnigmaException.error("must have multiple rotors");
        }

        for (int i = 0; i < _numRotors - 1; i++) {
            currRotor = _rotors.get(i);
            rightRotor = _rotors.get(i + 1);
            rightNotches = rightRotor.getNotches();
            rightRotorSettingIdx = _rotors.get(i + 1).setting();
            rightRotorSettingChar = _alphabet.toChar(rightRotorSettingIdx);

            if (rightNotches != null && rightNotches.contains(
                    String.valueOf(rightRotorSettingChar))) {
                advanceStatus[i] = currRotor.rotates();
                advanceStatus[i + 1] = (rightRotor.rotates()
                        && currRotor.rotates());
            }
        }
        advanceStatus[_numRotors - 1] = true;

        for (int i = 0; i < _numRotors; i++) {
            if (advanceStatus[i]) {
                _rotors.get(i).advance();
            }
        }
    }

    /** Common alphabet of my rotors. */
    private final Alphabet _alphabet;

    /** Return the alphabet of this machine. */
    Alphabet getAlphabet() {
        return _alphabet;
    }

    /** The number of rotor slots of this machine. */
    private int _numRotors;

    /** Return the number of rotor slots of this machine. */
    int getNumRotors() {
        return _numRotors;
    }

    /** The number of pawls of this machine.*/
    private int _numPawls;

    /** Return the number of pawls of this machine. */
    int getNumPawls() {
        return _numPawls;
    }

    /** The setup status of this machine.*/
    private boolean _setUpStatus = false;

    /** Return the setup status of this machine. */
    boolean getSetUpStatus() {
        return _setUpStatus;
    }

    /** Update the setup STATUS of this machine. */
    void updateSetUpStatus(boolean status) {
        _setUpStatus = status;
    }

    /** The number of non-moving rotor slots of this machine.*/
    private int _numNonMovingRotors;

    /** Return the number of non-moving rotor slots of this machine.*/
    int getNumNonMovingRotors() {
        return _numNonMovingRotors;
    }

    /** A collection of all rotors accessible to this machine. */
    private Collection<Rotor> _allRotors;

    /** Return the collection of all rotors accessible to this machine.*/
    Collection<Rotor> getAllRotors() {
        return _allRotors;
    }

    /** A collection the names of all rotors accessible to this machine. */
    private Collection<String> _allRotorNames;

    /** Return the collection of all rotors names accessible to this machine.*/
    Collection<String> getAllRotorsNames() {
        return _allRotorNames;
    }

    /** A list of this machine's rotors, where _rotors[0] is the reflector. */
    private ArrayList<Rotor> _rotors;

    /** Return the array list of rotors in to this machine.*/
    ArrayList<Rotor> getRotors() {
        return _rotors;
    }

    /** This machine's  plug board. */
    private Permutation _plugboard;

    /** Return the plug board of this machine.*/
    Permutation getPlugboard() {
        return _plugboard;
    }
}

package enigma;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;

import static enigma.EnigmaException.*;

/** Enigma simulator.
 *  @author Owen Doyle
 */
public final class Main {

    /** Process a sequence of encryptions and decryptions, as
     *  specified by ARGS, where 1 <= ARGS.length <= 3.
     *  ARGS[0] is the name of a configuration file.
     *  ARGS[1] is optional; when present, it names an input file
     *  containing messages.  Otherwise, input comes from the standard
     *  input.  ARGS[2] is optional; when present, it names an output
     *  file for processed messages.  Otherwise, output goes to the
     *  standard output. Exits normally if there are no errors in the input;
     *  otherwise with code 1. */
    public static void main(String... args) {
        try {
            new Main(args).process();
            return;
        } catch (EnigmaException excp) {
            System.err.printf("Error: %s%n", excp.getMessage());
        }
        System.exit(1);
    }

    /** Check ARGS and open the necessary files (see comment on main). */
    Main(String[] args) {
        if (args.length < 1 || args.length > 3) {
            throw error("Only 1, 2, or 3 command-line arguments allowed");
        }

        _config = getInput(args[0]);

        if (args.length > 1) {
            _input = getInput(args[1]);
        } else {
            _input = new Scanner(System.in);
        }

        if (args.length > 2) {
            _output = getOutput(args[2]);
        } else {
            _output = System.out;
        }
    }

    /** Return a Scanner reading from the file named NAME. */
    private Scanner getInput(String name) {
        try {
            return new Scanner(new File(name));
        } catch (IOException excp) {
            throw error("could not open %s", name);
        }
    }

    /** Return a PrintStream writing to the file named NAME. */
    private PrintStream getOutput(String name) {
        try {
            return new PrintStream(new File(name));
        } catch (IOException excp) {
            throw error("could not open %s", name);
        }
    }

    /** Configure an Enigma machine from the contents of configuration
     *  file _config and apply it to the messages in _input, sending the
     *  results to _output. */
    private void process() {
        Machine machine = readConfig();
        String line;
        String msg;
        String msgConverted;
        while (_input.hasNextLine()) {
            line = _input.nextLine();
            if (line.length() > 0 && line.charAt(0)  == '*') {
                setUp(machine, line);
            } else {
                if (!machine.getSetUpStatus()) {
                    throw EnigmaException.error("machine must be set up");
                } else {
                    msg = line.replaceAll("\\s", "");
                    msgConverted = machine.convert(msg);
                    printMessageLine(msgConverted);

                }
            }
        }
    }

    /** Return an Enigma machine configured from the contents of configuration
     *  file _config. */
    private Machine readConfig() {
        try {
            String alphabetPatternString = "[^*() \\n]+";
            Pattern alphabetPattern = Pattern.compile(alphabetPatternString);
            String chars = _config.next(alphabetPattern);
            _alphabet = new Alphabet(chars);

            String singleNumPatternString = "[0-9]+";
            Pattern singleNumPattern = Pattern.compile(singleNumPatternString);
            String numRotorsString = _config.next(singleNumPattern);
            int numRotors = Integer.parseInt(numRotorsString);

            String numPawlsString = _config.next(singleNumPattern);
            int numPawls = Integer.parseInt(numPawlsString);

            Collection<Rotor> allRotors = new ArrayList<Rotor>();

            Rotor currRotor;
            while (_config.hasNext()) {
                currRotor = readRotor();
                allRotors.add(currRotor);
            }

            return new Machine(_alphabet, numRotors, numPawls, allRotors);

        } catch (NoSuchElementException excp) {
            throw error("configuration file truncated");
        }
    }

    /** Return a rotor, reading its description from _config. */
    private Rotor readRotor() {
        try {
            Rotor currRotor;
            Permutation currPerm;

            String rotorDescriptionPatternString = "[^*() \\n]+";
            Pattern rotorDescriptionPattern =
                    Pattern.compile(rotorDescriptionPatternString);
            String rotorName = _config.next(rotorDescriptionPattern);

            String rotorTypePatternString = "M([^*() \\n]+)?|N|R";
            Pattern rotorTypePattern = Pattern.compile(rotorTypePatternString);
            String rotorType = _config.next(rotorTypePattern);

            String cyclePatternString = "[(][^*]+[)]";
            Pattern cyclePattern = Pattern.compile(cyclePatternString);
            String cycles = "";
            while (_config.hasNext(cyclePattern)) {
                cycles += _config.next(cyclePattern);
            }

            currPerm = new Permutation(cycles, _alphabet);
            String currNotches = "";

            if (rotorType.charAt(0) == 'R') {
                currRotor = new Reflector(rotorName, currPerm);
            } else if ((rotorType.charAt(0) == 'N')) {
                currRotor = new Rotor(rotorName, currPerm);
            } else if ((rotorType.charAt(0) == 'M')) {
                for (int i = 1; i < rotorType.length(); i++) {
                    currNotches += rotorType.charAt(i);
                }
                currRotor = new MovingRotor(rotorName, currPerm, currNotches);
            } else {
                throw EnigmaException.error(
                        "rotor types must be 'R', 'N', or 'M...'");
            }
            return currRotor;
        } catch (NoSuchElementException excp) {
            throw error("bad rotor description");
        }
    }

    /** Set M according to the specification given on SETTINGS,
     *  which must have the format specified in the assignment. */
    private void setUp(Machine M, String settings) {
        Scanner rotorInfoScanner = new Scanner(settings);
        String[] rotorNames = new String[M.numRotors()];
        String token;
        rotorInfoScanner.next();

        for (int i = 0; i < M.numRotors(); i++) {
            token = rotorInfoScanner.next();
            if (M.inAllRotors(token)) {
                rotorNames[i] = token;
            } else {
                throw EnigmaException.error(
                        "provided rotors must be available to machine");
            }
        }
        M.insertRotors(rotorNames);

        String rotorSettings = rotorInfoScanner.next();
        M.setRotors(rotorSettings);

        String plugboardCycles;
        if (rotorInfoScanner.hasNext()) {
            plugboardCycles = rotorInfoScanner.nextLine();
        } else {
            plugboardCycles = "";
        }
        Permutation plugboard = new Permutation(plugboardCycles, _alphabet);
        M.setPlugboard(plugboard);
        M.updateSetUpStatus(true);

    }

    /** Print MSG in groups of five (except that the last group may
     *  have fewer letters). */
    private void printMessageLine(String msg) {
        int groupIdx = 0;
        for (int i = 0; i < msg.length(); i++) {
            if (groupIdx > 4)  {
                _output.print(" ");
                groupIdx = 0;
            }
            _output.print(msg.charAt(i));
            groupIdx++;
        }
        _output.println();
    }

    /** Alphabet used in this machine. */
    private Alphabet _alphabet;

    /** Return the alphabet used in this machine. */
    Alphabet getAlphabet() {
        return _alphabet;
    }

    /** Source of input messages. */
    private Scanner _input;

    /** Return the source of input messages. */
    Scanner getInput() {
        return _input;
    }

    /** Source of machine configuration. */
    private Scanner _config;

    /** Return the source of machine configuration. */
    Scanner getConfig() {
        return _config;
    }

    /** File for encoded/decoded messages. */
    private PrintStream _output;

    /** Return the output message. */
    PrintStream getOutput() {
        return _output;
    }

}

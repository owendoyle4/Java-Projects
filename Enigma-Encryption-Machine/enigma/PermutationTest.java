package enigma;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.Timeout;
import static org.junit.Assert.*;

import static enigma.TestUtils.*;

/** The suite of all JUnit tests for the Permutation class.
 *  @Owen Doyle
 */
public class PermutationTest {

    /** Testing time limit. */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(5);

    /* ***** TESTING UTILITIES ***** */

    private Permutation perm;
    private String alpha = UPPER_STRING;

    /** Check that PERM has an ALPHABET whose size is that of
     *  FROMALPHA and TOALPHA and that maps each character of
     *  FROMALPHA to the corresponding character of FROMALPHA, and
     *  vice-versa. TESTID is used in error messages. */
    private void checkPerm(String testId,
                           String fromAlpha, String toAlpha,
                           Permutation p, Alphabet a) {
        int N = fromAlpha.length();
        assertEquals(testId + " (wrong length)", N, p.size());
        for (int i = 0; i < N; i += 1) {
            char c = fromAlpha.charAt(i), e = toAlpha.charAt(i);
            assertEquals(msg(testId, "wrong translation of '%c'", c),
                    e, p.permute(c));
            assertEquals(msg(testId, "wrong inverse of '%c'", e),
                    c, p.invert(e));
            int ci = a.toInt(c), ei = a.toInt(e);
            assertEquals(msg(testId, "wrong translation of %d", ci),
                    ei, p.permute(ci));
            assertEquals(msg(testId, "wrong inverse of %d", ei),
                    ci, p.invert(ei));
        }
    }

    /**
     * For this lab, you must use this to get a new Permutation,
     * the equivalent to:
     * new Permutation(cycles, alphabet)
     * @return a Permutation with cycles as its cycles and alphabet as
     * its alphabet
     * @see Permutation for description of the Permutation conctructor
     */
    Permutation getNewPermutation(String cycles, Alphabet alphabet) {
        return new Permutation(cycles, alphabet);
    }

    /**
     * For this lab, you must use this to get a new Alphabet,
     * the equivalent to:
     * new Alphabet(chars)
     * @return an Alphabet with chars as its characters
     * @see Alphabet for description of the Alphabet constructor
     */
    public Alphabet getNewAlphabet(String chars) {
        return new Alphabet(chars);
    }

    /**
     * For this lab, you must use this to get a new Alphabet,
     * the equivalent to:
     * new Alphabet()
     * @return a default Alphabet with characters ABCD...Z
     * @see Alphabet for description of the Alphabet constructor
     */
    Alphabet getNewAlphabet() {
        return new Alphabet();
    }

    /* ***** TESTS ***** */

    @Test
    public void checkIdTransform() {
        Alphabet alpha1 = getNewAlphabet();
        Permutation perm1 = getNewPermutation("", alpha1);
        checkPerm("identity", UPPER_STRING, UPPER_STRING, perm1, alpha1);
    }

    @Test
    public void testInvertChar() {
        Permutation p = getNewPermutation("(BACD)",
                getNewAlphabet("ABCD"));
        assertEquals('B', p.invert('A'));
        assertEquals(1, p.invert(0));

        assertEquals('D', p.invert('B'));
        assertEquals(3, p.invert(1));

        assertEquals(4, p.alphabet().size());
    }

    @Test
    public void testPermuteChar() {
        Permutation p = getNewPermutation("(BACD)",
                getNewAlphabet("ABCD"));
        assertEquals('A', p.permute('B'));
        assertEquals(0, p.permute(1));

        assertEquals('B', p.permute('D'));
        assertEquals(1, p.permute(3));

        assertEquals(4, p.alphabet().size());
    }

    @Test
    public void testPermuteInvertAndSizeChar() {
        Alphabet a = getNewAlphabet();
        Permutation p = getNewPermutation("(ABZ) (O) (MN)", a);
        checkPerm("check real permutation",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                "BZCDEFGHIJKLNMOPQRSTUVWXYA", p, a);

        assertEquals(26, p.alphabet().size());
    }

    @Test
    public void testSize() {
        Permutation p = getNewPermutation("(BACD)",
                getNewAlphabet("ABCD"));
        assertEquals(4, p.size());

        Permutation p1 = getNewPermutation("",
                getNewAlphabet("ABCDEFG"));
        assertEquals(7, p1.size());

        Permutation p2 = getNewPermutation("",
                getNewAlphabet(""));
        assertEquals(0, p2.size());
    }

    @Test
    public void testAlphabet() {
        Permutation p1 = getNewPermutation("(BACD)",
                getNewAlphabet("ABCD"));
        p1.alphabet();
        assertEquals(4, p1.alphabet().size());
        assertEquals(4, p1.size());

        Permutation p2 = getNewPermutation("",
                getNewAlphabet(""));
        p2.alphabet();
        assertEquals(0, p2.alphabet().size());
    }

    @Test
    public void testDerangement() {
        Permutation p = getNewPermutation("(BACD)",
                getNewAlphabet("ABCD"));
        assertTrue(p.derangement());

        Permutation p1 = getNewPermutation("(BAC) (D)",
                getNewAlphabet("ABCD"));
        assertFalse(p1.derangement());

        Permutation p2 = getNewPermutation("(BAC)",
                getNewAlphabet("ABCD"));
        assertFalse(p2.derangement());
    }

    @Test(expected = EnigmaException.class)
    public void testNestedCycle() {
        Alphabet a = getNewAlphabet("ABCD");
        Permutation p1 = getNewPermutation("(AB(CD))", a);
    }

    @Test(expected = EnigmaException.class)
    public void testNotInAlphabet() {
        Permutation p1 = getNewPermutation("(BACD)",
                getNewAlphabet("ABCD"));
        p1.invert('F');
        p1.invert(8);
        p1.permute(6);
        p1.permute('J');
    }

    @Test(expected = EnigmaException.class)
    public void testBadCycle() {
        Alphabet a = getNewAlphabet("ABCD");
        Permutation p1 = getNewPermutation("(ABE)", a);
    }

    @Test(expected = EnigmaException.class)
    public void testRepeatCycle() {
        Alphabet a = getNewAlphabet("ABCD");
        Permutation p2 = getNewPermutation("(BB)", a);
    }

    @Test(expected = EnigmaException.class)
    public void testCycleTooLong() {
        Alphabet a = getNewAlphabet("ABCD");

        Permutation p2 = getNewPermutation("(ABCDE)", a);
    }

    @Test(expected = EnigmaException.class)
    public void testMalformedCycle() {
        Alphabet a = getNewAlphabet("ABCD");
        Permutation p1 = getNewPermutation("(A)(", a);
        Permutation p2 = getNewPermutation("(A))", a);
    }

    @Test(expected = EnigmaException.class)
    public void testCycleWhitespace() {
        Alphabet a = getNewAlphabet("ABCD");
        Permutation p1 = getNewPermutation("(AB)(CD)", a);
        Permutation p2 = getNewPermutation("(A B)", a);
        Permutation p3 = getNewPermutation("()", a);
        Permutation p4 = getNewPermutation("( )", a);
    }

    @Test
    public void testSingleCycle() {
        Permutation p = getNewPermutation("(A) (B) (C)",
                getNewAlphabet("ABCD"));
        assertEquals('B', p.permute('B'));
        assertEquals('B', p.invert('B'));
        assertEquals(1,  p.permute(1));
        assertEquals(1, p.invert(1));

        assertEquals('D', p.permute('D'));
        assertEquals('D', p.invert('D'));
        assertEquals(3, p.permute(3));
        assertEquals(3, p.invert(3));
    }

}


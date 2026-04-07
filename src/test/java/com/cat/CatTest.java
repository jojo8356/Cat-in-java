package com.cat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests based on the GNU coreutils test suite for cat.
 * <p>
 * Sources:
 * - GNU coreutils tests/cat/ (cat-E.sh, cat-buf.sh, cat-proc.sh, cat-self.sh)
 * - uutils/coreutils tests/by-util/test_cat.rs
 */
class CatTest {

  @TempDir
  Path tempDir;

  private ByteArrayOutputStream outStream;
  private ByteArrayOutputStream errStream;
  private PrintStream originalOut;
  private PrintStream originalErr;
  private PrintStream originalIn;

  @BeforeEach
  void setUp() {
    outStream = new ByteArrayOutputStream();
    errStream = new ByteArrayOutputStream();
    originalOut = System.out;
    originalErr = System.err;
  }

  /** Run cat with given args and stdin content, return captured stdout. */
  private String runCat(String stdinContent, String... args) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    var oldOut = System.out;
    var oldErr = System.err;
    var oldIn = System.in;
    try {
      System.setOut(new PrintStream(out));
      System.setErr(new PrintStream(err));
      if (stdinContent != null) {
        System.setIn(new ByteArrayInputStream(stdinContent.getBytes(StandardCharsets.UTF_8)));
      }
      Main.main(args);
    } finally {
      System.setOut(oldOut);
      System.setErr(oldErr);
      System.setIn(oldIn);
    }
    return out.toString(StandardCharsets.UTF_8);
  }

  /**
   * Run cat with given args and raw bytes as stdin, return captured raw bytes.
   */
  private byte[] runCatBytes(byte[] stdinBytes, String... args) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    var oldOut = System.out;
    var oldErr = System.err;
    var oldIn = System.in;
    try {
      System.setOut(new PrintStream(out));
      System.setErr(new PrintStream(new ByteArrayOutputStream()));
      if (stdinBytes != null) {
        System.setIn(new ByteArrayInputStream(stdinBytes));
      }
      Main.main(args);
    } finally {
      System.setOut(oldOut);
      System.setErr(oldErr);
      System.setIn(oldIn);
    }
    return out.toByteArray();
  }

  /** Get stderr from last runCat call. */
  private String runCatGetErr(String stdinContent, String... args) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    var oldOut = System.out;
    var oldErr = System.err;
    var oldIn = System.in;
    try {
      System.setOut(new PrintStream(out));
      System.setErr(new PrintStream(err));
      if (stdinContent != null) {
        System.setIn(new ByteArrayInputStream(stdinContent.getBytes(StandardCharsets.UTF_8)));
      }
      Main.main(args);
    } finally {
      System.setOut(oldOut);
      System.setErr(oldErr);
      System.setIn(oldIn);
    }
    return err.toString(StandardCharsets.UTF_8);
  }

  private Path createFile(String name, String content) throws IOException {
    Path file = tempDir.resolve(name);
    Files.writeString(file, content);
    return file;
  }

  private Path createFileBytes(String name, byte[] content) throws IOException {
    Path file = tempDir.resolve(name);
    Files.write(file, content);
    return file;
  }

  // =========================================================================
  // Basic I/O -- no options
  // =========================================================================

  @Nested
  class NoOptions {

    @Test
    void readFromStdin() {
      String result = runCat("hello world\n");
      assertEquals("hello world\n", result);
    }

    @Test
    void readSingleFile() throws IOException {
      Path f = createFile("input.txt", "hello\nworld\n");
      String result = runCat(null, f.toString());
      assertEquals("hello\nworld\n", result);
    }

    @Test
    void readMultipleFiles() throws IOException {
      Path f1 = createFile("a.txt", "aaa\n");
      Path f2 = createFile("b.txt", "bbb\n");
      String result = runCat(null, f1.toString(), f2.toString());
      assertEquals("aaa\nbbb\n", result);
    }

    @Test
    void emptyFile() throws IOException {
      Path f = createFile("empty.txt", "");
      String result = runCat(null, f.toString());
      assertEquals("", result);
    }

    @Test
    void noTrailingNewline() throws IOException {
      Path f = createFile("notail.txt", "no newline");
      String result = runCat(null, f.toString());
      assertEquals("no newline", result);
    }

    @Test
    void dashMeansStdin() {
      String result = runCat("from stdin\n", "-");
      assertEquals("from stdin\n", result);
    }

    @Test
    void mixFilesAndStdin() throws IOException {
      Path f = createFile("file.txt", "from file\n");
      String result = runCat("from stdin\n", f.toString(), "-");
      assertEquals("from file\nfrom stdin\n", result);
    }

    @Test
    void largeInput() {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 10000; i++) {
        sb.append("line ").append(i).append("\n");
      }
      String input = sb.toString();
      String result = runCat(input);
      assertEquals(input, result);
    }
  }

  // =========================================================================
  // -n : number all output lines
  // =========================================================================

  @Nested
  class NumberLines {

    @Test
    void numberAllLines() {
      String result = runCat("a\nb\nc\n", "-n");
      assertEquals("     1\ta\n     2\tb\n     3\tc\n", result);
    }

    @Test
    void numberLinesIncludingBlanks() {
      String result = runCat("a\n\nb\n", "-n");
      assertEquals("     1\ta\n     2\t\n     3\tb\n", result);
    }

    @Test
    void numberLinesNoTrailingNewline() {
      String result = runCat("a\nb", "-n");
      assertEquals("     1\ta\n     2\tb", result);
    }

    @Test
    void numberLinesMultipleFiles() throws IOException {
      Path f1 = createFile("a.txt", "a\n");
      Path f2 = createFile("b.txt", "b\n");
      String result = runCat(null, "-n", f1.toString(), f2.toString());
      assertEquals("     1\ta\n     2\tb\n", result);
    }

    @Test
    void numberLinesCrlf() {
      String result = runCat("a\r\nb\r\n", "-n");
      assertEquals("     1\ta\r\n     2\tb\r\n", result);
    }
  }

  // =========================================================================
  // -b : number non-blank lines only
  // =========================================================================

  @Nested
  class NumberNonBlankLines {

    @Test
    void numberNonBlank() {
      String result = runCat("a\n\nb\n\nc\n", "-b");
      assertEquals("     1\ta\n\n     2\tb\n\n     3\tc\n", result);
    }

    @Test
    void bOverridesN() {
      String result = runCat("a\n\nb\n", "-n", "-b");
      assertEquals("     1\ta\n\n     2\tb\n", result);
    }

    @Test
    void bOverridesNReversed() {
      String result = runCat("a\n\nb\n", "-b", "-n");
      assertEquals("     1\ta\n\n     2\tb\n", result);
    }

    @Test
    void multipleBlankLines() {
      String result = runCat("a\n\n\n\nb\n", "-b");
      assertEquals("     1\ta\n\n\n\n     2\tb\n", result);
    }
  }

  // =========================================================================
  // -s : squeeze blank lines
  // =========================================================================

  @Nested
  class SqueezeBlank {

    @Test
    void squeezeConsecutiveBlanks() {
      String result = runCat("a\n\n\n\nb\n", "-s");
      assertEquals("a\n\nb\n", result);
    }

    @Test
    void singleBlankNotAffected() {
      String result = runCat("a\n\nb\n", "-s");
      assertEquals("a\n\nb\n", result);
    }

    @Test
    void leadingBlanks() {
      String result = runCat("\n\n\na\n", "-s");
      assertEquals("\na\n", result);
    }

    @Test
    void trailingBlanks() {
      String result = runCat("a\n\n\n\n", "-s");
      assertEquals("a\n\n", result);
    }

    @Test
    void squeezeAcrossFiles() throws IOException {
      Path f1 = createFile("a.txt", "a\n\n\n");
      Path f2 = createFile("b.txt", "\n\nb\n");
      String result = runCat(null, "-s", f1.toString(), f2.toString());
      assertEquals("a\n\nb\n", result);
    }

    @Test
    void squeezeWithNumbering() {
      // -s applies before -n (GNU behavior)
      String result = runCat("a\n\n\n\nb\n", "-sn");
      assertEquals("     1\ta\n     2\t\n     3\tb\n", result);
    }

    @Test
    void allBlanks() {
      String result = runCat("\n\n\n\n", "-s");
      assertEquals("\n", result);
    }

    @Test
    void repeatedSFlag() {
      String result = runCat("a\n\n\n\nb\n", "-s", "-s");
      assertEquals("a\n\nb\n", result);
    }
  }

  // =========================================================================
  // -E : show ends ($)
  // =========================================================================

  @Nested
  class ShowEnds {

    @Test
    void appendDollarSign() {
      String result = runCat("hello\nworld\n", "-E");
      assertEquals("hello$\nworld$\n", result);
    }

    @Test
    void noTrailingNewline() {
      String result = runCat("hello\nworld", "-E");
      assertEquals("hello$\nworld", result);
    }

    @Test
    void emptyLines() {
      String result = runCat("a\n\nb\n", "-E");
      assertEquals("a$\n$\nb$\n", result);
    }

    /**
     * GNU cat-E.sh: \r\n should display as ^M$ (with -vE or -A).
     */
    @Test
    void crlfShowsAsCaretMDollar() {
      // With -E alone, \r is not transformed (only -v does that)
      // but \n gets $
      String result = runCat("a\r\nb\r\n", "-E");
      assertEquals("a\r$\nb\r$\n", result);
    }

    /**
     * GNU cat-E.sh: with -vE, \r\n shows as ^M$
     */
    @Test
    void crlfWithVE() {
      String result = runCat("a\r\nb\r\n", "-vE");
      assertEquals("a^M$\nb^M$\n", result);
    }
  }

  // =========================================================================
  // -T : show tabs as ^I
  // =========================================================================

  @Nested
  class ShowTabs {

    @Test
    void tabsDisplayedAsCaretI() {
      String result = runCat("a\tb\n", "-T");
      assertEquals("a^Ib\n", result);
    }

    @Test
    void multipleTabs() {
      String result = runCat("\t\t\n", "-T");
      assertEquals("^I^I\n", result);
    }

    @Test
    void tabsNoTrailingNewline() {
      String result = runCat("a\tb", "-T");
      assertEquals("a^Ib", result);
    }
  }

  // =========================================================================
  // -v : show non-printing characters
  // =========================================================================

  @Nested
  class ShowNonPrinting {

    @Test
    void controlCharacters() {
      // NUL (0x00) -> ^@, SOH (0x01) -> ^A, BEL (0x07) -> ^G
      byte[] input = new byte[] { 0x00, 0x01, 0x07, 0x0A }; // ^@^A^G\n
      byte[] output = runCatBytes(input, "-v");
      String result = new String(output, StandardCharsets.UTF_8);
      assertEquals("^@^A^G\n", result);
    }

    @Test
    void carriageReturnDisplayed() {
      String result = runCat("a\rb\n", "-v");
      assertEquals("a^Mb\n", result);
    }

    @Test
    void delCharacter() {
      // DEL (0x7F) -> ^?
      byte[] input = new byte[] { 0x7F, 0x0A };
      byte[] output = runCatBytes(input, "-v");
      String result = new String(output, StandardCharsets.UTF_8);
      assertEquals("^?\n", result);
    }

    @Test
    void highBytesDisplayed() {
      // 0x80 -> M-^@, 0x9A -> M-^Z, 0xFF -> M-^?
      byte[] input = new byte[] { (byte) 0x80, 0x0A };
      byte[] output = runCatBytes(input, "-v");
      String result = new String(output, StandardCharsets.UTF_8);
      assertEquals("M-^@\n", result);
    }

    @Test
    void highPrintableBytes() {
      // 0xC0 -> M-@, 0xE9 -> M-i
      byte[] input = new byte[] { (byte) 0xC0, 0x0A };
      byte[] output = runCatBytes(input, "-v");
      String result = new String(output, StandardCharsets.UTF_8);
      assertEquals("M-@\n", result);
    }

    @Test
    void tabAndNewlineNotAffected() {
      // -v does NOT transform \t (0x09) or \n (0x0A)
      String result = runCat("a\tb\n", "-v");
      assertEquals("a\tb\n", result);
    }
  }

  // =========================================================================
  // -A : equivalent to -vET
  // =========================================================================

  @Nested
  class ShowAll {

    @Test
    void showAllEquivalentToVET() {
      String result = runCat("a\tb\r\n", "-A");
      assertEquals("a^Ib^M$\n", result);
    }

    @Test
    void showAllWithControlChars() {
      byte[] input = new byte[] { 0x01, 0x09, 0x0A }; // ^A, TAB, LF
      byte[] output = runCatBytes(input, "-A");
      String result = new String(output, StandardCharsets.UTF_8);
      assertEquals("^A^I$\n", result);
    }

    @Test
    void showAllMultipleFilesWithNumbering() throws IOException {
      Path f1 = createFile("a.txt", "a\tb\n");
      Path f2 = createFile("b.txt", "c\n");
      String result = runCat(null, "-An", f1.toString(), f2.toString());
      assertEquals("     1\ta^Ib$\n     2\tc$\n", result);
    }
  }

  // =========================================================================
  // -e : equivalent to -vE
  // =========================================================================

  @Nested
  class NonPrintingAndEnds {

    @Test
    void eEquivalentToVE() {
      String result = runCat("a\r\n", "-e");
      assertEquals("a^M$\n", result);
    }

    @Test
    void eRepeated() {
      // -e -e should behave same as -e
      String result = runCat("a\r\n", "-e", "-e");
      assertEquals("a^M$\n", result);
    }

    @Test
    void eDoesNotAffectTabs() {
      String result = runCat("a\tb\n", "-e");
      assertEquals("a\tb$\n", result);
    }
  }

  // =========================================================================
  // -t : equivalent to -vT
  // =========================================================================

  @Nested
  class NonPrintingAndTabs {

    @Test
    void tEquivalentToVT() {
      String result = runCat("a\tb\r\n", "-t");
      assertEquals("a^Ib^M\n", result);
    }

    @Test
    void tRepeated() {
      String result = runCat("a\tb\r\n", "-t", "-t");
      assertEquals("a^Ib^M\n", result);
    }

    @Test
    void tDoesNotAffectLineEnds() {
      String result = runCat("a\tb\n", "-t");
      assertEquals("a^Ib\n", result);
    }
  }

  // =========================================================================
  // -u : accepted but ignored (POSIX)
  // =========================================================================

  @Nested
  class Unbuffered {

    @Test
    void uFlagAccepted() {
      String result = runCat("hello\n", "-u");
      assertEquals("hello\n", result);
    }

    @Test
    void uWithOtherFlags() {
      String result = runCat("a\n\nb\n", "-un");
      assertEquals("     1\ta\n     2\t\n     3\tb\n", result);
    }
  }

  // =========================================================================
  // Combined flags
  // =========================================================================

  @Nested
  class CombinedFlags {

    @Test
    void squeezeAndNumber() {
      String result = runCat("a\n\n\n\nb\n", "-sn");
      assertEquals("     1\ta\n     2\t\n     3\tb\n", result);
    }

    @Test
    void squeezeAndNumberNonBlank() {
      String result = runCat("a\n\n\n\nb\n", "-sb");
      assertEquals("     1\ta\n\n     2\tb\n", result);
    }

    @Test
    void allFlagsCombined() {
      String result = runCat("a\t\r\n\n\n\nb\n", "-Asn");
      assertEquals("     1\ta^I^M$\n     2\t$\n     3\tb$\n", result);
    }

    @Test
    void numberAndShowEnds() {
      String result = runCat("hello\nworld\n", "-nE");
      assertEquals("     1\thello$\n     2\tworld$\n", result);
    }

    @Test
    void numberAndShowTabs() {
      String result = runCat("a\tb\n", "-nT");
      assertEquals("     1\ta^Ib\n", result);
    }
  }

  // =========================================================================
  // GNU cat-E.sh: \r\n edge cases
  // =========================================================================

  @Nested
  class CRLFEdgeCases {

    /**
     * GNU cat-E.sh: \r\n spanning buffer boundary should still show ^M$
     */
    @Test
    void crlfAcrossFiles() throws IOException {
      Path f1 = createFileBytes("cr.bin", new byte[] { 'a', '\r' });
      Path f2 = createFileBytes("lf.bin", new byte[] { '\n', 'b', '\n' });
      String result = runCat(null, "-A", f1.toString(), f2.toString());
      assertEquals("a^M$\nb$\n", result);
    }

    @Test
    void loneCR() {
      String result = runCat("a\rb\n", "-A");
      assertEquals("a^Mb$\n", result);
    }

    @Test
    void multipleCRBeforeLF() {
      String result = runCat("a\r\r\n", "-A");
      assertEquals("a^M^M$\n", result);
    }

    @Test
    void crOnly() {
      String result = runCat("a\rb\rc\n", "-vE");
      assertEquals("a^Mb^Mc$\n", result);
    }
  }

  // =========================================================================
  // Error handling
  // =========================================================================

  @Nested
  class ErrorHandling {

    @Test
    void nonExistentFile() {
      String err = runCatGetErr(null, "/nonexistent/file.txt");
      assertTrue(err.contains("No such file") || err.contains("not found") || err.contains("No existe"),
          "Expected error message for missing file, got: " + err);
    }

    @Test
    void directoryArgument() throws IOException {
      Path dir = tempDir.resolve("subdir");
      Files.createDirectory(dir);
      String err = runCatGetErr(null, dir.toString());
      assertTrue(err.contains("directory") || err.contains("Is a directory") || err.contains("répertoire"),
          "Expected error message for directory, got: " + err);
    }

    @Test
    void directoryAndFileArgs() throws IOException {
      Path dir = tempDir.resolve("subdir");
      Files.createDirectory(dir);
      Path file = createFile("ok.txt", "hello\n");
      // Should output file content and error for directory
      String result = runCat(null, dir.toString(), file.toString());
      assertEquals("hello\n", result);
    }

    @Test
    void multipleNonExistentFiles() {
      String err = runCatGetErr(null, "/no/a.txt", "/no/b.txt");
      // Should report errors for both files
      assertFalse(err.isEmpty(), "Expected error messages for missing files");
    }
  }

  // =========================================================================
  // GNU cat-self.sh: input/output same file detection
  // =========================================================================

  @Nested
  class SameFileDetection {

    @Test
    void readingFileNormally() throws IOException {
      Path f = createFile("normal.txt", "content\n");
      String result = runCat(null, f.toString());
      assertEquals("content\n", result);
    }
  }

  // =========================================================================
  // Edge cases
  // =========================================================================

  @Nested
  class EdgeCases {

    @Test
    void onlyNewlines() {
      String result = runCat("\n\n\n", "-n");
      assertEquals("     1\t\n     2\t\n     3\t\n", result);
    }

    @Test
    void singleCharNoNewline() {
      String result = runCat("x");
      assertEquals("x", result);
    }

    @Test
    void emptyStdin() {
      String result = runCat("");
      assertEquals("", result);
    }

    @Test
    void longLines() {
      String longLine = "x".repeat(100_000) + "\n";
      String result = runCat(longLine);
      assertEquals(longLine, result);
    }

    @Test
    void binaryContent() throws IOException {
      // All 256 byte values except \n
      byte[] input = new byte[256];
      for (int i = 0; i < 256; i++) {
        input[i] = (byte) i;
      }
      // Just verify it doesn't crash
      byte[] output = runCatBytes(input);
      assertEquals(input.length, output.length);
    }

    @Test
    void manyFiles() throws IOException {
      StringBuilder expected = new StringBuilder();
      String[] args = new String[50];
      for (int i = 0; i < 50; i++) {
        Path f = createFile("f" + i + ".txt", "line" + i + "\n");
        args[i] = f.toString();
        expected.append("line").append(i).append("\n");
      }
      String result = runCat(null, args);
      assertEquals(expected.toString(), result);
    }

    @Test
    void numberingContinuesAcrossFiles() throws IOException {
      Path f1 = createFile("a.txt", "a\nb\n");
      Path f2 = createFile("b.txt", "c\nd\n");
      String result = runCat(null, "-n", f1.toString(), f2.toString());
      assertEquals("     1\ta\n     2\tb\n     3\tc\n     4\td\n", result);
    }

    @Test
    void numberNonBlankContinuesAcrossFiles() throws IOException {
      Path f1 = createFile("a.txt", "a\n\n");
      Path f2 = createFile("b.txt", "\nb\n");
      String result = runCat(null, "-b", f1.toString(), f2.toString());
      assertEquals("     1\ta\n\n\n     2\tb\n", result);
    }

    @Test
    void squeezeBlankWithNonBlankNumbering() {
      String result = runCat("a\n\n\n\n\nb\n\n\nc\n", "-sb");
      assertEquals("     1\ta\n\n     2\tb\n\n     3\tc\n", result);
    }

    @Test
    void windowsLineEndings() {
      String result = runCat("a\r\nb\r\nc\r\n", "-n");
      assertEquals("     1\ta\r\n     2\tb\r\n     3\tc\r\n", result);
    }

    @Test
    void mixedLineEndings() {
      String result = runCat("a\nb\r\nc\rd\n", "-nv");
      assertEquals("     1\ta\n     2\tb^M\n     3\tc^Md\n", result);
    }
  }
}

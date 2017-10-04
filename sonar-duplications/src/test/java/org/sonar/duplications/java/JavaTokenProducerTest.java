/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.duplications.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.duplications.DuplicationsTestUtil;
import org.sonar.duplications.token.Token;
import org.sonar.duplications.token.TokenChunker;
import org.sonar.duplications.token.TokenQueue;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaTokenProducerTest {

  private final TokenChunker chunker = JavaTokenProducer.build();

  /**
   * <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.6">White Space</a>
   */
  @Test
  public void shouldIgnoreWhitespaces() {
    assertThat(chunk(" \t\f\n\r")).containsExactly();
  }

  /**
   * <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.7">Comments</a>
   */
  @Test
  public void shouldIgnoreEndOfLineComment() {
    assertThat(chunk("// This is a comment")).containsExactly();
    assertThat(chunk("// This is a comment \n and_this_is_not")).containsExactly(new Token("and_this_is_not", 2, 1));
  }

  @Test
  public void shouldIgnoreTraditionalComment() {
    assertThat(chunk("/* This is a comment \n and the second line */")).containsExactly();
    assertThat(chunk("/** This is a javadoc \n and the second line */")).containsExactly();
    assertThat(chunk("/* this \n comment /* \n // /** ends \n here: */")).containsExactly();
  }

  /**
   * <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.8">Identifiers</a>
   */
  @Test
  public void shouldPreserveIdentifiers() {
    assertThat(chunk("String")).containsExactly(new Token("String", 1, 0));
    assertThat(chunk("i3")).containsExactly(new Token("i3", 1, 0));
    assertThat(chunk("MAX_VALUE")).containsExactly(new Token("MAX_VALUE", 1, 0));
    assertThat(chunk("isLetterOrDigit")).containsExactly(new Token("isLetterOrDigit", 1, 0));

    assertThat(chunk("_")).containsExactly(new Token("_", 1, 0));
    assertThat(chunk("_123_")).containsExactly(new Token("_123_", 1, 0));
    assertThat(chunk("_Field")).containsExactly(new Token("_Field", 1, 0));
    assertThat(chunk("_Field5")).containsExactly(new Token("_Field5", 1, 0));

    assertThat(chunk("$")).containsExactly(new Token("$", 1, 0));
    assertThat(chunk("$field")).containsExactly(new Token("$field", 1, 0));

    assertThat(chunk("i2j")).containsExactly(new Token("i2j", 1, 0));
    assertThat(chunk("from1to4")).containsExactly(new Token("from1to4", 1, 0));

    assertThat(chunk("αβγ")).as("identifier with unicode").containsExactly(new Token("αβγ", 1, 0));
  }

  /**
   * <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.9">Keywords</a>
   */
  @Test
  public void shouldPreserverKeywords() {
    assertThat(chunk("private static final")).containsExactly(new Token("private", 1, 0), new Token("static", 1, 8), new Token("final", 1, 15));
  }

  /**
   * <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.1">Integer Literals</a>
   */
  @Test
  public void shouldNormalizeDecimalIntegerLiteral() {
    assertThat(chunk("543")).containsExactly(numericLiteral());
    assertThat(chunk("543l")).containsExactly(numericLiteral());
    assertThat(chunk("543L")).containsExactly(numericLiteral());
  }

  @Test
  public void shouldNormalizeOctalIntegerLiteral() {
    assertThat(chunk("077")).containsExactly(numericLiteral());
    assertThat(chunk("077l")).containsExactly(numericLiteral());
    assertThat(chunk("077L")).containsExactly(numericLiteral());
  }

  @Test
  public void shouldNormalizeHexIntegerLiteral() {
    assertThat(chunk("0xFF")).containsExactly(numericLiteral());
    assertThat(chunk("0xFFl")).containsExactly(numericLiteral());
    assertThat(chunk("0xFFL")).containsExactly(numericLiteral());

    assertThat(chunk("0XFF")).containsExactly(numericLiteral());
    assertThat(chunk("0XFFl")).containsExactly(numericLiteral());
    assertThat(chunk("0XFFL")).containsExactly(numericLiteral());
  }

  /**
   * New in Java 7.
   */
  @Test
  public void shouldNormalizeBinaryIntegerLiteral() {
    assertThat(chunk("0b10")).containsExactly(numericLiteral());
    assertThat(chunk("0b10l")).containsExactly(numericLiteral());
    assertThat(chunk("0b10L")).containsExactly(numericLiteral());

    assertThat(chunk("0B10")).containsExactly(numericLiteral());
    assertThat(chunk("0B10l")).containsExactly(numericLiteral());
    assertThat(chunk("0B10L")).containsExactly(numericLiteral());
  }

  /**
   * <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.2">Floating-Point Literals</a>
   */
  @Test
  public void shouldNormalizeDecimalFloatingPointLiteral() {
    // with dot at the end
    assertThat(chunk("1234.")).containsExactly(numericLiteral());
    assertThat(chunk("1234.E1")).containsExactly(numericLiteral());
    assertThat(chunk("1234.e+1")).containsExactly(numericLiteral());
    assertThat(chunk("1234.E-1")).containsExactly(numericLiteral());
    assertThat(chunk("1234.f")).containsExactly(numericLiteral());

    // with dot between
    assertThat(chunk("12.34")).containsExactly(numericLiteral());
    assertThat(chunk("12.34E1")).containsExactly(numericLiteral());
    assertThat(chunk("12.34e+1")).containsExactly(numericLiteral());
    assertThat(chunk("12.34E-1")).containsExactly(numericLiteral());

    assertThat(chunk("12.34f")).containsExactly(numericLiteral());
    assertThat(chunk("12.34E1F")).containsExactly(numericLiteral());
    assertThat(chunk("12.34E+1d")).containsExactly(numericLiteral());
    assertThat(chunk("12.34e-1D")).containsExactly(numericLiteral());

    // with dot at the beginning
    assertThat(chunk(".1234")).containsExactly(numericLiteral());
    assertThat(chunk(".1234e1")).containsExactly(numericLiteral());
    assertThat(chunk(".1234E+1")).containsExactly(numericLiteral());
    assertThat(chunk(".1234E-1")).containsExactly(numericLiteral());

    assertThat(chunk(".1234f")).containsExactly(numericLiteral());
    assertThat(chunk(".1234E1F")).containsExactly(numericLiteral());
    assertThat(chunk(".1234e+1d")).containsExactly(numericLiteral());
    assertThat(chunk(".1234E-1D")).containsExactly(numericLiteral());

    // without dot
    assertThat(chunk("1234e1")).containsExactly(numericLiteral());
    assertThat(chunk("1234E+1")).containsExactly(numericLiteral());
    assertThat(chunk("1234E-1")).containsExactly(numericLiteral());

    assertThat(chunk("1234E1f")).containsExactly(numericLiteral());
    assertThat(chunk("1234e+1d")).containsExactly(numericLiteral());
    assertThat(chunk("1234E-1D")).containsExactly(numericLiteral());
  }

  @Test
  public void shouldNormalizeHexadecimalFloatingPointLiteral() {
    // with dot at the end
    assertThat(chunk("0xAF.")).containsExactly(numericLiteral());
    assertThat(chunk("0XAF.P1")).containsExactly(numericLiteral());
    assertThat(chunk("0xAF.p+1")).containsExactly(numericLiteral());
    assertThat(chunk("0XAF.p-1")).containsExactly(numericLiteral());
    assertThat(chunk("0xAF.f")).containsExactly(numericLiteral());

    // with dot between
    assertThat(chunk("0XAF.BC")).containsExactly(numericLiteral());
    assertThat(chunk("0xAF.BCP1")).containsExactly(numericLiteral());
    assertThat(chunk("0XAF.BCp+1")).containsExactly(numericLiteral());
    assertThat(chunk("0xAF.BCP-1")).containsExactly(numericLiteral());

    assertThat(chunk("0xAF.BCf")).containsExactly(numericLiteral());
    assertThat(chunk("0xAF.BCp1F")).containsExactly(numericLiteral());
    assertThat(chunk("0XAF.BCP+1d")).containsExactly(numericLiteral());
    assertThat(chunk("0XAF.BCp-1D")).containsExactly(numericLiteral());

    // without dot
    assertThat(chunk("0xAFp1")).containsExactly(numericLiteral());
    assertThat(chunk("0XAFp+1")).containsExactly(numericLiteral());
    assertThat(chunk("0xAFp-1")).containsExactly(numericLiteral());

    assertThat(chunk("0XAFp1f")).containsExactly(numericLiteral());
    assertThat(chunk("0xAFp+1d")).containsExactly(numericLiteral());
    assertThat(chunk("0XAFp-1D")).containsExactly(numericLiteral());
  }

  /**
   * New in Java 7.
   */
  @Test
  public void shouldNormalizeNumericLiteralsWithUnderscores() {
    assertThat(chunk("54_3L")).containsExactly(numericLiteral());
    assertThat(chunk("07_7L")).containsExactly(numericLiteral());
    assertThat(chunk("0b1_0L")).containsExactly(numericLiteral());
    assertThat(chunk("0xF_FL")).containsExactly(numericLiteral());

    assertThat(chunk("1_234.")).containsExactly(numericLiteral());
    assertThat(chunk("1_2.3_4")).containsExactly(numericLiteral());
    assertThat(chunk(".1_234")).containsExactly(numericLiteral());
    assertThat(chunk("1_234e1_0")).containsExactly(numericLiteral());

    assertThat(chunk("0xA_F.")).containsExactly(numericLiteral());
    assertThat(chunk("0xA_F.B_C")).containsExactly(numericLiteral());
    assertThat(chunk("0x1.ffff_ffff_ffff_fP1_023")).containsExactly(numericLiteral());
  }

  /**
   * <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.3">Boolean Literals</a>
   */
  @Test
  public void shouldPreserveBooleanLiterals() {
    assertThat(chunk("true false")).containsExactly(new Token("true", 1, 0), new Token("false", 1, 5));
  }

  /**
   * <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.4">Character Literals</a>
   */
  @Test
  public void shouldNormalizeCharacterLiterals() {
    assertThat(chunk("'a'")).as("single character").containsExactly(stringLiteral());
    assertThat(chunk("'\\n'")).as("escaped LF").containsExactly(stringLiteral());
    assertThat(chunk("'\\''")).as("escaped quote").containsExactly(stringLiteral());
    assertThat(chunk("'\\177'")).as("octal escape").containsExactly(stringLiteral());
    assertThat(chunk("'\\u03a9'")).as("unicode escape").containsExactly(stringLiteral());
  }

  /**
   * <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.5">String Literals</a>
   */
  @Test
  public void shouldNormalizeStringLiterals() {
    assertThat(chunk("\"string\"")).as("regular string").containsExactly(stringLiteral());
    assertThat(chunk("\"\"")).as("empty string").containsExactly(stringLiteral());
    assertThat(chunk("\"\\n\"")).as("escaped LF").containsExactly(stringLiteral());
    assertThat(chunk("\"string, which contains \\\"escaped double quotes\\\"\"")).as("escaped double quotes").containsExactly(stringLiteral());
    assertThat(chunk("\"string \\177\"")).as("octal escape").containsExactly(stringLiteral());
    assertThat(chunk("\"string \\u03a9\"")).as("unicode escape").containsExactly(stringLiteral());
  }

  /**
   * <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.10.7">The Null Literal</a>
   */
  @Test
  public void shouldPreserverNullLiteral() {
    assertThat(chunk("null")).containsExactly(new Token("null", 1, 0));
  }

  /**
   * <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.11">Separators</a>
   */
  @Test
  public void shouldPreserveSeparators() {
    assertThat(chunk("(){}[];,.")).containsExactly(
      new Token("(", 1, 0), new Token(")", 1, 1),
      new Token("{", 1, 2), new Token("}", 1, 3),
      new Token("[", 1, 4), new Token("]", 1, 5),
      new Token(";", 1, 6), new Token(",", 1, 7),
      new Token(".", 1, 8));
  }

  /**
   * <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.12">Operators</a>
   */
  @Test
  public void shouldPreserveOperators() {
    assertThat(chunk("+=")).containsExactly(new Token("+", 1, 0), new Token("=", 1, 1));
    assertThat(chunk("--")).containsExactly(new Token("-", 1, 0), new Token("-", 1, 1));
  }

  @Test
  public void realExamples() {
    File testFile = DuplicationsTestUtil.findFile("/java/MessageResources.java");
    assertThat(chunk(testFile)).isNotEmpty();

    testFile = DuplicationsTestUtil.findFile("/java/RequestUtils.java");
    assertThat(chunk(testFile)).isNotEmpty();
  }

  private TokenQueue chunk(File file) {
    Reader reader = null;
    try {
      reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
      return chunker.chunk(reader);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  private static Token numericLiteral() {
    return new Token("$NUMBER", 1, 0);
  }

  private static Token stringLiteral() {
    return new Token("$CHARS", 1, 0);
  }

  private List<Token> chunk(String sourceCode) {
    List<Token> target = new ArrayList<>();
    chunker.chunk(sourceCode).forEach(target::add);
    return target;
  }

}

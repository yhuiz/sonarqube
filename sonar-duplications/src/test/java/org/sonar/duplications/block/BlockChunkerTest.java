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
package org.sonar.duplications.block;

import java.util.List;
import org.junit.Test;
import org.sonar.duplications.statement.Statement;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChunkerTest extends BlockChunkerTestCase {

  @Override
  protected BlockChunker createChunkerWithBlockSize(int blockSize) {
    return new BlockChunker(blockSize);
  }

  /**
   * Rolling hash must produce exactly the same values as without rolling behavior.
   * Moreover those values must always be the same (without dependency on JDK).
   */
  @Test
  public void shouldCalculateHashes() {
    List<Statement> statements = createStatementsFromStrings("aaaaaa", "bbbbbb", "cccccc", "dddddd", "eeeeee");
    BlockChunker blockChunker = createChunkerWithBlockSize(3);
    List<Block> blocks = blockChunker.chunk("resource", statements);
    assertThat(blocks.get(0).getBlockHash()).isEqualTo(hash("aaaaaa", "bbbbbb", "cccccc"));
    assertThat(blocks.get(1).getBlockHash()).isEqualTo(hash("bbbbbb", "cccccc", "dddddd"));
    assertThat(blocks.get(2).getBlockHash()).isEqualTo(hash("cccccc", "dddddd", "eeeeee"));
    assertThat(blocks.get(0).getBlockHash().toString()).isEqualTo("fffffeb6ae1af4c0");
    assertThat(blocks.get(1).getBlockHash().toString()).isEqualTo("fffffebd8512d120");
    assertThat(blocks.get(2).getBlockHash().toString()).isEqualTo("fffffec45c0aad80");
  }

  private ByteArray hash(String... statements) {
    long hash = 0;
    for (String statement : statements) {
      hash = hash * 31 + statement.hashCode();
    }
    return new ByteArray(hash);
  }

}

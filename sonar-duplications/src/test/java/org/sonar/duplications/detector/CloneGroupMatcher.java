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
package org.sonar.duplications.detector;

import java.util.function.Predicate;
import org.assertj.core.api.Condition;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;

public class CloneGroupMatcher {

  public static Condition<CloneGroup> hasCloneGroup(int expectedLen, ClonePart... expectedParts) {
    StringBuilder builder = new StringBuilder();
    for (ClonePart part : expectedParts) {
      builder.append(part).append(" - ");
    }
    builder.append(expectedLen);
    Predicate<CloneGroup> p = cloneGroup -> {
      return check(expectedLen, cloneGroup, expectedParts);
    };
    return new Condition<>(p, builder.toString());
  }

  private static boolean check(int expectedLen, CloneGroup cloneGroup, ClonePart... expectedParts) {
    // Check length
    if (expectedLen != cloneGroup.getCloneUnitLength()) {
      return false;
    }
    // Check number of parts
    if (expectedParts.length != cloneGroup.getCloneParts().size()) {
      return false;
    }
    // Check origin
    if (!expectedParts[0].equals(cloneGroup.getOriginPart())) {
      return false;
    }
    // Check parts
    for (ClonePart expectedPart : expectedParts) {
      boolean matched = false;
      for (ClonePart part : cloneGroup.getCloneParts()) {
        if (part.equals(expectedPart)) {
          matched = true;
          break;
        }
      }
      if (!matched) {
        return false;
      }
    }
    return true;
  }

}

/*
 * SonarQube PHP Plugin
 * Copyright (C) 2010 SonarSource and Akram Ben Aissi
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.php.checks;

import org.junit.Test;
import org.sonar.php.PHPAstScanner;
import org.sonar.plugins.php.CheckTest;
import org.sonar.plugins.php.TestUtils;
import org.sonar.squidbridge.api.SourceFile;

public class BooleanEqualityComparisonCheckTest extends CheckTest {

  @Test
  public void test() throws Exception {
    SourceFile file = PHPAstScanner.scanSingleFile(TestUtils.getCheckFile("BooleanEqualityComparisonCheck.php"), new BooleanEqualityComparisonCheck());

    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(3).withMessage("Remove the literal \"true\" boolean value.")
      .next().atLine(4).withMessage("Remove the literal \"false\" boolean value.")
      .next().atLine(5)
      .next().atLine(6)
      .next().atLine(7)
      .next().atLine(8)
      .next().atLine(9)
      .next().atLine(10)
      .next().atLine(11)
      .next().atLine(12)
      .next().atLine(13)
      .next().atLine(14)
      .next().atLine(15)
      .next().atLine(16)
      .next().atLine(17)
      .next().atLine(18)
      .next().atLine(18)
      .noMore();
  }
}

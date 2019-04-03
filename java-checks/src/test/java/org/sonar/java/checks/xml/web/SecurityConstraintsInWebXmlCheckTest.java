/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.checks.xml.web;

import org.junit.Test;
import org.sonar.java.checks.verifier.XmlCheckVerifier;
import org.sonar.java.xml.XmlCheck;

public class SecurityConstraintsInWebXmlCheckTest {

  private static final XmlCheck CHECK = new SecurityConstraintsInWebXmlCheck();

  @Test
  public void web_xml_without_constraints() {
    XmlCheckVerifier.verifyIssueOnFile(
      "src/test/files/checks/xml/web/SecurityConstraintsInWebXmlCheck/withoutSecurityConstraints/web.xml",
      "Add \"security-constraint\" elements to this descriptor.",
      CHECK);
  }

  @Test
  public void web_xml_with_constraints() {
    XmlCheckVerifier.verifyNoIssue("src/test/files/checks/xml/web/SecurityConstraintsInWebXmlCheck/withSecurityConstraints/web.xml", CHECK);
  }

  @Test
  public void not_a_web_xml() {
    XmlCheckVerifier.verifyNoIssue("src/test/files/checks/xml/irrelevant.xml", CHECK);
  }

}

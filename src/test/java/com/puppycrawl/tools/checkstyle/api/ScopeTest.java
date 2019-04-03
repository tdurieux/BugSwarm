////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2015 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

/**
 * Test cases for {@link Scope} enumeration.
 * @author Mehmet Can Cömert
 */
public class ScopeTest {

    /* Additional test for jacoco, since valueOf()
     * is generated by javac and jacoco reports that
     * valueOf() is uncovered.
     */
    @Test
    public void testScopeValueOf() {
        Scope scope = Scope.valueOf("PRIVATE");
        assertEquals(Scope.PRIVATE, scope);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMisc() {
        final Scope o = Scope.getInstance("public");
        assertNotNull(o);
        assertEquals("public", o.toString());
        assertEquals("public", o.getName());

        // will fail
        Scope.getInstance("unknown");
    }

    @Test
    public void testMixedCaseSpaces() {
        Scope.getInstance("NothinG ");
        Scope.getInstance(" PuBlic");
        Scope.getInstance(" ProteCted");
        Scope.getInstance("    PackAge ");
        Scope.getInstance("privaTe   ");
        Scope.getInstance("AnonInner");
    }

    @Test
    public void testMixedCaseSpacesWithDifferentLocales() {
        Locale[] differentLocales = {new Locale("TR", "tr") };
        Locale defaultLocale = Locale.getDefault();
        try {
            for (Locale differentLocale : differentLocales) {
                Locale.setDefault(differentLocale);
                testMixedCaseSpaces();
            }
        }
        finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void testIsInAnonInner() {
        assertTrue(Scope.NOTHING.isIn(Scope.ANONINNER));
        assertTrue(Scope.PUBLIC.isIn(Scope.ANONINNER));
        assertTrue(Scope.PROTECTED.isIn(Scope.ANONINNER));
        assertTrue(Scope.PACKAGE.isIn(Scope.ANONINNER));
        assertTrue(Scope.PRIVATE.isIn(Scope.ANONINNER));
        assertTrue(Scope.ANONINNER.isIn(Scope.ANONINNER));
    }

    @Test
    public void testIsInPrivate() {
        assertTrue(Scope.NOTHING.isIn(Scope.PRIVATE));
        assertTrue(Scope.PUBLIC.isIn(Scope.PRIVATE));
        assertTrue(Scope.PROTECTED.isIn(Scope.PRIVATE));
        assertTrue(Scope.PACKAGE.isIn(Scope.PRIVATE));
        assertTrue(Scope.PRIVATE.isIn(Scope.PRIVATE));
        assertFalse(Scope.ANONINNER.isIn(Scope.PRIVATE));
    }

    @Test
    public void testIsInPackage() {
        assertTrue(Scope.NOTHING.isIn(Scope.PACKAGE));
        assertTrue(Scope.PUBLIC.isIn(Scope.PACKAGE));
        assertTrue(Scope.PROTECTED.isIn(Scope.PACKAGE));
        assertTrue(Scope.PACKAGE.isIn(Scope.PACKAGE));
        assertFalse(Scope.PRIVATE.isIn(Scope.PACKAGE));
        assertFalse(Scope.ANONINNER.isIn(Scope.PACKAGE));
    }

    @Test
    public void testIsInProtected() {
        assertTrue(Scope.NOTHING.isIn(Scope.PROTECTED));
        assertTrue(Scope.PUBLIC.isIn(Scope.PROTECTED));
        assertTrue(Scope.PROTECTED.isIn(Scope.PROTECTED));
        assertFalse(Scope.PACKAGE.isIn(Scope.PROTECTED));
        assertFalse(Scope.PRIVATE.isIn(Scope.PROTECTED));
        assertFalse(Scope.ANONINNER.isIn(Scope.PROTECTED));
    }

    @Test
    public void testIsInPublic() {
        assertTrue(Scope.NOTHING.isIn(Scope.PUBLIC));
        assertTrue(Scope.PUBLIC.isIn(Scope.PUBLIC));
        assertFalse(Scope.PROTECTED.isIn(Scope.PUBLIC));
        assertFalse(Scope.PACKAGE.isIn(Scope.PUBLIC));
        assertFalse(Scope.PRIVATE.isIn(Scope.PUBLIC));
        assertFalse(Scope.ANONINNER.isIn(Scope.PUBLIC));
    }

    @Test
    public void testIsInNothing() {
        assertTrue(Scope.NOTHING.isIn(Scope.NOTHING));
        assertFalse(Scope.PUBLIC.isIn(Scope.NOTHING));
        assertFalse(Scope.PROTECTED.isIn(Scope.NOTHING));
        assertFalse(Scope.PACKAGE.isIn(Scope.NOTHING));
        assertFalse(Scope.PRIVATE.isIn(Scope.NOTHING));
        assertFalse(Scope.ANONINNER.isIn(Scope.NOTHING));
    }
}

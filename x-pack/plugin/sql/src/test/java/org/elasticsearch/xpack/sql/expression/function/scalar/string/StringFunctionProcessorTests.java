/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.sql.expression.function.scalar.string;

import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.test.AbstractWireSerializingTestCase;
import org.elasticsearch.xpack.sql.SqlIllegalArgumentException;
import org.elasticsearch.xpack.sql.expression.function.scalar.string.StringProcessor.StringOperation;

import java.io.IOException;
import java.util.Locale;

public class StringFunctionProcessorTests extends AbstractWireSerializingTestCase<StringProcessor> {
    public static StringProcessor randomStringFunctionProcessor() {
        return new StringProcessor(randomFrom(StringOperation.values()));
    }

    @Override
    protected StringProcessor createTestInstance() {
        return randomStringFunctionProcessor();
    }

    @Override
    protected Reader<StringProcessor> instanceReader() {
        return StringProcessor::new;
    }

    @Override
    protected StringProcessor mutateInstance(StringProcessor instance) throws IOException {
        return new StringProcessor(randomValueOtherThan(instance.processor(), () -> randomFrom(StringOperation.values())));
    }

    private void stringCharInputValidation(StringProcessor proc) {
        SqlIllegalArgumentException siae = expectThrows(SqlIllegalArgumentException.class, () -> proc.process(123));
        assertEquals("A string/char is required; received [123]", siae.getMessage());
    }

    private void numericInputValidation(StringProcessor proc) {
        SqlIllegalArgumentException siae = expectThrows(SqlIllegalArgumentException.class, () -> proc.process("A"));
        assertEquals("A number is required; received [A]", siae.getMessage());
    }

    public void testAscii() {
        StringProcessor proc = new StringProcessor(StringOperation.ASCII);
        assertNull(proc.process(null));
        assertEquals(65, proc.process("A"));
        // accepts chars as well
        assertEquals(65, proc.process('A'));
        assertEquals(65, proc.process("Alpha"));
        // validate input
        stringCharInputValidation(proc);
    }

    public void testChar() {
        StringProcessor proc = new StringProcessor(StringOperation.CHAR);
        assertNull(proc.process(null));
        assertEquals("A", proc.process(65));
        assertNull(proc.process(256));
        assertNull(proc.process(-1));
        // validate input
        numericInputValidation(proc);
    }

    public void testLCase() {
        StringProcessor proc = new StringProcessor(StringOperation.LCASE);
        assertNull(proc.process(null));
        assertEquals("fulluppercase", proc.process("FULLUPPERCASE"));
        assertEquals("someuppercase", proc.process("SomeUpPerCasE"));
        assertEquals("fulllowercase", proc.process("fulllowercase"));
        assertEquals("a", proc.process('A'));

        stringCharInputValidation(proc);
    }
    
    public void testLCaseWithTRLocale() {
        Locale.setDefault(Locale.forLanguageTag("tr"));
        StringProcessor proc = new StringProcessor(StringOperation.LCASE);

        // ES-SQL is not locale sensitive (so far). The obvious test for this is the Turkish language, uppercase letter I conversion
        // in non-Turkish locale the lowercasing would create i and an additional dot, while in Turkish Locale it would only create "i"
        // unicode 0069 = i
        assertEquals("\u0069\u0307", proc.process("\u0130"));
        // unicode 0049 = I (regular capital letter i)
        // in Turkish locale this would be lowercased to a "i" without dot (unicode 0131)
        assertEquals("\u0069", proc.process("\u0049"));
    }

    public void testUCase() {
        StringProcessor proc = new StringProcessor(StringOperation.UCASE);
        assertNull(proc.process(null));
        assertEquals("FULLLOWERCASE", proc.process("fulllowercase"));
        assertEquals("SOMELOWERCASE", proc.process("SomeLoweRCasE"));
        assertEquals("FULLUPPERCASE", proc.process("FULLUPPERCASE"));
        assertEquals("A", proc.process('a'));
        
        // special uppercasing for small letter sharp "s" resulting "SS"
        assertEquals("\u0053\u0053", proc.process("\u00df"));

        stringCharInputValidation(proc);
    }
    
    public void testUCaseWithTRLocale() {
        Locale.setDefault(Locale.forLanguageTag("tr"));
        StringProcessor proc = new StringProcessor(StringOperation.UCASE);

        // ES-SQL is not Locale sensitive (so far).
        // in Turkish locale, small letter "i" is uppercased to "I" with a dot above (unicode 130), otherwise in "i" (unicode 49)
        assertEquals("\u0049", proc.process("\u0069"));
    }

    public void testLength() {
        StringProcessor proc = new StringProcessor(StringOperation.LENGTH);
        assertNull(proc.process(null));
        assertEquals(7, proc.process("foo bar"));
        assertEquals(0, proc.process(""));
        assertEquals(0, proc.process("    "));
        assertEquals(7, proc.process("foo bar   "));
        assertEquals(10, proc.process("   foo bar   "));
        assertEquals(1, proc.process('f'));

        stringCharInputValidation(proc);
    }

    public void testRTrim() {
        StringProcessor proc = new StringProcessor(StringOperation.RTRIM);
        assertNull(proc.process(null));
        assertEquals("foo bar", proc.process("foo bar"));
        assertEquals("", proc.process(""));
        assertEquals("", proc.process("    "));
        assertEquals("foo bar", proc.process("foo bar   "));
        assertEquals("   foo bar", proc.process("   foo bar   "));
        assertEquals("f", proc.process('f'));

        stringCharInputValidation(proc);
    }

    public void testLTrim() {
        StringProcessor proc = new StringProcessor(StringOperation.LTRIM);
        assertNull(proc.process(null));
        assertEquals("foo bar", proc.process("foo bar"));
        assertEquals("", proc.process(""));
        assertEquals("", proc.process("    "));
        assertEquals("foo bar", proc.process("   foo bar"));
        assertEquals("foo bar   ", proc.process("   foo bar   "));
        assertEquals("f", proc.process('f'));

        stringCharInputValidation(proc);
    }

    public void testSpace() {
        StringProcessor proc = new StringProcessor(StringOperation.SPACE);
        int count = 7;
        assertNull(proc.process(null));
        assertEquals("       ", proc.process(count));
        assertEquals(count, ((String) proc.process(count)).length());
        assertNotNull(proc.process(0));
        assertEquals("", proc.process(0));
        assertNull(proc.process(-1));

        numericInputValidation(proc);
    }

    public void testBitLength() {
        StringProcessor proc = new StringProcessor(StringOperation.BIT_LENGTH);
        assertNull(proc.process(null));
        assertEquals(56, proc.process("foo bar"));
        assertEquals(0, proc.process(""));
        assertEquals(8, proc.process('f'));

        stringCharInputValidation(proc);
    }

    public void testCharLength() {
        StringProcessor proc = new StringProcessor(StringOperation.CHAR_LENGTH);
        assertNull(proc.process(null));
        assertEquals(7, proc.process("foo bar"));
        assertEquals(0, proc.process(""));
        assertEquals(1, proc.process('f'));
        assertEquals(1, proc.process('€'));

        stringCharInputValidation(proc);
    }
}

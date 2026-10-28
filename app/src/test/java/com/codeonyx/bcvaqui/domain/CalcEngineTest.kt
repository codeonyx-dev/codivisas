package com.codeonyx.bcvaqui.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalcEngineTest {
    private fun eval(e: String) = CalcEngine.evaluate(e)!!

    @Test fun precedence() {
        assertEquals(14.0, eval("2+3×4"), 1e-9)
        assertEquals(5.0, eval("20÷4"), 1e-9)
        assertEquals(-1.0, eval("2−3"), 1e-9)
    }

    @Test fun commaDecimals() {
        assertEquals(3.75, eval("1,5+2,25"), 1e-9)
        assertEquals(1234.5, eval("1.234,5"), 1e-9)
    }

    @Test fun percentages() {
        assertEquals(220.0, eval("200+10%"), 1e-9)
        assertEquals(180.0, eval("200−10%"), 1e-9)
        assertEquals(20.0, eval("200×10%"), 1e-9)
        assertEquals(0.15, eval("15%"), 1e-9)
    }

    @Test fun trailingOperatorIgnored() {
        assertEquals(5.0, eval("5+"), 1e-9)
        assertEquals(5.0, eval("5,"), 1e-9)
    }

    @Test fun invalid() {
        assertNull(CalcEngine.evaluate("5÷0"))
        assertNull(CalcEngine.evaluate(""))
        assertNull(CalcEngine.evaluate("×"))
    }

    @Test fun negativeStart() {
        assertEquals(-3.0, eval("−5+2"), 1e-9)
    }

    @Test fun format() {
        assertEquals("887,3", CalcEngine.format(887.3))
        assertEquals("1.234.567,8912", CalcEngine.format(1234567.89123))
        assertEquals("0", CalcEngine.format(0.0))
    }
}

class CalcInputTest {
    @Test fun typing() {
        var s = CalcInput()
        s = s.digit("1").digit("2").comma().digit("5").operator(CalcEngine.PLUS).digit("3")
        assertEquals("12,5+3", s.expression)
        assertEquals(15.5, s.result!!, 1e-9)
    }

    @Test fun operatorReplaces() {
        val s = CalcInput("5+").operator(CalcEngine.TIMES)
        assertEquals("5×", s.expression)
    }

    @Test fun singleCommaPerNumber() {
        assertEquals("1,5", CalcInput("1,5").comma().expression)
        assertEquals("0,", CalcInput().comma().expression)
    }

    @Test fun leadingZero() {
        assertEquals("7", CalcInput("0").digit("7").expression)
        assertEquals("0", CalcInput("0").digit("0").expression)
    }

    @Test fun withValue() {
        assertEquals("1234,5", CalcInput().withValue(1234.5).expression)
    }
}

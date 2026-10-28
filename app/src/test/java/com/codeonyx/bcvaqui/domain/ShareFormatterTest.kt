package com.codeonyx.bcvaqui.domain

import com.codeonyx.bcvaqui.data.model.PaymentContact
import com.codeonyx.bcvaqui.data.model.PaymentMigration
import com.codeonyx.bcvaqui.data.model.PaymentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareFormatterTest {
    private val pm = PaymentContact(
        id = "1", type = PaymentType.PAGO_MOVIL, displayName = "Dalvin",
        phone = "04140000000", documentId = "V12345678", bankCode = "0102", bankName = "Banco de Venezuela"
    )
    private val conv = ShareConversion("Usdt", "USDT", 43.0, 41470.06)

    @Test fun exampleFormat() {
        val expected = """
            -Tasa: Usdt
            -Monto en Dolares: 43,00
            -Monto Bs: 41.470,06

            -Pago Movil:
            -Tlf: 04140000000
            -CI: V12345678
            -Banco: 0102 - Banco de Venezuela
        """.trimIndent()
        assertEquals(expected, ShareFormatter.build(conv, listOf(pm)))
    }

    @Test fun onlyConversion() {
        assertEquals("-Tasa: Usdt\n-Monto en Dolares: 43,00\n-Monto Bs: 41.470,06", ShareFormatter.build(conv, emptyList()))
    }

    @Test fun rateLabels() {
        assertEquals("BCV", ShareFormatter.rateLabelFor("USD"))
        assertEquals("Euro", ShareFormatter.rateLabelFor("EUR"))
        assertEquals("Usdt", ShareFormatter.rateLabelFor("USDT"))
        assertEquals("Kontigo", ShareFormatter.rateLabelFor("USD", "Kontigo"))
    }

    @Test fun zelleShowsUsdAmount() {
        val zelle = PaymentContact(id = "z", type = PaymentType.ZELLE, displayName = "Ana Pérez", email = "ana@mail.com")
        val text = ShareFormatter.build(ShareConversion("BCV", "USD", 43.0, 9000.0), listOf(zelle))
        assertTrue(text.endsWith("-Zelle:\n-Nombre: Ana Pérez\n-Correo: ana@mail.com\n-Monto a enviar: 43,00 USD"))
    }

    @Test fun euroUsesUsdAmountForUsdMethods() {
        val pp = PaymentContact(id = "p", type = PaymentType.PAYPAL, email = "a@b.com", userHandle = "ana")
        val text = ShareFormatter.build(ShareConversion("Euro", "EUR", 10.0, 1000.0, usdAmount = 11.5), listOf(pp))
        assertTrue(text.contains("-Monto en Euros: 10,00"))
        assertTrue(text.contains("-Link: paypal.me/ana"))
        assertTrue(text.contains("-Monto a enviar: 11,50 USD"))
    }

    @Test fun binanceUsesUsdt() {
        val b = PaymentContact(id = "b", type = PaymentType.BINANCE, accountNumber = "123456")
        assertTrue(ShareFormatter.build(conv, listOf(b)).contains("-Pay ID: 123456\n-Monto a enviar: 43,00 USDT"))
    }

    @Test fun colombiaCop() {
        val co = PaymentContact(
            id = "c", type = PaymentType.CUENTA_CO, displayName = "Luis", bankName = "Bancolombia",
            accountType = "Ahorros", accountNumber = "123", documentId = "CC 99"
        )
        val text = ShareFormatter.build(ShareConversion("BCV", "USD", 10.0, 1000.0), listOf(co), copPerUsd = 4000.0)
        assertTrue(text.contains("-Cuenta Colombia:\n-Banco: Bancolombia\n-Tipo: Ahorros\n-Número: 123\n-Titular: Luis\n-CC/NIT: CC 99\n-Monto COP: 40.000"))
        assertFalse(ShareFormatter.build(ShareConversion("BCV", "USD", 10.0, 1000.0), listOf(co)).contains("COP"))
    }

    @Test fun nequiWallet() {
        val n = PaymentContact(id = "n", type = PaymentType.CUENTA_CO, bankName = "Nequi", phone = "3001234567")
        assertTrue(ShareFormatter.methodBlock(n, null, null).startsWith("-Nequi:\n-Tlf: 3001234567"))
    }

    @Test fun multipleMethodsSeparated() {
        val z = PaymentContact(id = "z", type = PaymentType.ZELLE, email = "x@y.com")
        val text = ShareFormatter.build(conv, listOf(pm, z))
        assertTrue(text.contains("-Banco: 0102 - Banco de Venezuela\n\n-Zelle:"))
    }
}

class PaymentMigrationTest {
    @Test fun legacyJsonKeepsData() {
        val legacy = """[{"id":"a","type":"PAGO_MOVIL","displayName":"Yo","phone":"0414","documentId":"V1","bankCode":"0102","bankName":"Banco de Venezuela"},
            {"id":"b","type":"CUENTA_EXTRANJERA","displayName":"Ext","accountNumber":"ES12","country":"España"}]"""
        val list = PaymentMigration.decode(legacy)
        assertEquals(2, list.size)
        assertEquals("0414", list[0].phone)
        assertEquals("Banco de Venezuela", list[0].bankName)
        assertTrue(list[0].isDefault)
        assertFalse(list[1].isDefault)
        assertEquals("", list[1].email)
    }

    @Test fun badEntrySkippedOthersKept() {
        val raw = """[{"id":"a","type":"PAGO_MOVIL","phone":"1"},{"type":123},{"id":"c","type":"ZELLE","email":"z@z"}]"""
        val list = PaymentMigration.decode(raw)
        assertEquals(listOf("a", "c"), list.map { it.id })
    }

    @Test fun singleDefault() {
        val l = listOf(
            PaymentContact(id = "1", isDefault = true), PaymentContact(id = "2", isDefault = true)
        )
        assertEquals(listOf(true, false), PaymentMigration.normalize(l).map { it.isDefault })
        assertEquals(listOf(false, true), PaymentMigration.setDefault(l, "2").map { it.isDefault })
    }

    @Test fun emptyOrGarbage() {
        assertEquals(0, PaymentMigration.decode(null).size)
        assertEquals(0, PaymentMigration.decode("{not json").size)
    }
}

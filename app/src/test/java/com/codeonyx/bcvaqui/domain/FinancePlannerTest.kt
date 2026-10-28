package com.codeonyx.bcvaqui.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FinancePlannerTest {
    private val d0 = LocalDate.of(2026, 1, 1)
    private fun mv(a: Double) = Movement("m$a", d0.toEpochDay(), a)

    @Test fun installments_andSchedule() {
        assertEquals(33.33, FinancePlanner.installmentAmount(100.0, 3), 0.0)
        assertEquals(listOf(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1)),
            FinancePlanner.schedule(d0, 2, Frequency.MENSUAL))
        assertEquals(LocalDate.of(2026, 1, 16), Frequency.QUINCENAL.advance(d0, 1))
    }

    @Test fun debt_nextInstallmentAfterPayment() {
        val debt = Debt("1", true, "Ana", 300.0, startEpochDay = d0.toEpochDay(), installments = 3,
            payments = listOf(mv(100.0)))
        val s = FinancePlanner.debtStatus(debt, d0)
        assertEquals(200.0, s.remaining, 0.0)
        assertEquals(LocalDate.of(2026, 3, 1), s.nextDue)
        assertEquals(100.0, s.nextAmount, 0.0)
        assertFalse(s.overdue)
    }

    @Test fun debt_overdue() {
        val debt = Debt("1", false, "Luis", 50.0, startEpochDay = d0.toEpochDay(), dueEpochDay = d0.toEpochDay())
        val s = FinancePlanner.debtStatus(debt, d0.plusDays(3))
        assertTrue(s.overdue)
        assertEquals(-3L, s.daysToNext)
        assertEquals(1, FinancePlanner.debtsToRemind(listOf(debt), d0.plusDays(3)).size)
    }

    @Test fun debt_settledWhenPaidOrMarked() {
        val a = Debt("1", true, "X", 10.0, startEpochDay = 0, payments = listOf(mv(10.0)))
        assertTrue(FinancePlanner.debtStatus(a, d0).settled)
        val b = Debt("2", true, "X", 10.0, startEpochDay = 0, dueEpochDay = 0, markedPaid = true)
        assertFalse(FinancePlanner.debtStatus(b, d0).overdue)
    }

    @Test fun goal_requiredPerPeriodAndBehind() {
        val goal = SavingsGoal("g", "Moto", target = 1200.0, createdEpochDay = d0.toEpochDay(),
            targetEpochDay = d0.plusMonths(12).toEpochDay(), frequency = Frequency.MENSUAL)
        val today = d0.plusMonths(6)
        val s = FinancePlanner.goalStatus(goal.copy(movements = listOf(mv(300.0))), today)
        assertEquals(6, s.periodsLeft)
        assertEquals(150.0, s.requiredPerPeriod, 0.0)
        assertTrue(s.behindBy > 290 && s.behindBy < 310)
    }

    @Test fun goal_projectionAndComplete() {
        val goal = SavingsGoal("g", "x", target = 100.0, createdEpochDay = d0.toEpochDay(),
            targetEpochDay = d0.plusDays(100).toEpochDay(), movements = listOf(mv(50.0)))
        assertEquals(d0.plusDays(20), FinancePlanner.projectedCompletion(goal, 50.0, d0.plusDays(10)))
        assertNull(FinancePlanner.projectedCompletion(goal, 0.0, d0.plusDays(10)))
        assertTrue(FinancePlanner.goalStatus(goal.copy(movements = listOf(mv(100.0))), d0).complete)
    }

    @Test fun periods_andConversions() {
        assertEquals(4, Frequency.SEMANAL.periodsBetween(d0, d0.plusDays(28)))
        assertEquals(1, Frequency.MENSUAL.periodsBetween(d0, d0))
        assertEquals(10.0, FinancePlanner.toUsd(1000.0, Money.VES, 100.0), 0.0)
        assertEquals(50.0, FinancePlanner.erosionPercent(100.0, 200.0)!!, 0.0)
    }
}

package com.codeonyx.bcvaqui.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max

enum class Frequency(val label: String, val plural: String) {
    SEMANAL("Semanal", "semana"),
    QUINCENAL("Quincenal", "quincena"),
    MENSUAL("Mensual", "mes");

    fun advance(date: LocalDate, steps: Long): LocalDate = when (this) {
        SEMANAL -> date.plusWeeks(steps)
        QUINCENAL -> date.plusDays(15 * steps)
        MENSUAL -> date.plusMonths(steps)
    }

    /** Períodos completos (al menos 1) entre [from] y [to]. */
    fun periodsBetween(from: LocalDate, to: LocalDate): Int {
        if (!to.isAfter(from)) return 1
        val n = when (this) {
            SEMANAL -> ChronoUnit.DAYS.between(from, to) / 7.0
            QUINCENAL -> ChronoUnit.DAYS.between(from, to) / 15.0
            MENSUAL -> {
                val m = ChronoUnit.MONTHS.between(from, to)
                (if (from.plusMonths(m).isBefore(to)) m + 1 else m).toDouble()
            }
        }
        return max(1, ceil(n - 1e-9).toInt())
    }
}

enum class Money { USD, VES }

@Serializable
data class Movement(
    val id: String,
    val epochDay: Long,
    val amount: Double,
    val note: String = ""
)

@Serializable
data class Debt(
    val id: String,
    /** true = "Debo"; false = "Me deben". */
    val iOwe: Boolean,
    val person: String,
    val amount: Double,
    val currency: Money = Money.USD,
    val startEpochDay: Long,
    val dueEpochDay: Long? = null,
    val installments: Int = 0,
    val frequency: Frequency = Frequency.MENSUAL,
    val notes: String = "",
    val payments: List<Movement> = emptyList(),
    val archived: Boolean = false,
    val markedPaid: Boolean = false,
    val remindDaysBefore: Int = 1
)

@Serializable
data class SavingsGoal(
    val id: String,
    val name: String,
    val emoji: String = "🎯",
    val target: Double,
    val currency: Money = Money.USD,
    val createdEpochDay: Long,
    val targetEpochDay: Long,
    val frequency: Frequency = Frequency.MENSUAL,
    val movements: List<Movement> = emptyList(),
    /** Tasa BCV al crear la meta (para mostrar la erosión de metas en Bs). */
    val rateAtCreation: Double? = null
)

data class DebtStatus(
    val paid: Double,
    val remaining: Double,
    val progress: Float,
    val installmentAmount: Double?,
    val nextDue: LocalDate?,
    val nextAmount: Double,
    val overdue: Boolean,
    val daysToNext: Long?,
    val settled: Boolean
)

data class GoalStatus(
    val saved: Double,
    val remaining: Double,
    val progress: Float,
    val requiredPerPeriod: Double,
    val periodsLeft: Int,
    val expectedByNow: Double,
    /** Positivo = atrasado esa cantidad; ≤0 = al día. */
    val behindBy: Double,
    val projectedDate: LocalDate?,
    val complete: Boolean
)

object FinancePlanner {
    fun installmentAmount(total: Double, count: Int): Double =
        if (count <= 0) total else FxCalculator.round2(total / count)

    fun schedule(start: LocalDate, count: Int, frequency: Frequency): List<LocalDate> =
        (1..count).map { frequency.advance(start, it.toLong()) }

    fun debtStatus(debt: Debt, today: LocalDate): DebtStatus {
        val paid = FxCalculator.round2(debt.payments.sumOf { it.amount })
        val remaining = FxCalculator.round2((debt.amount - paid).coerceAtLeast(0.0))
        val settled = debt.markedPaid || remaining <= 0.005
        val progress = if (settled) 1f else (paid / debt.amount).toFloat().coerceIn(0f, 1f)
        val start = LocalDate.ofEpochDay(debt.startEpochDay)
        var nextDue: LocalDate? = null
        var nextAmount = remaining
        var cuota: Double? = null
        if (debt.installments > 0) {
            cuota = installmentAmount(debt.amount, debt.installments)
            val dates = schedule(start, debt.installments, debt.frequency)
            // Cuotas cubiertas por lo abonado (con tolerancia de redondeo).
            val covered = ((paid + 0.01) / cuota).toInt().coerceAtMost(debt.installments)
            if (!settled && covered < debt.installments) {
                nextDue = dates[covered]
                val owedUpToNext = cuota * (covered + 1)
                nextAmount = FxCalculator.round2(minOf(remaining, owedUpToNext - paid))
            }
        } else if (!settled) {
            nextDue = debt.dueEpochDay?.let { LocalDate.ofEpochDay(it) }
        }
        val overdue = !settled && nextDue != null && nextDue.isBefore(today)
        return DebtStatus(
            paid, remaining, progress, cuota, nextDue, if (settled) 0.0 else nextAmount, overdue,
            nextDue?.let { ChronoUnit.DAYS.between(today, it) }, settled
        )
    }

    fun toUsd(amount: Double, currency: Money, rate: Double): Double =
        if (currency == Money.USD) amount else if (rate > 0) amount / rate else 0.0

    fun toVes(amount: Double, currency: Money, rate: Double): Double =
        if (currency == Money.VES) amount else amount * rate

    fun goalStatus(goal: SavingsGoal, today: LocalDate): GoalStatus {
        val saved = FxCalculator.round2(goal.movements.sumOf { it.amount }.coerceAtLeast(0.0))
        val remaining = FxCalculator.round2((goal.target - saved).coerceAtLeast(0.0))
        val complete = remaining <= 0.005
        val created = LocalDate.ofEpochDay(goal.createdEpochDay)
        val target = LocalDate.ofEpochDay(goal.targetEpochDay)
        val periodsLeft = goal.frequency.periodsBetween(today, target)
        val required = if (complete) 0.0 else FxCalculator.round2(remaining / periodsLeft)
        val totalDays = ChronoUnit.DAYS.between(created, target).coerceAtLeast(1)
        val elapsed = ChronoUnit.DAYS.between(created, today).coerceIn(0, totalDays)
        val expected = FxCalculator.round2(goal.target * elapsed / totalDays)
        val behind = FxCalculator.round2(expected - saved)
        return GoalStatus(
            saved, remaining, (saved / goal.target).toFloat().coerceIn(0f, 1f), required, periodsLeft,
            expected, if (complete) 0.0 else behind, projectedCompletion(goal, saved, today), complete
        )
    }

    /** Fecha estimada según el ritmo real (ahorro neto / días desde la creación). */
    fun projectedCompletion(goal: SavingsGoal, saved: Double, today: LocalDate): LocalDate? {
        if (saved >= goal.target) return today
        val days = ChronoUnit.DAYS.between(LocalDate.ofEpochDay(goal.createdEpochDay), today).coerceAtLeast(1)
        if (saved <= 0) return null
        val perDay = saved / days
        val daysNeeded = ceil((goal.target - saved) / perDay).toLong()
        return if (daysNeeded > 365L * 50) null else today.plusDays(daysNeeded)
    }

    /** Pérdida % del valor en $ de un monto en Bs entre dos tasas. */
    fun erosionPercent(rateThen: Double?, rateNow: Double): Double? {
        if (rateThen == null || rateThen <= 0 || rateNow <= 0) return null
        return FxCalculator.round2((1 - rateThen / rateNow) * 100.0)
    }

    /** Deudas que deben avisarse hoy: próximo pago dentro de [Debt.remindDaysBefore] días o vencido. */
    fun debtsToRemind(debts: List<Debt>, today: LocalDate): List<Pair<Debt, DebtStatus>> =
        debts.filter { !it.archived && it.remindDaysBefore >= 0 }
            .map { it to debtStatus(it, today) }
            .filter { (d, s) -> !s.settled && s.daysToNext != null && s.daysToNext <= d.remindDaysBefore }
}

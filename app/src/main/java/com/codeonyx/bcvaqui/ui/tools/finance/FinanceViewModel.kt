package com.codeonyx.bcvaqui.ui.tools.finance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeonyx.bcvaqui.data.local.FinanceRepository
import com.codeonyx.bcvaqui.domain.Debt
import com.codeonyx.bcvaqui.domain.Movement
import com.codeonyx.bcvaqui.domain.SavingsGoal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class FinanceViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = FinanceRepository(app)

    val debts: StateFlow<List<Debt>> = repo.debts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val goals: StateFlow<List<SavingsGoal>> = repo.goals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun newId(): String = UUID.randomUUID().toString()

    fun saveDebt(debt: Debt) = viewModelScope.launch {
        repo.updateDebts { list -> if (list.any { it.id == debt.id }) list.map { if (it.id == debt.id) debt else it } else list + debt }
    }

    fun deleteDebt(id: String) = viewModelScope.launch { repo.updateDebts { l -> l.filterNot { it.id == id } } }

    fun addPayment(id: String, amount: Double, note: String) = viewModelScope.launch {
        repo.updateDebts { l -> l.map { if (it.id == id) it.copy(payments = it.payments + Movement(newId(), LocalDate.now().toEpochDay(), amount, note)) else it } }
    }

    fun setDebtFlags(id: String, paid: Boolean? = null, archived: Boolean? = null) = viewModelScope.launch {
        repo.updateDebts { l -> l.map { if (it.id == id) it.copy(markedPaid = paid ?: it.markedPaid, archived = archived ?: it.archived) else it } }
    }

    fun saveGoal(goal: SavingsGoal) = viewModelScope.launch {
        repo.updateGoals { list -> if (list.any { it.id == goal.id }) list.map { if (it.id == goal.id) goal else it } else list + goal }
    }

    fun deleteGoal(id: String) = viewModelScope.launch { repo.updateGoals { l -> l.filterNot { it.id == id } } }

    /** [amount] positivo = depósito, negativo = retiro. */
    fun addMovement(id: String, amount: Double, note: String) = viewModelScope.launch {
        repo.updateGoals { l -> l.map { if (it.id == id) it.copy(movements = it.movements + Movement(newId(), LocalDate.now().toEpochDay(), amount, note)) else it } }
    }
}

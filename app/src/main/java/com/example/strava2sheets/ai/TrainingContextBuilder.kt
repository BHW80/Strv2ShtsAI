package com.example.strava2sheets.ai

import com.example.strava2sheets.data.local.ActivityEntity
import com.example.strava2sheets.data.local.AthleteProfileEntity
import com.example.strava2sheets.ui.fitness.FitnessEngine
import com.example.strava2sheets.ui.goals.GoalCalculator
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Zukunftssichere Trainer-Felder (Optionale Biometrie)
 */
data class CoachBiometrics(
    val rhrBpm: Int? = null,
    val rhr7dAvg: Double? = null,
    val hrvRmssd: Double? = null,
    val sleepHours: Double? = null,
    val muscleSorenessScore: Int? = null // Skala 1 (frisch) bis 5 (starker Muskelkater)
)

object TrainingContextBuilder {

    /**
     * Erstellt einen kompakten, hochgradig informativen System-Prompt (~2.000 Tokens)
     * für AeroCoach mit Fokus auf Trainingssteuerung, Belastung und Empfehlungen.
     */
    fun buildSystemPrompt(
        athlete: AthleteProfileEntity?,
        activities: List<ActivityEntity>,
        biometrics: CoachBiometrics = CoachBiometrics(),
        referenceDate: LocalDate = LocalDate.now()
    ): String {
        val sb = StringBuilder()

        // 1. Trainer Persona & Anweisungen
        sb.appendLine("Du bist 'AeroCoach', ein erfahrener, datenbasierter Ausdauersport- und Radsport-Trainer.")
        sb.appendLine("Deine Aufgabe ist es, den Athleten basierend auf seinen Leistungsdaten, seiner Belastung (PMC) und seinen Aktivitäten kompetent, präzise und motivierend zu coachen.")
        sb.appendLine("Antworte strukturiert in prägnantem Markdown (Bulletpoints, Felsche/Tabellen wenn sinnvoll), auf Deutsch, direkt auf den Punkt.")
        sb.appendLine("Berücksichtige stets die Trainingslehre (z.B. ACWR zur Verletzungsprävention, 80/20 polarisiertes Training, TSB Formsteuerung).")
        sb.appendLine()
        sb.appendLine("--- HEUTIGES DATUM: $referenceDate ---")
        sb.appendLine()

        // 2. Athletenprofil
        sb.appendLine("=== 1. ATHLETENPROFIL ===")
        if (athlete != null) {
            val name = listOfNotNull(athlete.firstname, athlete.lastname).joinToString(" ").ifBlank { "Athlet" }
            sb.appendLine("- Name: $name (Geschlecht: ${athlete.sex ?: "k.A."})")
            sb.appendLine("- Gewicht: ${athlete.weightKg?.let { String.format(Locale.US, "%.1f kg", it) } ?: "k.A."}")
            sb.appendLine("- FTP: ${athlete.ftpWatts?.let { "${it}W" } ?: "k.A."} (${athlete.calculatedWPerKg?.let { String.format(Locale.US, "%.2f W/kg", it) } ?: "k.A."})")
            sb.appendLine("- Herzfrequenz-Zonen: ${athlete.heartRateZonesJson ?: "k.A."}")
            sb.appendLine("- Watt-Zonen: ${athlete.powerZonesJson ?: "k.A."}")
        } else {
            sb.appendLine("Kein Profil hinterlegt. Verwende Standard-Ausdauerannahmen.")
        }
        sb.appendLine()

        // 3. Optionale Biometrie & Regeneration (Zukunftssicher)
        sb.appendLine("=== 2. BIOMETRIE & REGENERATION ===")
        sb.appendLine("- Ruhepuls (RHR): ${biometrics.rhrBpm?.let { "$it bpm" } ?: "Keine Daten"}")
        sb.appendLine("- RHR 7-Tage-Schnitt: ${biometrics.rhr7dAvg?.let { String.format(Locale.US, "%.1f bpm", it) } ?: "Keine Daten"}")
        sb.appendLine("- HRV (rMSSD): ${biometrics.hrvRmssd?.let { String.format(Locale.US, "%.1f ms", it) } ?: "Keine Daten"}")
        sb.appendLine("- Schlaf: ${biometrics.sleepHours?.let { String.format(Locale.US, "%.1f h", it) } ?: "Keine Daten"}")
        sb.appendLine("- Muskelkater/RPE (1-5): ${biometrics.muscleSorenessScore ?: "Keine Angabe"}")
        sb.appendLine()

        // 4. Form & Belastung (PMC aus FitnessEngine + ACWR)
        sb.appendLine("=== 3. FORM & BELASTUNG (PMC) ===")
        val fitnessHistory = FitnessEngine.calculateFitnessHistory(
            activities = activities,
            ftp = athlete?.ftpWatts,
            daysToInclude = 60,
            referenceDate = referenceDate
        )
        val todayFitness = fitnessHistory.lastOrNull()
        if (todayFitness != null) {
            val ctl = todayFitness.fitness
            val atl = todayFitness.fatigue
            val tsb = todayFitness.form
            // ACWR = Acute (ATL) / Chronic (CTL). 0.8 - 1.3 = Sweetspot, > 1.5 = Hohes Verletzungsrisiko
            val acwr = if (ctl > 0.0) atl / ctl else 1.0
            val acwrStatus = when {
                acwr < 0.8 -> "Unterbelastung / Detraining"
                acwr <= 1.3 -> "Optimaler Belastungsaufbau (Sweetspot)"
                acwr <= 1.5 -> "Erhöhte Ermüdung (Vorsicht)"
                else -> "Gefahrenbereich! Überlastungs-/Verletzungsrisiko stark erhöht"
            }

            sb.appendLine("- CTL (Fitness / chronische Belastung, 42d): ${String.format(Locale.US, "%.1f", ctl)}")
            sb.appendLine("- ATL (Fatigue / akute Belastung, 7d): ${String.format(Locale.US, "%.1f", atl)}")
            sb.appendLine("- TSB (Form = CTL - ATL): ${String.format(Locale.US, "%.1f", tsb)} (${getTsbStatus(tsb)})")
            sb.appendLine("- ACWR (Acute:Chronic Ratio): ${String.format(Locale.US, "%.2f", acwr)} -> $acwrStatus")
        } else {
            sb.appendLine("Nicht genügend Aktivitätsdaten für PMC vorhanden.")
        }
        sb.appendLine()

        // 5. 365-Tage-Zusammenfassung
        sb.appendLine("=== 4. 365-TAGE ZUSAMMENFASSUNG ===")
        val oneYearAgo = referenceDate.minusYears(1)
        val yearActivities = activities.filter { act ->
            GoalCalculator.parseDateFlexible(act.startDatum)?.let { it.isAfter(oneYearAgo) || it.isEqual(oneYearAgo) } ?: false
        }

        val totalDistYear = yearActivities.sumOf { it.distanzKm ?: 0.0 }
        val totalElevYear = yearActivities.sumOf { it.hoehenmeterM ?: 0 }
        val totalSecYear = yearActivities.sumOf { it.bewegungszeitSek }

        val activeDatesYear = yearActivities.mapNotNull { GoalCalculator.parseDateFlexible(it.startDatum) }.toSet()
        val totalActiveDays = activeDatesYear.size
        val totalRestDays = 365 - totalActiveDays
        val restDayRatio = if (totalActiveDays > 0) (totalRestDays.toDouble() / 365.0) * 100 else 100.0

        sb.appendLine("- Jahreskilometer: ${String.format(Locale.US, "%.1f km", totalDistYear)}")
        sb.appendLine("- Höhenmeter: $totalElevYear hm")
        sb.appendLine("- Gesamtzeit: ${formatHours(totalSecYear)}")
        sb.appendLine("- Aktivtage vs. Ruhetage: $totalActiveDays Tage aktiv / $totalRestDays Ruhetage (Ruhetags-Quote: ${String.format(Locale.US, "%.1f%%", restDayRatio)})")

        // Sportarten-Split im Jahr
        val sportSplit = yearActivities.groupBy { it.sportart.lowercase().trim() }
        val sportSplitFormatted = sportSplit.entries.sortedByDescending { it.value.sumOf { a -> a.distanzKm ?: 0.0 } }
            .take(5)
            .joinToString(", ") { (sport, acts) ->
                val km = acts.sumOf { it.distanzKm ?: 0.0 }
                "${sport.replaceFirstChar { it.uppercase() }}: ${acts.size}x (${String.format(Locale.US, "%.0f km", km)})"
            }
        sb.appendLine("- Sportarten-Split: ${sportSplitFormatted.ifBlank { "Keine" }}")
        sb.appendLine()

        // 6. Trends & 80/20 Intensitätsverteilung
        sb.appendLine("=== 5. TRENDS & 80/20 VERTEILUNG ===")
        // 4-Wochen-KM-Entwicklung
        sb.appendLine("- KM-Entwicklung letzte 4 Wochen (älteste -> neueste):")
        for (w in 4 downTo 1) {
            val wStart = referenceDate.minusWeeks(w.toLong())
            val wEnd = referenceDate.minusWeeks((w - 1).toLong())
            val wActs = activities.filter { act ->
                GoalCalculator.parseDateFlexible(act.startDatum)?.let {
                    (it.isAfter(wStart) || it.isEqual(wStart)) && it.isBefore(wEnd)
                } ?: false
            }
            val wKm = wActs.sumOf { it.distanzKm ?: 0.0 }
            val wHours = wActs.sumOf { it.bewegungszeitSek } / 3600.0
            sb.appendLine("  * Vor $w Wo: ${String.format(Locale.US, "%.1f km", wKm)} (${String.format(Locale.US, "%.1f h", wHours)}, ${wActs.size} Einheiten)")
        }

        // Aktueller Monat vs. Vormonat
        val currentMonthStart = referenceDate.withDayOfMonth(1)
        val prevMonthStart = currentMonthStart.minusMonths(1)
        val prevMonthEnd = currentMonthStart.minusDays(1)

        val curMonthActs = activities.filter { act ->
            GoalCalculator.parseDateFlexible(act.startDatum)?.let {
                (it.isAfter(currentMonthStart) || it.isEqual(currentMonthStart)) && (it.isBefore(referenceDate) || it.isEqual(referenceDate))
            } ?: false
        }
        val prevMonthActs = activities.filter { act ->
            GoalCalculator.parseDateFlexible(act.startDatum)?.let {
                (it.isAfter(prevMonthStart) || it.isEqual(prevMonthStart)) && (it.isBefore(prevMonthEnd) || it.isEqual(prevMonthEnd))
            } ?: false
        }
        val curMonthKm = curMonthActs.sumOf { it.distanzKm ?: 0.0 }
        val prevMonthKm = prevMonthActs.sumOf { it.distanzKm ?: 0.0 }
        sb.appendLine("- Monatstrend: Aktueller Monat bisher ${String.format(Locale.US, "%.1f km", curMonthKm)} vs. Vormonat gesamt ${String.format(Locale.US, "%.1f km", prevMonthKm)}")

        // 80/20 Intensitätsanalyse (letzte 28 Tage)
        val last28Days = referenceDate.minusDays(28)
        val last4WeeksActs = activities.filter { act ->
            GoalCalculator.parseDateFlexible(act.startDatum)?.let { it.isAfter(last28Days) } ?: false
        }
        // Schätzung Basis / Intensiv anhand Suffer Score / HR / Power
        var lowIntensitySec = 0L
        var highIntensitySec = 0L
        for (act in last4WeeksActs) {
            val isHighIntensity = (act.sufferScore != null && act.sufferScore > 60) ||
                    (act.avgHF != null && athlete?.heartRateZonesJson != null && act.avgHF > 155) ||
                    (act.avgLeistungWatt != null && athlete?.ftpWatts != null && act.avgLeistungWatt > athlete.ftpWatts * 0.85)
            if (isHighIntensity) {
                highIntensitySec += act.bewegungszeitSek
            } else {
                lowIntensitySec += act.bewegungszeitSek
            }
        }
        val totalIntensitySec = lowIntensitySec + highIntensitySec
        if (totalIntensitySec > 0) {
            val lowRatio = (lowIntensitySec.toDouble() / totalIntensitySec) * 100
            val highRatio = (highIntensitySec.toDouble() / totalIntensitySec) * 100
            sb.appendLine("- 80/20 Verteilung (letzte 4 Wochen): ${String.format(Locale.US, "%.0f%% Low (Grundlage) / %.0f%% High (Intensiv)", lowRatio, highRatio)}")
        } else {
            sb.appendLine("- 80/20 Verteilung: Keine ausreichenden Einheiten in den letzten 4 Wochen.")
        }
        sb.appendLine()

        // 7. Detail-Historie: Letzte 12 Einheiten
        sb.appendLine("=== 6. HISTORIE (LETZTE EINHEITEN) ===")
        val sortedRecent = activities.sortedByDescending { it.startDatum + " " + it.startUhrzeit }.take(12)
        if (sortedRecent.isNotEmpty()) {
            sortedRecent.forEachIndexed { idx, act ->
                val dist = act.distanzKm?.let { String.format(Locale.US, "%.1fkm", it) } ?: "- km"
                val hr = if (act.avgHF != null) "HF ${act.avgHF}/${act.maxHF ?: "-"}" else ""
                val watt = if (act.avgLeistungWatt != null) "${act.avgLeistungWatt}W" else ""
                val suffer = if (act.sufferScore != null) "Suffer: ${act.sufferScore}" else ""
                val details = listOf(dist, act.bewegungszeitFormatted, hr, watt, suffer).filter { it.isNotBlank() }.joinToString(", ")
                sb.appendLine("${idx + 1}. [${act.startDatum}] ${act.sportart}: \"${act.titel}\" | $details")
            }
        } else {
            sb.appendLine("Keine protokollierten Einheiten vorhanden.")
        }
        sb.appendLine()

        sb.appendLine("=== ANWEISUNG AN DICH ===")
        sb.appendLine("Nutze diese Zahlen aktiv für fundierte Antworten. Wenn der Athlet nach Trainingsempfehlungen für heute/morgen fragt, beachte zwingend TSB, ACWR und die letzte Einheit!")

        return sb.toString()
    }

    private fun getTsbStatus(tsb: Double): String = when {
        tsb > 25.0 -> "Sehr hohe Frische / Möglicher Formverlust (Detraining)"
        tsb in 10.0..25.0 -> "Rennform / Tapering-Zone (Optimal für Wettkämpfe)"
        tsb in -10.0..10.0 -> "Neutrale Form / Ausgeglichenes Training"
        tsb in -30.0..-10.0 -> "Optimale Trainingsbelastung (Produktiv)"
        else -> "Starke Überlastung / Erholungsbedarf dringend empfohlen"
    }

    private fun formatHours(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        return "${hours}h ${mins}m"
    }
}

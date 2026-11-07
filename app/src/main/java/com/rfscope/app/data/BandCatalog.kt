package com.rfscope.app.data

import kotlin.math.abs

object OperatorCatalog {
    private val operators = listOf(
        OperatorInfo("CMCC", "460", "00", "China Mobile", "中国移动"),
        OperatorInfo("CMCC", "460", "02", "China Mobile", "中国移动"),
        OperatorInfo("CMCC", "460", "04", "China Mobile", "中国移动"),
        OperatorInfo("CMCC", "460", "07", "China Mobile", "中国移动"),
        OperatorInfo("CMCC", "460", "08", "China Mobile", "中国移动"),
        OperatorInfo("CU", "460", "01", "China Unicom", "中国联通"),
        OperatorInfo("CU", "460", "06", "China Unicom", "中国联通"),
        OperatorInfo("CU", "460", "09", "China Unicom", "中国联通"),
        OperatorInfo("CT", "460", "03", "China Telecom", "中国电信"),
        OperatorInfo("CT", "460", "05", "China Telecom", "中国电信"),
        OperatorInfo("CT", "460", "11", "China Telecom", "中国电信"),
        OperatorInfo("CBN", "460", "15", "China Broadcasting Network", "中国广电"),
    )

    fun resolve(mcc: String?, mnc: String?): OperatorInfo? {
        if (mcc.isNullOrBlank() || mnc.isNullOrBlank()) return null
        val normalizedMnc = mnc.padStart(2, '0')
        return operators.firstOrNull { it.mcc == mcc && it.mnc == normalizedMnc }
    }

    fun resolveFromPlmn(plmn: String?): OperatorInfo? {
        if (plmn.isNullOrBlank() || plmn.length < 5) return null
        val mcc = plmn.take(3)
        val mnc = plmn.drop(3)
        return resolve(mcc, mnc)
    }
}

object NrArfcn {
    /**
     * 3GPP TS 38.104 global NR-ARFCN raster.
     * Returned value is the RF reference frequency in MHz.
     */
    fun toMhz(nrarfcn: Int?): Double? {
        if (nrarfcn == null || nrarfcn < 0) return null
        return when {
            nrarfcn <= 599_999 -> nrarfcn * 0.005
            nrarfcn <= 2_016_666 -> 3000.0 + (nrarfcn - 600_000) * 0.015
            nrarfcn <= 3_279_165 -> 24250.0 + (nrarfcn - 2_016_667) * 0.060
            else -> null
        }
    }
}

object BandCatalog {
    private data class BandDefinition(
        val number: Int,
        val displayName: String,
        val operatorCode: String?,
        val startMhz: Double,
        val endMhz: Double,
        val noteEn: String,
        val noteZh: String,
    )

    private val definitions = listOf(
        BandDefinition(
            number = 41,
            displayName = "n41",
            operatorCode = "CMCC",
            startMhz = 2515.0,
            endMhz = 2675.0,
            noteEn = "China Mobile 2.6 GHz 5G",
            noteZh = "中国移动 2.6 GHz 5G",
        ),
        BandDefinition(
            number = 79,
            displayName = "n79",
            operatorCode = "CMCC",
            startMhz = 4800.0,
            endMhz = 4900.0,
            noteEn = "China Mobile 4.9 GHz 5G",
            noteZh = "中国移动 4.9 GHz 5G",
        ),
        BandDefinition(
            number = 8,
            displayName = "GSM900",
            operatorCode = "CMCC",
            startMhz = 934.0,
            endMhz = 949.0,
            noteEn = "Legacy GSM900 downlink",
            noteZh = "传统 GSM900 下行",
        ),
        BandDefinition(
            number = 28,
            displayName = "n28",
            operatorCode = "CBN",
            startMhz = 758.0,
            endMhz = 788.0,
            noteEn = "China Broadcasting Network 700 MHz",
            noteZh = "中国广电 700 MHz",
        ),
        BandDefinition(
            number = 78,
            displayName = "n78",
            operatorCode = "CU",
            startMhz = 3500.0,
            endMhz = 3600.0,
            noteEn = "China Unicom 3.5 GHz 5G",
            noteZh = "中国联通 3.5 GHz 5G",
        ),
        BandDefinition(
            number = 78,
            displayName = "n78",
            operatorCode = "CT",
            startMhz = 3400.0,
            endMhz = 3500.0,
            noteEn = "China Telecom 3.5 GHz 5G",
            noteZh = "中国电信 3.5 GHz 5G",
        ),
        BandDefinition(
            number = 1,
            displayName = "n1",
            operatorCode = "CT",
            startMhz = 2110.0,
            endMhz = 2170.0,
            noteEn = "China Telecom 2.1 GHz 5G",
            noteZh = "中国电信 2.1 GHz 5G",
        ),
    )

    fun resolve(
        frequencyMhz: Double?,
        operator: OperatorInfo?,
        reportedBands: IntArray = intArrayOf(),
    ): BandMatch? {
        val reported = reportedBands.toSet()
        val possible = definitions.filter { definition ->
            definition.number in reported ||
                (frequencyMhz != null && frequencyMhz in definition.startMhz..definition.endMhz)
        }
        if (possible.isEmpty()) return null

        val best = possible.maxByOrNull { definition ->
            var score = 0
            if (definition.number in reported) score += 8
            if (frequencyMhz != null && frequencyMhz in definition.startMhz..definition.endMhz) {
                score += 4
            }
            if (operator != null && definition.operatorCode == operator.code) score += 2
            score
        } ?: return null

        val operatorMatches = operator == null || best.operatorCode == operator.code
        val rangeMatches = frequencyMhz != null &&
            frequencyMhz in best.startMhz..best.endMhz

        val noteEn = if (operatorMatches && rangeMatches) {
            best.noteEn
        } else {
            "${best.noteEn}; operator sub-band unverified"
        }
        val noteZh = if (operatorMatches && rangeMatches) {
            best.noteZh
        } else {
            "${best.noteZh}；运营商子频段未确认"
        }

        return BandMatch(
            name = best.displayName,
            operatorCode = best.operatorCode,
            startMhz = best.startMhz,
            endMhz = best.endMhz,
            noteEn = noteEn,
            noteZh = noteZh,
        )
    }

    fun harmonicNear(fundamentalMhz: Double, observedMhz: Double, tolerancePercent: Double = 1.5): Int? {
        if (fundamentalMhz <= 0.0 || observedMhz <= 0.0) return null
        val order = (observedMhz / fundamentalMhz).toInt().coerceAtLeast(2)
        val expected = fundamentalMhz * order
        val errorPercent = abs(observedMhz - expected) / expected * 100.0
        return if (errorPercent <= tolerancePercent) order else null
    }
}

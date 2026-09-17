package com.akshaykadam.pixelboard.patches.gboard.registry

import com.akshaykadam.pixelboard.patches.shared.Patch
import com.akshaykadam.pixelboard.patches.shared.ResourcePatchBuilder
import com.akshaykadam.pixelboard.patches.gboard.features.advancedvoice.gboardAdvancedVoice1803ZhTwPatch
import com.akshaykadam.pixelboard.patches.gboard.features.advancedvoice.gboardAdvancedVoiceFlagValuePatch
import com.akshaykadam.pixelboard.patches.gboard.features.rambler.gboardRambler1803OfficialSelectorPatch

internal class GboardContributionWiringUnit(
    val contributionIds: Set<String>,
    private val dependencySupplier: () -> Array<Patch<*>>,
) {
    fun dependencies(): Array<Patch<*>> = dependencySupplier()
}

internal object GboardContributionWiring {
    private val unitsByFeature = mapOf(
        "advanced_voice_typing" to listOf(
            unit("advanced_voice_typing.flag") {
                arrayOf(gboardAdvancedVoiceFlagValuePatch)
            },
            unit("advanced_voice_typing.bytecode") {
                arrayOf(
                    gboardAdvancedVoice1803ZhTwPatch,
                    gboardRambler1803OfficialSelectorPatch,
                )
            },
        ),
    )

    val registeredContributionIdsByFeature: Map<String, Set<String>> =
        unitsByFeature.mapValues { (_, units) ->
            units.flatMapTo(linkedSetOf()) { unit -> unit.contributionIds }
        }

    fun activeContributionIds(featureId: String, admitted: Set<String>): Set<String> =
        activeUnits(featureId, admitted).flatMapTo(linkedSetOf()) { unit -> unit.contributionIds }

    fun wireAdmitted(
        featureId: String,
        admitted: Set<String>,
        builder: ResourcePatchBuilder,
    ) {
        activeUnits(featureId, admitted).forEach { unit ->
            builder.dependsOn(*unit.dependencies())
        }
    }

    private fun activeUnits(
        featureId: String,
        admitted: Set<String>,
    ): List<GboardContributionWiringUnit> {
        val units = unitsByFeature[featureId] ?: return emptyList()
        val registeredIds = units.flatMapTo(linkedSetOf()) { unit -> unit.contributionIds }
        check(admitted.all { contributionId -> contributionId in registeredIds }) {
            "$featureId admits unregistered contribution IDs ${(admitted - registeredIds).sorted()}"
        }
        units.forEach { unit ->
            val admittedUnitIds = unit.contributionIds.intersect(admitted)
            check(admittedUnitIds.isEmpty() || admittedUnitIds == unit.contributionIds) {
                "$featureId partially admits indivisible execution unit " +
                    "${unit.contributionIds.sorted()}: ${admittedUnitIds.sorted()}"
            }
        }
        return units.filter { unit -> unit.contributionIds.all(admitted::contains) }
    }

    private fun unit(
        vararg contributionIds: String,
        dependencySupplier: () -> Array<Patch<*>>,
    ) = GboardContributionWiringUnit(contributionIds.toSet(), dependencySupplier)
}

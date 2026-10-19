// Read-only native structure/trace proof. Physical tests and compliance remain NOT_VERIFIED.
assert session.sessionResource.URI.toString().toLowerCase().contains('/fsae-2027-electric-arcadia/')
def req = api.requirements()
assert req.available() && req.viewpointEnabled()
assert api.find('Constraint', 'FSAE2027:SD semantic repair v2 APPLIED') != null
def stableId = { key -> java.util.UUID.nameUUIDFromBytes(('FSAE2027:SD:' + key).getBytes('UTF-8')).toString() }
def element = { key ->
    def value = api.findById(stableId(key))
    assert value != null && value.eResource() != null && !value.eIsProxy(): 'Missing persisted shutdown element ' + key
    assert value.eResource().getURIFragment(value) == api.id(value): 'Stale serialized-reference ID for ' + key
    assert value.eResource().getEObject(api.id(value)) == value: 'Native resource lookup disagrees with ID for ' + key
    value
}
def module = req.find('FSAE-2027-V1:source-module')
def derivedModule = req.find('FSAE2027:SD:derived-module')
assert module != null && derivedModule != null && module != derivedModule
def sourceRequirements = req.requirements(module)
assert sourceRequirements.size() == 3197: 'The original source-module paragraph bijection must remain intact'
assert sourceRequirements.every { it.ReqIFIdentifier.startsWith('FSAE-2027-V1:') }
def derived = req.requirements(derivedModule)
assert derived.size() == 39
def sources = sourceRequirements.findAll { req.attributes(it).shutdown_slice_id == 'FSAE2027:shutdown-v1' }
assert sources.size() == 122
assert sources.every { !it.ReqIFIdentifier.startsWith('FSAE-2027-V1:EV.8.7') }
sources.each { requirement ->
    def attributes = req.attributes(requirement)
    assert attributes.coverage_status == 'allocated' && attributes.interpretation_status == 'interpreted'
    assert attributes.verification_status == 'not_verified'
    assert attributes.source_sha256 == 'f71df5552e341dab1b58f1ff47212e7281953d66b9c41833de8347560913ed0e'
    assert attributes.source_text_sha256 != null
    assert attributes.model_element_ids.startsWith('[') && attributes.verification_element_ids.startsWith('[')
    def verifies = requirement.ownedRelations.findAll { it.eClass().name == 'CapellaIncomingRelation' && it.relationType?.ReqIFLongName == 'verify' }
    assert verifies.any { it.source == requirement && it.target?.eClass().name == 'Constraint' && it.target.summary.startsWith('NOT_VERIFIED') }
}
derived.each { requirement ->
    def attributes = req.attributes(requirement)
    assert attributes.coverage_status == 'allocated' && attributes.verification_status == 'not_verified'
    assert requirement.ownedRelations.any { it.eClass().name == 'InternalRelation' && it.source == requirement && sources.contains(it.target) }
    assert requirement.ownedRelations.any { it.eClass().name == 'CapellaIncomingRelation' && it.source == requirement && it.target?.eClass().name == 'Constraint' }
}
['bms','imd','series','latch','coils','torque','indicate'].each { key ->
    def logical = element('logical:' + key)
    def behavior = element('behavior:' + key)
    def equipment = element('node:' + key)
    def ci = element('ci:' + key)
    assert behavior.nature == PhysicalComponentNature.BEHAVIOR && equipment.nature == PhysicalComponentNature.NODE
    assert ci.ownedPhysicalArtifactRealizations.any { it.targetElement == equipment }
    assert ci.ownedPhysicalArtifactRealizations.any { it.targetElement == behavior }
    assert behavior.ownedComponentRealizations.any { it.targetElement == logical }
    def deployment = element('deployment:' + key)
    assert deployment.location.abstractType == equipment && deployment.deployedElement.abstractType == behavior
}
def functionKeys = ['series','accept','cutout','latch','reset','coils','current','discharge','torque','glv','glv-supply','glv-master','switches','interlocks',
    'voltage','temperature','sense-fuse','bms-health','bms','balance','imd','indicate','lamp-check','demonstrate']
functionKeys.each { key ->
    ['SA','LA','PA'].each { layer ->
        def function = element('function:' + layer + ':' + key)
        assert function.eClass().name == [SA:'SystemFunction',LA:'LogicalFunction',PA:'PhysicalFunction'][layer]
        assert !function.componentFunctionalAllocations.isEmpty()
        if (layer != 'SA') assert function.ownedFunctionRealizations.size() == 1
        assert function.ownedExtensions.any { it.eClass().name == 'CapellaOutgoingRelation' && it.relationType?.ReqIFLongName == 'satisfy' }
    }
}
['accept','torque'].each { key ->
    def activity = element('function:OA:' + key)
    assert activity.eClass().name == 'OperationalActivity' && activity.subFunctions.isEmpty()
    assert activity.allocatorEntities.any { it.name == 'FSAE2027:Vehicle in operational context' }
    assert element('function:SA:' + key).ownedFunctionRealizations.any { it.targetElement == activity }
    assert activity.ownedExtensions.any { it.eClass().name == 'CapellaOutgoingRelation' && it.relationType?.ReqIFLongName == 'satisfy' }
}
['SA','LA','PA'].each { layer ->
    assert element('flow:' + layer + ':low-voltage:glv').source.eContainer() == element('function:' + layer + ':glv-supply')
}
def flows = api.findAll('FunctionalExchange').findAll { it.name?.startsWith('FSAE2027:SD ') }
assert flows.size() == 78
flows.each { exchange ->
    assert exchange.source.eContainer().subFunctions.isEmpty()
    assert exchange.target.eContainer().subFunctions.isEmpty()
}
flows.findAll { it.name.startsWith('FSAE2027:SD SA ') }.each { upper ->
    def suffix = upper.name.substring('FSAE2027:SD SA '.length())
    def middle = flows.find { it.name == 'FSAE2027:SD LA ' + suffix }
    def lower = flows.find { it.name == 'FSAE2027:SD PA ' + suffix }
    [[middle,upper],[lower,middle]].each { pair ->
        assert pair[0].ownedFunctionalExchangeRealizations.any { it.sourceElement == pair[0] && it.targetElement == pair[1] }
        ['source','target'].each { side ->
            def from = pair[0][side], to = pair[1][side]
            assert from.ownedPortRealizations.any { it.sourceElement == from && it.targetElement == to }
            assert from.eContainer().ownedFunctionRealizations.any { it.targetElement == to.eContainer() }
        }
    }
}
def wireKeys = (0..10).collect { 'series-' + it } + ['IR-POSITIVE','IR-NEGATIVE','PRECHARGE-RELAY'].collectMany { ['load-' + it, 'return-' + it] } + ['all-glv-loads-supply','all-glv-loads-return']
wireKeys.each { key ->
    def link = element('wire:' + key)
    assert link.eClass().name == 'PhysicalLink' && link.linkEnds.size() == 2 && link.ownedPhysicalLinkEnds.size() == 2
    link.linkEnds.each { endpoint ->
        assert endpoint.eClass().name == 'PhysicalLinkEnd'
        assert endpoint.part.abstractType == endpoint.port.eContainer()
        assert endpoint.part.eResource() != null && endpoint.port.eResource() != null
    }
}
def lastSwitchWire = element('wire:series-10')
assert lastSwitchWire.linkEnds[0].part.abstractType == element('node:TSMS')
assert lastSwitchWire.linkEnds[1].part.abstractType == element('node:INTERLOCKS')
['BMS-CONTACT','IMD-CONTACT','BSPD-CONTACT'].each { key ->
    assert element('node:' + key).ownedPropertyValues.any { it.name == 'Normally open protective output' && it.value == true }
}
['BUTTON-LEFT','BUTTON-RIGHT','BUTTON-COCKPIT'].each { element('node:' + it) }
def machine = element('state-machine')
assert machine.eContainer() == element('logical:latch')
assert machine.ownedRegions.size() == 1
def region = machine.ownedRegions[0]
assert region.ownedStates.size() == 5 && region.ownedTransitions.size() == 8
assert region.ownedTransitions.every { it.source != null && it.target != null && it.guard?.ownedSpecification != null && it.guard.eContainer() == it }
def release = element('transition:Only local manual reset releases latch')
assert release.source == element('state:Latched protective trip') && release.target == element('state:Open')
assert !region.ownedTransitions.any { it.source == element('state:Latched protective trip') && it.target == element('state:Energized') }
assert element('state:Open').availableAbstractFunctions.contains(element('function:LA:voltage'))
assert element('state:Open').availableAbstractFunctions.contains(element('function:LA:temperature'))
assert api.layer('SA').system.ownedSystemComponents.isEmpty() && api.layer('SA').system.ownedFeatures.isEmpty()
return [slice_id:'FSAE2027:shutdown-v1', native_structure_verified:true, source_records_allocated:sources.size(),
    source_records_retained_unresolved_figure_labels:13, derived_candidate_obligations:derived.size(),
    functions:72, operational_shutdown_interfaces:2, realized_functional_exchanges:52,
    physical_current_links:wireKeys.size(), native_states:5, guarded_transitions:8,
    physical_verification_status:'not_verified', completion_status:'partial_candidate_design_with_TBD_hardware_and_dependencies']

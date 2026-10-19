// Read-only native graph checks. Passing these checks is not physical verification or compliance.
assert session.sessionResource.URI.isPlatformResource()
assert session.sessionResource.URI.segment(1) == 'FSAE-2027-Electric-Arcadia'
assert api.layer('SA').system.summary == 'FSAE2027:bootstrap-v1'
def req = api.requirements()
assert req.available() && req.viewpointEnabled()
def identifier = { item -> item.eGet(item.eClass().getEStructuralFeature('ReqIFIdentifier')) }
def modules = req.listModules().collectEntries { [(identifier(it)):it] }
def sourceModule = modules['FSAE-2027-V1:source-module'], derivedModule = modules['FSAE2027:DD:module']
assert sourceModule != null && derivedModule != null
def requirementsById = [:]
(req.requirements(sourceModule) + req.requirements(derivedModule)).each { item ->
    def id = identifier(item); assert !requirementsById.containsKey(id)
    requirementsById[id] = item
}
def prefix = 'FSAE2027:DD '
assert api.find('Constraint',prefix + 'slice v1 APPLIED') != null
/* OBLIGATIONS */
def functions = [
    'pedal-return':['Return accelerator pedal independently to zero','Independent mechanical pedal return'],
    'apps-input':['Acquire separate analogue APPS channels A and B','Independent analogue APPS channels'],
    'apps-fault':['Detect APPS wiring range and plausibility failures','APPS BSE and brake conflict safety guard'],
    'brake-input':['Acquire brake demand and mechanical engagement','Brake demand and engagement sensing'],
    'brake-fault':['Detect BSE wiring and range failures','APPS BSE and brake conflict safety guard'],
    'brake-conflict':['Latch brake accelerator conflict motor inhibit','APPS BSE and brake conflict safety guard'],
    'torque-bound':['Bound total wheel torque by driver demand','Driver bounded torque and power inhibit'],
    'forward-only':['Reject motor powered reverse','Driver bounded torque and power inhibit'],
    'regen-inhibit':['Inhibit regeneration in low speed interval','Driver bounded torque and power inhibit'],
    'motor-inhibit':['Motor power inhibit enforcement','Driver bounded torque and power inhibit'],
    'bspd-monitor':['BSPD standalone brake and current monitor','Independent nonprogrammable BSPD'],
    'bspd-trip':['BSPD latched shutdown request','Independent nonprogrammable BSPD'],
    'inspection':['Provide and exercise safe inspection interfaces','Isolated BSPD and sensor inspection interface'],
    'vehicle-speed':['Observe vehicle speed and validity','powertrain-boundary'],
    'tractive-current':['Observe independently sensed tractive current','Independent nonprogrammable BSPD'],
    'motor-command':['Execute permitted motor power and forward torque command','powertrain-boundary']
]
def types = [OA:'OperationalActivity',SA:'SystemFunction',LA:'LogicalFunction',PA:'PhysicalFunction']
def nodes = [], edges = [], selected = [:], fnByKey = [:]
def get = { element, field ->
    def feature = element.eClass().getEStructuralFeature(field)
    assert feature != null: element.eClass().name + '.' + field
    element.eGet(feature)
}
def record = { element ->
    assert api.id(element) != null && !api.id(element).isEmpty()
    nodes << [id:api.id(element),type:element.eClass().name,name:element.name]
}
functions.each { key,spec ->
    fnByKey[key] = [:]
    types.each { layer,type ->
        def fn = api.find(type,prefix + spec[0]); assert fn != null
        fnByKey[key][layer] = fn; record(fn)
        if (layer == 'LA' || layer == 'PA') {
            def ownerName = spec[1] == 'powertrain-boundary' ? 'FSAE2027:powertrain ' + (layer == 'LA' ? 'Logical subsystem' : 'Physical behavior') : prefix + spec[1] + (layer == 'LA' ? ' Logical component' : ' Physical behavior')
            def owner = api.find(layer == 'LA' ? 'LogicalComponent' : 'PhysicalComponent',ownerName)
            assert owner != null
            def allocation = owner.ownedFunctionalAllocation.find { it.targetElement == fn && it.sourceElement == owner }
            assert allocation != null: layer + ' allocation missing for ' + key
            edges << [source:api.id(owner),target:api.id(fn),type:'ComponentFunctionalAllocation',id:api.id(allocation)]
        }
        assert fn.ownedFunctions.isEmpty(): 'DD allocations and endpoints must be leaf functions'
    }
    [['SA','OA'],['LA','SA'],['PA','LA']].each { pair ->
        assert fnByKey[key][pair[0]].ownedFunctionRealizations.any { it.targetElement == fnByKey[key][pair[1]] }
    }
}
def componentNames = functions.values().collect { it[1] }.findAll { it != 'powertrain-boundary' }.unique()
componentNames.each { name ->
    def logical = api.find('LogicalComponent',prefix + name + ' Logical component')
    def behavior = api.find('PhysicalComponent',prefix + name + ' Physical behavior')
    def equipment = api.find('PhysicalComponent',prefix + name + ' Equipment')
    def ci = api.find('ConfigurationItem',prefix + name + ' CI')
    assert logical != null && behavior != null && equipment != null && ci != null
    assert logical.eContainer().eClass().name == 'LogicalComponent'
    assert behavior.nature == PhysicalComponentNature.BEHAVIOR && equipment.nature == PhysicalComponentNature.NODE
    assert ci.kind == ConfigurationItemKind.HWCI
    def behaviorPart = behavior.eContainer().ownedFeatures.find { it instanceof Part && it.abstractType == behavior }
    def equipmentPart = equipment.eContainer().ownedFeatures.find { it instanceof Part && it.abstractType == equipment }
    assert behaviorPart != null && equipmentPart != null
    def deployment = equipmentPart.ownedDeploymentLinks.find { it.deployedElement == behaviorPart && it.location == equipmentPart }
    assert deployment != null
    assert ci.ownedPhysicalArtifactRealizations.any { it.targetElement == behavior }
    assert ci.ownedPhysicalArtifactRealizations.any { it.targetElement == equipment }
    [logical,behavior,equipment,ci].each { record(it) }
    edges << [source:api.id(behavior),target:api.id(equipment),type:'PartDeploymentLink',id:api.id(deployment)]
}
['SA','LA','PA'].each { layer ->
    def receiver = api.find(types[layer],'FSAE2027:SD Accept independent shutdown trips')
    def trip = api.findAll('FunctionalExchange').find { it.name == prefix + layer + ' BSPD trip request' }
    assert receiver != null && trip != null
    // FunctionalExchange endpoints are native function ports. Resolve their containing functions.
    assert trip.source.eContainer() == fnByKey['bspd-trip'][layer] && trip.target.eContainer() == receiver
    edges << [source:api.id(fnByKey['bspd-trip'][layer]),target:api.id(receiver),type:'FunctionalExchange',id:api.id(trip)]
}
def stateMachines = []
def guardOwner = api.find('LogicalComponent',prefix + 'APPS BSE and brake conflict safety guard Logical component')
['brake-conflict':['Brake conflict latch','Driver demand brake conflict state machine'],
    'apps-fault':['APPS independent fault timing','Driver demand APPS fault state machine'],
    'brake-fault':['BSE independent fault timing','Driver demand BSE fault state machine']].each { key,spec ->
    def guardMachine = guardOwner.ownedStateMachines.find { it.name == prefix + spec[1] }
    assert guardMachine != null && guardMachine.ownedRegions.size() == 1
    def region = guardMachine.ownedRegions.find { it.name == prefix + spec[0] }; assert region != null
    def expected = region.ownedStates.findAll { it.eClass().name == 'State' }
    def fn = fnByKey[key].LA
    def feature = fn.eClass().getEStructuralFeature('availableInStates')
    assert feature != null && feature.changeable && !feature.derived
    assert fn.availableInStates.toSet() == expected.toSet(): 'Stored availability must remain scoped to the corresponding monitor region'
    stateMachines << guardMachine
}
['LogicalComponent','PhysicalComponent'].each { type ->
    def owner = api.find(type,prefix + 'Independent nonprogrammable BSPD' + (type == 'LogicalComponent' ? ' Logical component' : ' Physical behavior'))
    def sm = owner.ownedStateMachines.find { it.name == prefix + 'Nonprogrammable BSPD hardware state machine' }
    assert sm != null && sm.ownedRegions.size() == 1
    def layer = type == 'LogicalComponent' ? 'LA' : 'PA'
    def monitoring = sm.ownedRegions[0].ownedStates.find { it.name == prefix + 'BSPD monitoring' }
    def requested = sm.ownedRegions[0].ownedStates.find { it.name == prefix + 'BSPD trip requested' }
    assert monitoring != null && requested != null
    assert fnByKey['bspd-monitor'][layer].availableInStates.toSet() == [monitoring,requested].toSet()
    assert fnByKey['bspd-trip'][layer].availableInStates.toSet() == [requested].toSet(): 'BSPD trip output must be scoped to the requested state'
    // Latch release is owned by the shutdown slice, never an automatic BSPD reset transition.
    assert sm.ownedRegions[0].ownedTransitions.count { it.source.eClass().name == 'State' } == 2
    stateMachines << sm
}
stateMachines.each { sm ->
    record(sm)
    sm.ownedRegions.each { region ->
        assert region.ownedStates.count { it.eClass().name == 'InitialPseudoState' } == 1
        region.ownedTransitions.each { transition ->
            assert region.ownedStates.contains(transition.source) && region.ownedStates.contains(transition.target)
            if (transition.source.eClass().name == 'State') {
                assert transition.guard != null && transition.guard.eContainer() == transition
                assert transition.guard.ownedSpecification.bodies.size() == 1
            }
            edges << [source:api.id(transition.source),target:api.id(transition.target),type:'StateTransition',id:api.id(transition),condition:transition.triggerDescription]
        }
    }
}
assert derivedModule != null && derivedModule.eContainer() == api.layer('SA')
assert req.requirements(derivedModule).size() == obligations.size()
obligations.each { obligation ->
    def derived = requirementsById['FSAE2027:DD:' + obligation.id]; assert derived != null
    def verification = api.find('Constraint',prefix + 'verify ' + obligation.id + ' NOT VERIFIED'); assert verification != null
    def attributes = req.attributes(derived)
    assert attributes.interpretation_status == 'interpreted' && attributes.coverage_status == 'allocated'
    assert attributes.verification_status == 'not_verified' && attributes.compliance_status == 'not_claimed'
    assert attributes.evidence_status == 'not_available'
    def encodedIds = attributes.model_element_ids
    assert encodedIds.startsWith('["') && encodedIds.endsWith('"]') && !encodedIds.contains('\\')
    def ownerIds = encodedIds.substring(2,encodedIds.length()-2).split('","').toList()
    assert !ownerIds.isEmpty() && ownerIds.toSet().size() == ownerIds.size()
    ownerIds.each { id ->
        def owner = api.findById(id); assert owner != null
        def relation = get(owner,'ownedExtensions').find { it.eClass().name == 'CapellaOutgoingRelation' && get(it,'source') == owner && get(it,'target') == derived }
        assert relation != null: 'Missing native satisfy trace for ' + obligation.id
    }
    assert get(derived,'ownedRelations').any { it.eClass().name == 'CapellaIncomingRelation' && get(it,'source') == derived && get(it,'target') == verification }
    obligation.source_refs.each { ref ->
        def source = requirementsById[ref.clause_id]; assert source != null
        def sourceAttributes = req.attributes(source)
        assert sourceAttributes.source_sha256 == ref.source_sha256 && sourceAttributes.source_text_sha256 == ref.source_text_sha256
        assert sourceAttributes.coverage_slice == 'driver-demand-v1'
        assert sourceAttributes.interpretation_status == 'interpreted' && sourceAttributes.coverage_status == 'allocated'
        assert sourceAttributes.verification_status == 'not_verified' && sourceAttributes.compliance_status == 'not_claimed'
        assert get(derived,'ownedRelations').any { it.eClass().name == 'InternalRelation' && get(it,'source') == derived && get(it,'target') == source }
        assert get(source,'ownedRelations').any { it.eClass().name == 'CapellaIncomingRelation' && get(it,'source') == source && get(it,'target') == verification }
        selected[ref.clause_id] = source
    }
    record(verification)
}
assert selected.size() == 57
def exchanges = api.findAll('FunctionalExchange').findAll { it.name?.startsWith(prefix) }
assert exchanges.size() == 88
def byName = [:]
exchanges.each { exchange ->
    def parts = exchange.name.substring(prefix.length()).split(' ',2)
    if (!byName.containsKey(parts[1])) byName[parts[1]] = [:]
    byName[parts[1]][parts[0]] = exchange
    def sourceFn = parts[0] == 'OA' ? exchange.source : exchange.source.eContainer()
    def targetFn = parts[0] == 'OA' ? exchange.target : exchange.target.eContainer()
    assert sourceFn.ownedFunctions.isEmpty() && targetFn.ownedFunctions.isEmpty(): 'Every DD interface must use leaf endpoints'
    if (parts[1] == 'Vehicle speed and validity') assert sourceFn == fnByKey['vehicle-speed'][parts[0]]
    if (parts[1] == 'Independently sensed tractive current') assert sourceFn == fnByKey['tractive-current'][parts[0]]
    if (parts[1] == 'Permitted motor power and forward torque command') assert targetFn == fnByKey['motor-command'][parts[0]]
}
byName.each { name,byLayer ->
    [['SA','OA'],['LA','SA'],['PA','LA']].each { pair ->
        def lower = byLayer[pair[0]], higher = byLayer[pair[1]]
        assert lower.ownedFunctionalExchangeRealizations.any { it.sourceElement == lower && it.targetElement == higher }
        if (pair[1] != 'OA') ['source','target'].each { field ->
            assert lower[field].ownedPortRealizations.any { it.sourceElement == lower[field] && it.targetElement == higher[field] }
        }
    }
}
def decisions = api.find('Constraint',prefix + 'decision unresolved calibration thresholds and recovery')
assert decisions != null
return [phase:'driver-demand-native-structure-verification',source_paragraph_count:selected.size(),
    derived_requirement_count:obligations.size(),native_function_count:64,native_functional_exchange_count:exchanges.size(),
    native_state_machine_count:stateMachines.size(),verification_status:'not_verified',evidence_status:'not_available',
    compliance_status:'not_claimed',unresolved_decision:decisions.summary,source_clause_ids:selected.keySet().toList(),
    graph:[nodes:nodes.unique { it.id },edges:edges]]

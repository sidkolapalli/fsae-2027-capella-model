// Additive migration for an already-created DD v1 slice. Never rerun 30 to repair an existing model.
// Root-owned bootstrap repair handles aggregate allocations and base interfaces separately.
assert session.sessionResource.URI.isPlatformResource()
assert session.sessionResource.URI.segment(1) == 'FSAE-2027-Electric-Arcadia'
assert api.layer('SA').system.summary == 'FSAE2027:bootstrap-v1'
def prefix = 'FSAE2027:DD '
assert api.find('Constraint',prefix + 'slice v1 APPLIED') != null
def req = api.requirements()
assert req.available() && req.viewpointEnabled()
/* OBLIGATIONS */
def types = [OA:'OperationalActivity',SA:'SystemFunction',LA:'LogicalFunction',PA:'PhysicalFunction']
def identifier = { item -> item.eGet(item.eClass().getEStructuralFeature('ReqIFIdentifier')) }
def modules = req.listModules().collectEntries { [(identifier(it)):it] }
def sourceModule = modules['FSAE-2027-V1:source-module'], derivedModule = modules['FSAE2027:DD:module']
assert sourceModule != null && derivedModule != null
def requirementsById = [:]
(req.requirements(sourceModule) + req.requirements(derivedModule)).each { item ->
    def id = identifier(item); assert !requirementsById.containsKey(id): 'Duplicate native requirement identifier'
    requirementsById[id] = item
}
def functionsByLayer = [:]
types.each { layer,type ->
    def byName = [:]
    api.findAll(type).each { fn ->
        if (fn.name?.startsWith('FSAE2027:')) {
            assert !byName.containsKey(fn.name): 'Ambiguous candidate function name'
            byName[fn.name] = fn
        }
    }
    functionsByLayer[layer] = byName
}
def existingRepairMarker = api.find('Constraint',prefix + 'native semantics repair v2 APPLIED')
def shutdownReceivers = [:], shutdownInhibits = [:], powertrain = [:]
types.each { layer,type ->
    shutdownReceivers[layer] = functionsByLayer[layer]['FSAE2027:SD Accept independent shutdown trips']
    shutdownInhibits[layer] = functionsByLayer[layer]['FSAE2027:SD Remove motor torque on shutdown']
    powertrain[layer] = functionsByLayer[layer]['FSAE2027:powertrain Convert traction energy into vehicle motion']
    assert shutdownReceivers[layer] != null && shutdownInhibits[layer] != null && powertrain[layer] != null
}
def owner = [OA:api.find('Entity','FSAE2027:Vehicle in operational context'),SA:api.layer('SA').system,
    LA:api.find('LogicalComponent','FSAE2027:powertrain Logical subsystem'),PA:api.find('PhysicalComponent','FSAE2027:powertrain Physical behavior')]
def bspd = [OA:owner.OA,SA:owner.SA,
    LA:api.find('LogicalComponent',prefix + 'Independent nonprogrammable BSPD Logical component'),
    PA:api.find('PhysicalComponent',prefix + 'Independent nonprogrammable BSPD Physical behavior')]
def guard = api.find('LogicalComponent',prefix + 'APPS BSE and brake conflict safety guard Logical component')
assert guard != null && owner.values().every { it != null } && bspd.values().every { it != null }
obligations.each { obligation ->
    assert requirementsById['FSAE2027:DD:' + obligation.id] != null
    obligation.source_refs.each { ref ->
        def source = requirementsById[ref.clause_id]; assert source != null
        def attributes = req.attributes(source)
        assert attributes.source_sha256 == ref.source_sha256 && attributes.source_text_sha256 == ref.source_text_sha256
    }
}
def exchanges = api.findAll('FunctionalExchange').findAll { it.name?.startsWith(prefix) }
assert exchanges.size() == 88
def before = exchanges.collectEntries { exchange ->
    [(api.id(exchange)):[source:api.id(exchange.source),target:api.id(exchange.target)]]
}
def leaves = [:], repairedMachines = [], traceAdds = 0
def boundarySpecs = [
    [key:'vehicle-speed',name:'Observe vehicle speed and validity',owners:owner],
    [key:'tractive-current',name:'Observe independently sensed tractive current',owners:bspd],
    [key:'motor-command',name:'Execute permitted motor power and forward torque command',owners:owner]
]
def ownerIds = [:]
obligations.each { obligation ->
    def selected = [requirementsById['FSAE2027:DD:' + obligation.id]] + obligation.source_refs.collect { requirementsById[it.clause_id] }
    selected.each { requirement ->
        def encoded = req.attributes(requirement).model_element_ids
        assert encoded != null && encoded.startsWith('[') && encoded.endsWith(']') && !encoded.contains('\\')
        def ids = encoded == '[]' ? [] : encoded.substring(2,encoded.length()-2).split('","').toList()
        ownerIds[identifier(requirement)] = new java.util.LinkedHashSet(ids)
    }
}
def addOwners = { requirement, elements ->
    def ids = ownerIds[identifier(requirement)]
    assert ids != null
    def changed = false
    elements.each { element ->
        req.satisfy(element,requirement)
        def id = api.id(element); assert id != null && !id.contains('"') && !id.contains('\\')
        changed = ids.add(id) || changed
    }
    if (changed) {
        req.setAttribute(requirement,'model_element_ids','[' + ids.collect { '"' + it + '"' }.join(',') + ']')
    }
}
api.batch('Repair DD leaf interfaces independent state machines and native perspective traces') {
    boundarySpecs.each { spec ->
        leaves[spec.key] = [:]
        types.each { layer,type ->
            def fn = functionsByLayer[layer][prefix + spec.name]
            if (fn == null) fn = api.create(type,prefix + spec.name,powertrain[layer])
            assert fn.ownedFunctions.isEmpty(): 'Boundary endpoint must be a leaf'
            api.set(fn,'summary','Provisional DD leaf interface; sensor/controller implementation and physical evidence remain unresolved. NOT VERIFIED.')
            assert api.allocate(fn,spec.owners[layer])
            leaves[spec.key][layer] = fn
        }
        [['SA','OA'],['LA','SA'],['PA','LA']].each { pair ->
            def lower = leaves[spec.key][pair[0]], higher = leaves[spec.key][pair[1]]
            if (!lower.ownedFunctionRealizations.any { it.targetElement == higher }) api.link(lower,higher,'realization')
        }
    }
    // Reparent the existing function port itself. Exchange, port, view and trace identifiers remain intact.
    def endpoint = { exchange, field, fn, layer ->
        if (layer == 'OA') api.set(exchange,field,fn)
        else {
            def port = exchange[field]
            assert port.eClass().name == (field == 'source' ? 'FunctionOutputPort' : 'FunctionInputPort')
            if (port.eContainer() != fn) (field == 'source' ? fn.outputs : fn.inputs).add(port)
        }
    }
    types.each { layer,type ->
        ['Vehicle speed and validity','Independently sensed tractive current','Permitted motor power and forward torque command','BSPD trip request','Shutdown motor inhibit'].each { suffix ->
            def exchange = exchanges.find { it.name == prefix + layer + ' ' + suffix }; assert exchange != null
            if (suffix == 'Vehicle speed and validity') endpoint(exchange,'source',leaves['vehicle-speed'][layer],layer)
            if (suffix == 'Independently sensed tractive current') endpoint(exchange,'source',leaves['tractive-current'][layer],layer)
            if (suffix == 'Permitted motor power and forward torque command') endpoint(exchange,'target',leaves['motor-command'][layer],layer)
            if (suffix == 'BSPD trip request') endpoint(exchange,'target',shutdownReceivers[layer],layer)
            if (suffix == 'Shutdown motor inhibit') endpoint(exchange,'source',shutdownInhibits[layer],layer)
        }
    }
    // Preserve the existing machine and all regions/states/transitions/guards; move two regions into independent machines.
    def oldMachine = guard.ownedStateMachines.find { it.name == prefix + 'Driver demand safety state machine' }
    if (oldMachine != null) {
        assert oldMachine.ownedRegions.size() == 3
        api.rename(oldMachine,prefix + 'Driver demand brake conflict state machine')
        ['APPS independent fault timing':'Driver demand APPS fault state machine','BSE independent fault timing':'Driver demand BSE fault state machine'].each { regionName,machineName ->
            def region = oldMachine.ownedRegions.find { it.name == prefix + regionName }; assert region != null
            def sm = api.create('StateMachine',prefix + machineName,guard)
            sm.ownedRegions.add(region)
            api.set(sm,'summary','Independent candidate monitor behavior; exactly one initial state. Existing state and guard IDs preserved; physical verification pending.')
            repairedMachines << sm
        }
    }
    def machines = ['Driver demand brake conflict state machine','Driver demand APPS fault state machine','Driver demand BSE fault state machine'].collect { name ->
        def sm = guard.ownedStateMachines.find { it.name == prefix + name }
        assert sm != null && sm.ownedRegions.size() == 1
        assert sm.ownedRegions[0].ownedStates.count { it.eClass().name == 'InitialPseudoState' } == 1
        sm
    }
    def groups = [:]
    exchanges.each { exchange ->
        def parts = exchange.name.substring(prefix.length()).split(' ',2)
        if (!groups.containsKey(parts[1])) groups[parts[1]] = [:]
        groups[parts[1]][parts[0]] = exchange
        if (parts[0] != 'OA') {
            if (!exchange.source.name) api.rename(exchange.source,prefix + parts[0] + ' ' + parts[1] + ' source')
            if (!exchange.target.name) api.rename(exchange.target,prefix + parts[0] + ' ' + parts[1] + ' target')
        }
    }
    groups.each { name,byLayer ->
        [['SA','OA'],['LA','SA'],['PA','LA']].each { pair ->
            def lower = byLayer[pair[0]], higher = byLayer[pair[1]]; assert lower != null && higher != null
            if (!lower.ownedFunctionalExchangeRealizations.any { it.targetElement == higher }) {
                def relation = org.polarsys.capella.core.data.fa.FaFactory.eINSTANCE.createFunctionalExchangeRealization()
                relation.sourceElement = lower; relation.targetElement = higher
                lower.ownedFunctionalExchangeRealizations.add(relation); traceAdds++
            }
            if (pair[1] != 'OA') ['source','target'].each { field ->
                if (!lower[field].ownedPortRealizations.any { it.targetElement == higher[field] }) {
                    def relation = org.polarsys.capella.core.data.information.InformationFactory.eINSTANCE.createPortRealization()
                    relation.sourceElement = lower[field]; relation.targetElement = higher[field]
                    lower[field].ownedPortRealizations.add(relation); traceAdds++
                }
            }
        }
    }
    // Keep previous native traces and model IDs; append concrete boundary/independent-monitor owners.
    obligations.each { obligation ->
        def additions = []
        boundarySpecs.each { spec -> if (obligation.functions.contains(spec.key)) additions.addAll(leaves[spec.key].values()) }
        if (obligation.functions.contains('apps-fault')) additions << machines[1]
        if (obligation.functions.contains('brake-fault')) additions << machines[2]
        def requirements = [requirementsById['FSAE2027:DD:' + obligation.id]] + obligation.source_refs.collect { requirementsById[it.clause_id] }
        def uniqueAdditions = additions.unique()
        if (!uniqueAdditions.isEmpty()) requirements.each { requirement -> addOwners(requirement,uniqueAdditions) }
    }
    exchanges.each { exchange ->
        assert before.containsKey(api.id(exchange))
        if (exchange.source.eClass().name != 'OperationalActivity') {
            assert api.id(exchange.source) == before[api.id(exchange)].source
            assert api.id(exchange.target) == before[api.id(exchange)].target
        }
    }
    def marker = existingRepairMarker
    if (marker == null) marker = api.create('Constraint',prefix + 'native semantics repair v2 APPLIED',api.layer('SA'))
    api.set(marker,'summary','DD exchanges use leaf endpoints and native perspective/port realizations; independent monitor machines each have one initial state. Source and pre-existing exchange/port/state IDs preserved. Verification remains NOT VERIFIED.')
}
return [phase:'driver-demand-native-semantics-repair',leaf_functions:leaves.collectEntries { key,byLayer -> [(key):byLayer.collectEntries { layer,fn -> [(layer):api.id(fn)] }] },
    new_state_machines:repairedMachines.collect { api.id(it) },new_native_traces:traceAdds,preserved_exchange_count:before.size(),
    verification_status:'not_verified',compliance_status:'not_claimed']

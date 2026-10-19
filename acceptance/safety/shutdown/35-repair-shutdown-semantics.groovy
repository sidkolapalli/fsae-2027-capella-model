// Additive repair for the already-applied v1 shutdown slice. Review the full draft before applying.
// Shared bootstrap aggregate allocations/boundary leaves are repaired separately by the integration draft.
assert session.sessionResource.URI.toString().toLowerCase().contains('/fsae-2027-electric-arcadia/')
assert api.layer('SA').system.summary == 'FSAE2027:bootstrap-v1'
def req = api.requirements()
assert req.available() && req.viewpointEnabled()
def prefix = 'FSAE2027:SD '
def stableId = { key -> java.util.UUID.nameUUIDFromBytes(('FSAE2027:SD:' + key).getBytes('UTF-8')).toString() }
// One immutable lookup snapshot before mutation; reserve new deterministic IDs locally.
def existingById = [:]
api.query { value -> value.eClass().getEStructuralFeature('id') != null }.each { value ->
    def id = api.id(value)
    if (id != null) {
        assert !existingById.containsKey(id) || existingById[id] == value: 'Duplicate native ID in acceptance model'
        existingById[id] = value
    }
}
def existingIds = existingById.keySet().toSet()
def element = { key ->
    def value = existingById[stableId(key)]
    assert value != null && value.eResource() != null && !value.eIsProxy(): 'Missing applied shutdown element ' + key
    value
}
assert api.find('Constraint', prefix + 'semantic repair v2 APPLIED') == null: 'Repair already applied; run the verifier instead'
assert api.find('Constraint', prefix + 'semantic structure v2 APPLIED') == null: 'Structure repair already applied; continue with phase 36'
def modules = req.listModules().collectEntries { [(it.ReqIFIdentifier):it] }
def sourceModule = modules['FSAE-2027-V1:source-module']
def derivedModule = modules['FSAE2027:SD:derived-module']
assert sourceModule != null && derivedModule != null
def sourceRequirements = req.requirements(sourceModule)
def derivedRequirements = req.requirements(derivedModule)
assert sourceRequirements.size() == 3197 && derivedRequirements.size() == 39
def vehicle = api.find('Entity', 'FSAE2027:Vehicle in operational context')
assert vehicle != null && vehicle.actor == false
def types = [SA:'SystemFunction', LA:'LogicalFunction', PA:'PhysicalFunction']
def glv = [:]
types.each { layer, type ->
    def base = api.find(type, 'FSAE2027:low-voltage Supply and manage low voltage electrical power')
    def flow = element('flow:' + layer + ':low-voltage:glv')
    assert base != null && flow.name == prefix + layer + ' Loss of GLV supply'
    assert flow.target.eContainer() == element('function:' + layer + ':glv')
    assert flow.source.eClass().name == 'FunctionOutputPort'
    assert flow.source.outgoingFunctionalExchanges.size() == 1 && flow.source.outgoingFunctionalExchanges[0] == flow
    // Accept the old aggregate or its integration-created descendant boundary leaf, never an unrelated source.
    def owner = flow.source.eContainer()
    while (owner != null && owner != base) owner = owner.eContainer()
    assert owner == base: 'GLV exchange source no longer belongs to its reviewed low-voltage domain'
    assert !existingIds.contains(stableId('function:' + layer + ':glv-supply'))
    glv[layer] = [base:base, flow:flow]
}
['accept','torque'].each { key ->
    assert !existingIds.contains(stableId('function:OA:' + key))
    assert element('function:SA:' + key).subFunctions.isEmpty()
}
def trip = derivedRequirements.find { it.ReqIFIdentifier == 'FSAE2027:SD:obligation:TRIP-CAUSES' }
assert trip != null && req.attributes(trip).verification_status == 'not_verified'
def tripSources = sourceRequirements.findAll { it.ReqIFIdentifier.startsWith('FSAE-2027-V1:EV.8.2.1:') }
assert !tripSources.isEmpty()
tripSources.each { source ->
    def attrs = req.attributes(source)
    assert attrs.source_sha256 == 'f71df5552e341dab1b58f1ff47212e7281953d66b9c41833de8347560913ed0e'
    assert attrs.coverage_status == 'allocated' && attrs.verification_status == 'not_verified'
}
def originalFlows = api.findAll('FunctionalExchange').findAll { it.name?.startsWith(prefix) }
assert originalFlows.size() == 78: 'Expected the original 26 shutdown flows across SA/LA/PA'
def originalFlowIds = originalFlows.collect { api.id(it) }.toSet()
def oaShutdown = api.find('OperationalActivity', 'FSAE2027:shutdown Prevent or interrupt unsafe tractive energization')
assert oaShutdown != null
def system = api.layer('SA').system
def identify = { value, key ->
    def id = stableId(key)
    assert !existingIds.contains(id): 'Refuse a partial or conflicting prior repair element ' + key
    assert api.set(value, 'id', id)
    existingIds.add(id)
    existingById[id] = value
    value
}
def appendSourceModel = { requirement, model ->
    if (requirement.ReqIFIdentifier?.startsWith('FSAE-2027-V1:')) {
        def text = req.attributes(requirement).model_element_ids
        def id = api.id(model)
        assert text != null && text.startsWith('[') && text.endsWith(']')
        assert id != null && !id.contains('"') && !id.contains('\\')
        if (!text.contains('"' + id + '"')) {
            def body = text.substring(1, text.length()-1).trim()
            req.setAttribute(requirement, 'model_element_ids', '[' + body + (body.isEmpty() ? '' : ',') + '"' + id + '"]')
        }
    }
}
def trace = { model, requirement ->
    assert req.attributes(requirement).verification_status == 'not_verified'
    req.satisfy(model, requirement)
    appendSourceModel(requirement, model)
    requirement.ownedRelations.findAll { it.eClass().name == 'CapellaIncomingRelation' && it.relationType?.ReqIFLongName == 'verify' }.each { relation ->
        def constraint = relation.target
        assert constraint.eClass().name == 'Constraint' && constraint.summary.startsWith('NOT_VERIFIED')
        if (!constraint.constrainedElements.contains(model)) constraint.constrainedElements.add(model)
    }
}
def result = [slice_id:'FSAE2027:shutdown-v2-structure-repair', functions:[:], operational_interfaces:[:],
    preserved_exchange_count:originalFlowIds.size(), exchange_realization_phase:'pending_36', physical_verification_status:'not_verified']
api.batch('Repair shutdown leaf endpoints and operational interfaces') {
    types.each { layer, type ->
        def function = identify(api.create(type, prefix + 'Observe GLV supply presence and loss', glv[layer].base), 'function:' + layer + ':glv-supply')
        api.allocate(function, layer == 'SA' ? system : element((layer == 'LA' ? 'logical:' : 'behavior:') + 'series'))
        function.summary = 'Candidate independent shutdown supply observation; exact supply sensing and physical verification remain TBD.'
        // Recontain the dedicated existing output port: exchange IDs, references and safety paths remain intact.
        function.outputs.add(glv[layer].flow.source)
        glv[layer].function = function
        trace(function, trip)
        tripSources.each { trace(function, it) }
        result.functions[layer] = api.id(function)
    }
    api.link(glv.LA.function, glv.SA.function, 'realization')
    api.link(glv.PA.function, glv.LA.function, 'realization')
    ['accept','torque'].each { key ->
        def systemFunction = element('function:SA:' + key)
        def activity = identify(api.create('OperationalActivity', systemFunction.name, oaShutdown), 'function:OA:' + key)
        api.allocate(activity, vehicle)
        activity.summary = 'Operational shutdown interface; physical implementation and verification remain pending.'
        api.link(systemFunction, activity, 'realization')
        systemFunction.ownedExtensions.findAll { it.eClass().name == 'CapellaOutgoingRelation' && it.relationType?.ReqIFLongName == 'satisfy' }
            .collect { it.target }.unique().each { trace(activity, it) }
        result.operational_interfaces[key] = api.id(activity)
    }
    def marker = identify(api.create('Constraint', prefix + 'semantic structure v2 APPLIED', api.layer('SA')), 'constraint:SEMANTIC-STRUCTURE-v2')
    marker.summary = 'Shutdown leaf endpoints repaired. Exchange/port realization phase 36 and physical verification remain pending; NOT_VERIFIED.'
    assert originalFlows.every { it.eResource() != null }
    assert originalFlowIds == originalFlows.collect { api.id(it) }.toSet()
    assert req.requirements(sourceModule).size() == sourceRequirements.size()
    assert req.requirements(derivedModule).size() == derivedRequirements.size()
}
return result

// Bounded second phase: add native exchange/port traces after the separate structure repair.
// No functions, endpoints, source requirements, coverage or verification statuses are changed.
assert session.sessionResource.URI.toString().toLowerCase().contains('/fsae-2027-electric-arcadia/')
assert api.layer('SA').system.summary == 'FSAE2027:bootstrap-v1'
def req = api.requirements()
assert req.available() && req.viewpointEnabled()
def prefix = 'FSAE2027:SD '
assert api.find('Constraint', prefix + 'semantic structure v2 APPLIED') != null: 'Apply phase 35 first'
assert api.find('Constraint', prefix + 'semantic repair v2 APPLIED') == null: 'Realization repair already applied; run the verifier'
def stableId = { key -> java.util.UUID.nameUUIDFromBytes(('FSAE2027:SD:' + key).getBytes('UTF-8')).toString() }
// One ID snapshot prevents an imported-requirements rescan for each of the 156 new native traces.
def existingIds = api.query { value -> value.eClass().getEStructuralFeature('id') != null }.collect { api.id(it) }.toSet()
def identify = { value, key ->
    def id = stableId(key)
    assert !existingIds.contains(id): 'Conflicting prior realization repair ID ' + key
    assert api.set(value, 'id', id)
    existingIds.add(id)
    value
}
def flows = api.findAll('FunctionalExchange').findAll { it.name?.startsWith(prefix) }
assert flows.size() == 78
def flowIndex = flows.collectEntries { [(it.name):it] }
assert flowIndex.size() == flows.size(): 'Ambiguous shutdown exchange names'
def pairs = []
flows.findAll { it.name.startsWith(prefix + 'SA ') }.each { upper ->
    def suffix = upper.name.substring((prefix + 'SA ').length())
    def middle = flowIndex[prefix + 'LA ' + suffix]
    def lower = flowIndex[prefix + 'PA ' + suffix]
    assert middle != null && lower != null
    pairs << [lower:middle,upper:upper,key:'repair:LA:' + api.id(upper)]
    pairs << [lower:lower,upper:middle,key:'repair:PA:' + api.id(upper)]
}
assert pairs.size() == 52
pairs.each { pair ->
    ['source','target'].each { side ->
        def from = pair.lower[side], to = pair.upper[side]
        assert from instanceof org.polarsys.capella.core.data.information.Port
        assert to instanceof org.polarsys.capella.core.data.information.Port
        assert from.eContainer().subFunctions.isEmpty() && to.eContainer().subFunctions.isEmpty()
        assert from.eContainer().ownedFunctionRealizations.any { it.targetElement == to.eContainer() }
    }
}
def originalIds = flows.collect { api.id(it) }.toSet()
def result = [slice_id:'FSAE2027:shutdown-v2-realization-repair', exchange_realizations_added:0,
    port_realizations_added:0, original_exchanges_retained:78, physical_verification_status:'not_verified']
api.batch('Add shutdown native functional exchange and port realizations') {
    pairs.each { pair ->
        def lower = pair.lower, upper = pair.upper, key = pair.key
        def existing = lower.ownedFunctionalExchangeRealizations.find { it.targetElement == upper }
        if (existing == null) {
            def relation = identify(org.polarsys.capella.core.data.fa.FaFactory.eINSTANCE.createFunctionalExchangeRealization(), 'exchange-realization:' + key)
            relation.sourceElement = lower; relation.targetElement = upper
            lower.ownedFunctionalExchangeRealizations.add(relation)
            result.exchange_realizations_added++
        } else {
            assert existing.sourceElement == lower
        }
        ['source','target'].each { side ->
            def from = lower[side], to = upper[side]
            def previous = from.ownedPortRealizations.find { it.targetElement == to }
            if (previous == null) {
                def relation = identify(org.polarsys.capella.core.data.information.InformationFactory.eINSTANCE.createPortRealization(), 'port-realization:' + key + ':' + side)
                relation.sourceElement = from; relation.targetElement = to
                from.ownedPortRealizations.add(relation)
                result.port_realizations_added++
            } else {
                assert previous.sourceElement == from
            }
        }
    }
    def marker = identify(api.create('Constraint', prefix + 'semantic repair v2 APPLIED', api.layer('SA')), 'constraint:SEMANTIC-REPAIR-v2')
    marker.summary = 'Native leaf endpoints and exchange/port realizations repaired. Shared bootstrap normalization and physical verification remain separate; NOT_VERIFIED.'
    assert flows.every { it.eResource() != null }
    assert originalIds == flows.collect { api.id(it) }.toSet()
}
return result

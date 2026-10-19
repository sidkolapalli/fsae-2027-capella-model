
// EMF tree iterators also inherit a List used internally as a traversal stack.
// Walk hasNext/next explicitly; Groovy collection extensions can inspect that stack.
def nativeDescendants = { element ->
    def values = new java.util.ArrayList()
    def iterator = element.eAllContents()
    while (iterator.hasNext()) values.add(iterator.next())
    values
}

assert session.sessionResource.URI.toString().toLowerCase().contains('/fsae-2027-electric-arcadia/')
def req = api.requirements()
assert req.available() && req.viewpointEnabled()
def system = api.layer('SA').system
assert system.summary == 'FSAE2027:bootstrap-v1'
assert system.ownedSystemComponents.isEmpty() && system.ownedFeatures.isEmpty(): 'SA vehicle must remain a black box'
def module = req.find('FSAE-2027-V1:source-module')
assert module != null && module.eContainer() == api.layer('SA')
def keys = ['structure','cockpit','brakes','driver-demand','powertrain','energy-storage','tractive-distribution',
    'shutdown','status','charging','low-voltage','thermal','aerodynamics','fasteners','running-gear']
def nodes = []
def edges = []
keys.each { key ->
    def logical = api.find('LogicalComponent','FSAE2027:'+key+' Logical subsystem')
    def behavior = api.find('PhysicalComponent','FSAE2027:'+key+' Physical behavior')
    def equipment = api.find('PhysicalComponent','FSAE2027:'+key+' Equipment')
    def ci = api.find('ConfigurationItem','FSAE2027:'+key+' Subsystem CI')
    assert logical != null && behavior != null && equipment != null && ci != null
    assert logical.eContainer() == api.layer('LA').system
    assert behavior.eContainer() == api.layer('PA').system && equipment.eContainer() == api.layer('PA').system
    assert behavior.nature == PhysicalComponentNature.BEHAVIOR && equipment.nature == PhysicalComponentNature.NODE
    assert ci.eContainer() == api.epbs().system
    def behaviorPart = api.layer('PA').system.ownedFeatures.find { it instanceof Part && it.abstractType == behavior }
    def equipmentPart = api.layer('PA').system.ownedFeatures.find { it instanceof Part && it.abstractType == equipment }
    assert behaviorPart != null && equipmentPart != null
    def deployment = equipmentPart.ownedDeploymentLinks.find { it.deployedElement == behaviorPart && it.location == equipmentPart }
    assert deployment != null: 'Every behavior needs its explicit native deployment'
    assert ci.ownedPhysicalArtifactRealizations.any { it.targetElement == behavior }
    assert ci.ownedPhysicalArtifactRealizations.any { it.targetElement == equipment }
    // Detailed slices decompose domain functions. Only leaves are allocated;
    // allocations may be on the domain owner or its specialized child components.
    [logical,behavior].each { owner ->
        def ownerTree = [owner]
        nativeDescendants(owner).each { if (it instanceof Component) ownerTree << it }
        def allocations = ownerTree.collectMany { it.ownedFunctionalAllocation }
        assert !allocations.isEmpty(): 'Domain needs allocated leaf responsibilities'
        assert allocations.every { it.targetElement.ownedFunctions.isEmpty() }: 'Non-leaf function allocation'
    }
    [logical,behavior,equipment,ci].each { element -> nodes << [id:api.id(element),name:element.name,type:element.eClass().name] }
    edges << [source:api.id(behavior),target:api.id(equipment),type:'native_part_deployment',id:api.id(deployment)]
    ci.ownedPhysicalArtifactRealizations.each { relation ->
        edges << [source:api.id(ci),target:api.id(relation.targetElement),type:relation.eClass().name,id:api.id(relation)]
    }
}
def requirements = req.requirements(module)
def identifiers = requirements.collect { it.ReqIFIdentifier }
assert identifiers.size() == identifiers.toSet().size(): 'Persisted clause identifiers must be unique'
def coverage = [:]
def interpretation = [:]
def applicability = [:]
def verification = [:]
requirements.each { requirement ->
    def attributes = req.attributes(requirement)
    [ [coverage,'coverage_status'], [interpretation,'interpretation_status'], [applicability,'applicability'], [verification,'verification_status'] ].each { pair ->
        def status = attributes[pair[1]] ?: 'missing'
        pair[0][status] = (pair[0][status] ?: 0) + 1
    }
    assert attributes.source_document_id == 'FSAE-2027-V1'
    assert attributes.source_sha256 != null
}
def decisions = api.layer('SA').ownedConstraints.findAll { it.name.startsWith('FSAE2027:DEC-') }
assert decisions.size() == 8
def unresolved = decisions.findAll { it.name.endsWith(' UNRESOLVED') }
// This phase verifies native structure and reports unresolved work; it never promotes coverage/compliance.
return [phase:'fsae-structure-verification',project:session.sessionResource.URI.toString(),
    completion_status:'in_progress',native_source_requirements:requirements.size(),coverage:coverage,
    interpretation:interpretation,applicability:applicability,verification:verification,
    unresolved_decisions:unresolved.collect { [id:api.id(it),name:it.name,rationale:it.summary] },
    expected_ledger_bijection:'Host runner must compare identifiers to clauses plus source_objects in rules/normalized-clauses.json',
    identifiers:identifiers,graph:[nodes:nodes,edges:edges]]

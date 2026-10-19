import org.eclipse.sirius.business.api.query.DRepresentationQuery
import org.eclipse.sirius.diagram.DDiagram
import org.eclipse.sirius.diagram.DEdge
import org.eclipse.sirius.viewpoint.DSemanticDecorator
import org.polarsys.capella.core.data.ctx.SystemFunction
import org.polarsys.capella.core.data.fa.FunctionalExchange
import org.polarsys.capella.core.data.fa.FunctionInputPort
import org.polarsys.capella.core.data.fa.FunctionOutputPort

// EMF tree iterators also inherit a List used internally as a traversal stack.
// Walk hasNext/next explicitly; Groovy collection extensions can inspect that stack.
def nativeDescendants = { element ->
    def values = new java.util.ArrayList()
    def iterator = element.eAllContents()
    while (iterator.hasNext()) values.add(iterator.next())
    values
}


// One bounded native diagram per request. Coverage below concerns diagram views only.
// Persisted source interpretation, design completeness, and physical verification remain unchanged.
assert session.sessionResource.URI.toString().toLowerCase().contains('/fsae-2027-electric-arcadia/')
assert api.layer('SA').system.summary == 'FSAE2027:bootstrap-v1'
def slice = 'vehicle-protection'
def exchangePrefix = 'FSAE2027:VP SA '
def title = 'FSAE 2027 SA occupant protection and braking detailed flows (candidate)'
def root = api.container('SA', 'root-function')
assert root instanceof SystemFunction && root.eClass().name == 'SystemFunction'
assert root.eResource() != null && !root.eIsProxy()

// Scope to native functional containment, avoiding repeated scans of the requirements inventory.
def exchanges = nativeDescendants(root).findAll { element ->
    element instanceof FunctionalExchange && element.eClass().name == 'FunctionalExchange' &&
        element.name?.startsWith(exchangePrefix)
}.sort { a, b -> a.name <=> b.name }
assert !exchanges.isEmpty(): 'Create and verify the ' + slice + ' semantic slice first'
assert exchanges.size() <= 120: 'Review unexpectedly large slice before creating a detailed diagram'
def functionsById = [:]
def exchangeRows = []
exchanges.each { exchange ->
    assert exchange.eResource() == root.eResource() && !exchange.eIsProxy()
    def sourcePort = exchange.source
    def targetPort = exchange.target
    assert sourcePort instanceof FunctionOutputPort && sourcePort.eClass().name == 'FunctionOutputPort':
        'Expected a native source output port for ' + exchange.name
    assert targetPort instanceof FunctionInputPort && targetPort.eClass().name == 'FunctionInputPort':
        'Expected a native target input port for ' + exchange.name
    assert [sourcePort, targetPort].every { it.eResource() == root.eResource() && !it.eIsProxy() }
    def sourceFunction = sourcePort.eContainer()
    def targetFunction = targetPort.eContainer()
    [sourceFunction, targetFunction].each { function ->
        assert function instanceof SystemFunction && function.eClass().name == 'SystemFunction':
            'The native exchange port must belong to a SystemFunction'
        assert function.eResource() == root.eResource() && !function.eIsProxy()
        assert function.ownedFunctions.isEmpty():
            'Normalize non-leaf exchange endpoint before displaying ' + function.name
        def ancestor = function
        while (ancestor != null && ancestor != root) ancestor = ancestor.eContainer()
        assert ancestor == root: 'Exchange endpoint is outside the active SA root'
        functionsById[api.id(function)] = function
    }
    exchangeRows << [id: api.id(exchange), name: exchange.name,
        source_function: api.id(sourceFunction), target_function: api.id(targetFunction),
        source_port: api.id(sourcePort), target_port: api.id(targetPort)]
}
def functions = functionsById.values().toList().sort { a, b -> a.name <=> b.name }
assert functions.size() <= 120: 'Review unexpectedly large endpoint set before diagram creation'
def existing = api.diagram.list().findAll { it.name == title }
assert existing.size() <= 1: 'Ambiguous existing safety diagram title'
def diagram = existing.isEmpty() ? null : existing[0]
if (diagram != null) {
    assert diagram instanceof DDiagram && diagram.target == root
    assert diagram.description.name == 'System Data Flow Blank':
        'Existing title belongs to a different native diagram descriptor'
}

def created = diagram == null
def added = []
def started = java.time.Instant.now()
def layoutMillis = null
api.batch('Display native ' + slice + ' SA safety flows') {
    if (diagram == null) diagram = api.diagram.createSafe('SDFB', title, root)
    assert diagram instanceof DDiagram && diagram.target == root
    def alreadyDisplayed = nativeDescendants(diagram).findAll { it instanceof DSemanticDecorator && it.target != null }
        .collect { it.target }.toSet()
    functions.each { function ->
        if (!alreadyDisplayed.contains(function)) {
            assert api.diagram.add(diagram, function): 'Cannot display native leaf ' + function.name
            added << api.id(function)
        }
    }
    // Native Sirius mappings create the exchange edges once their leaf endpoints are displayed.
    api.diagram.refresh(diagram)
    def layoutStarted = java.time.Instant.now()
    assert api.diagram.layout(diagram, 'layered')
    layoutMillis = java.time.Duration.between(layoutStarted, java.time.Instant.now()).toMillis()
    if (layoutMillis > 3000L) log.warn('Safety diagram layout exceeded 3 s: ' + title + ' (' + layoutMillis + ' ms)')
}
def decorators = nativeDescendants(diagram).findAll { it instanceof DSemanticDecorator }
assert decorators.every { it.target != null && it.target.eResource() != null && !it.target.eIsProxy() }:
    'Native diagram contains a detached or unresolved semantic view'
assert functions.every { function -> decorators.any { it.target == function } }:
    'Native diagram omitted a requested leaf endpoint'
def descriptor = new DRepresentationQuery(diagram).representationDescriptor
assert descriptor != null && descriptor.name == title && diagram.name == title

def edges = decorators.findAll { it instanceof DEdge }
def missing = []
def hidden = []
exchangeRows.each { row ->
    def matching = edges.findAll { api.id(it.target) == row.id }
    row.native_edge_views = matching.size()
    row.visible_native_edge_views = matching.count { it.isVisible() }
    if (matching.isEmpty()) missing << [id: row.id, name: row.name]
    else if (row.visible_native_edge_views == 0) hidden << [id: row.id, name: row.name]
}
def sliceIds = exchangeRows.collect { it.id }.toSet()
def otherEdges = edges.findAll { !sliceIds.contains(api.id(it.target)) }
def externalFunctions = functions.findAll { !it.name?.startsWith('FSAE2027:VP ') }
def visibleSliceExchanges = exchangeRows.count { it.visible_native_edge_views > 0 }
def missingIds = missing.collect { it.id }
log.info('Native ' + slice + ' detailed SA diagram: ' + functions.size() + ' leaf endpoints, ' +
    exchanges.size() + ' semantic exchanges, ' + visibleSliceExchanges + ' exchanges with visible native edges')
if (!missing.isEmpty()) log.warn('Missing native safety exchange views: ' + missing)
if (!hidden.isEmpty()) log.warn('Hidden native safety exchange views: ' + hidden)
return [phase: 'fsae-safety-flow-diagram', slice: slice, name: title, uid: diagram.uid,
    descriptor: diagram.description.name, target: api.id(root), created: created, added_function_ids: added,
    displayed_functions: functions.collect { [id: api.id(it), name: it.name] },
    external_endpoint_functions: externalFunctions.collect { [id: api.id(it), name: it.name] },
    semantic_exchange_count: exchanges.size(), native_edge_view_count: edges.size(),
    exchanges_with_native_edge_views: exchangeRows.count { it.native_edge_views > 0 },
    exchanges_with_visible_native_edge_views: visibleSliceExchanges,
    missing_exchange_views: missing, hidden_exchange_views: hidden,
    additional_native_edge_views: otherEdges.collect { [id: api.id(it.target), name: it.target.name, visible: it.isVisible()] },
    exchange_view_details: exchangeRows, layout_millis: layoutMillis,
    elapsed_millis: java.time.Duration.between(started, java.time.Instant.now()).toMillis(),
    diagram_view_coverage_status: missingIds.isEmpty() && hidden.isEmpty() ? 'all_slice_exchange_views_visible' : 'requires_diagram_review',
    interpretation_status: 'needs_semantic_review', design_status: 'candidate',
    physical_verification_status: 'not_verified', source_requirement_statuses_changed: false]

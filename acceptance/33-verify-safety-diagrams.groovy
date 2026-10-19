import org.eclipse.sirius.business.api.query.DRepresentationQuery
import org.eclipse.sirius.diagram.DDiagram
import org.eclipse.sirius.diagram.DEdge
import org.eclipse.sirius.viewpoint.DSemanticDecorator
import org.polarsys.capella.core.data.fa.FunctionalExchange

// EMF tree iterators also inherit a List used internally as a traversal stack.
// Walk hasNext/next explicitly; Groovy collection extensions can inspect that stack.
def nativeDescendants = { element ->
    def values = new java.util.ArrayList()
    def iterator = element.eAllContents()
    while (iterator.hasNext()) values.add(iterator.next())
    values
}


// Read-only persistence check; count/view coverage is not engineering verification.
assert session.sessionResource.URI.toString().contains('/FSAE-2027-Electric-Arcadia/')
def root = api.container('SA', 'root-function')
def definitions = [
    [slice:'shutdown', prefix:'FSAE2027:SD SA ', count:26,
        title:'FSAE 2027 SA shutdown protection detailed flows (candidate)'],
    [slice:'driver-demand', prefix:'FSAE2027:DD SA ', count:22,
        title:'FSAE 2027 SA driver demand and BSPD detailed flows (candidate)'],
    [slice:'vehicle-protection', prefix:'FSAE2027:VP SA ', count:15,
        title:'FSAE 2027 SA occupant protection and braking detailed flows (candidate)']
]
def contents = nativeDescendants(root)
def rows = definitions.collect { definition ->
    def matches = api.diagram.list().findAll { it.name == definition.title }
    assert matches.size() == 1: 'Exactly one persisted diagram required: '+definition.title
    def diagram = matches[0]
    assert diagram instanceof DDiagram && diagram.target == root
    assert diagram.description.name == 'System Data Flow Blank'
    assert new DRepresentationQuery(diagram).representationDescriptor.name == definition.title
    def views = nativeDescendants(diagram).findAll { it instanceof DSemanticDecorator }
    assert views.every { it.target != null && it.target.eResource() != null && !it.target.eIsProxy() }
    def exchanges = contents.findAll { it instanceof FunctionalExchange && it.name?.startsWith(definition.prefix) }
    assert exchanges.size() == definition.count: 'Persisted slice exchange count changed: '+definition.slice
    def edges = views.findAll { it instanceof DEdge }
    exchanges.each { exchange ->
        assert edges.any { it.target == exchange && it.isVisible() }: 'Missing visible native exchange '+exchange.name
        [exchange.source.eContainer(),exchange.target.eContainer()].each { function ->
            assert function.ownedFunctions.isEmpty()
            assert views.any { it.target == function }: 'Missing persisted leaf endpoint '+function.name
        }
    }
    [slice:definition.slice,name:diagram.name,uid:diagram.uid,
        semantic_exchanges:exchanges.size(),all_native_exchange_views_visible:true]
}
return [phase:'fsae-safety-diagram-persistence-verification',diagrams:rows,
    diagram_view_coverage_status:'all_63_slice_exchange_views_visible',
    engineering_verification_status:'not_verified',completion_status:'in_progress']

import org.eclipse.sirius.business.api.query.DRepresentationQuery
import org.eclipse.sirius.diagram.DEdge
import org.eclipse.sirius.viewpoint.DSemanticDecorator

// EMF tree iterators also inherit a List used internally as a traversal stack.
// Walk hasNext/next explicitly; Groovy collection extensions can inspect that stack.
def nativeDescendants = { element ->
    def values = new java.util.ArrayList()
    def iterator = element.eAllContents()
    while (iterator.hasNext()) values.add(iterator.next())
    values
}


assert session.sessionResource.URI.toString().toLowerCase().contains('/fsae-2027-electric-arcadia/')
assert api.layer('SA').system.summary == 'FSAE2027:bootstrap-v1'
def definitions = [
    [layer:'OA',kind:'architecture',title:'OA operational context (candidate)',minimum:5],
    [layer:'OA',kind:'flow',title:'OA activities and interactions (candidate)',minimum:20],
    [layer:'SA',kind:'architecture',title:'SA vehicle and external actors (candidate)',minimum:5],
    [layer:'SA',kind:'flow',title:'SA vehicle functions and flows (candidate)',minimum:15],
    [layer:'LA',kind:'architecture',title:'LA logical subsystems (candidate)',minimum:15],
    [layer:'LA',kind:'flow',title:'LA logical functions and flows (candidate)',minimum:15],
    [layer:'PA',kind:'architecture',title:'PA equipment and deployed behavior (candidate)',minimum:30],
    [layer:'PA',kind:'flow',title:'PA physical functions and flows (candidate)',minimum:15],
    [layer:'EPBS',kind:'architecture',title:'EPBS subsystem configuration items (candidate)',minimum:15]
]
def rows = []
definitions.each { definition ->
    def layer = definition.layer
    def flow = definition.kind == 'flow'
    def diagram = api.diagram.find('FSAE 2027 '+definition.title)
    assert diagram != null: 'Missing native diagram '+definition.title
    def root = flow ? api.container(layer,'root-function') :
        (layer == 'OA' ? api.container('OA','component') : api.layer(layer).system)
    assert diagram.target == root
    def descriptor = new DRepresentationQuery(diagram).representationDescriptor
    assert descriptor != null && descriptor.name == diagram.name
    def decorators = nativeDescendants(diagram).findAll { it instanceof DSemanticDecorator }
    decorators.each { view ->
        assert view.target != null && view.target.eResource() != null && !view.target.eIsProxy():
            'Every native diagram view must retain a persisted semantic target'
    }
    def semanticElements = decorators.collect { it.target }.unique()
    def candidates = semanticElements.findAll { element ->
        def component = element instanceof Part ? element.abstractType : element
        component?.name?.startsWith('FSAE2027:') &&
            (flow ? component instanceof AbstractFunction : component instanceof Component)
    }
    assert candidates.size() >= definition.minimum:
        'Diagram '+diagram.name+' contains '+candidates.size()+' candidate elements; expected at least '+definition.minimum
    def edges = decorators.findAll { it instanceof DEdge }
    def expectedExchanges = flow ? nativeDescendants(api.layer(layer)).findAll {
        it instanceof FunctionalExchange && it.name?.startsWith('FSAE2027:'+layer+' ')
    } : []
    def missing = expectedExchanges.findAll { exchange -> !edges.any { it.target == exchange } }
    rows << [layer:layer,kind:definition.kind,uid:diagram.uid,name:diagram.name,descriptor:diagram.description.name,
        target:api.id(root),candidate_elements:candidates.size(),visible_edges:edges.size(),
        expected_exchanges:expectedExchanges.size(),missing_exchange_views:missing.collect { api.id(it) },
        candidate_status:'unreviewed']
    log.info('Verified persisted native '+layer+' '+definition.kind+' diagram: '+candidates.size()+
        ' candidate elements, '+edges.size()+' edges; missing exchange views '+missing.size())
}
return [phase:'fsae-diagrams-verify',native_diagrams:rows.size(),completion_status:'in_progress',
    exchange_views_complete:rows.every { it.missing_exchange_views.isEmpty() },diagrams:rows]

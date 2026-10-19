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


// Candidate diagrams display the persisted native model; they do not assert source-rule compliance.
assert session.sessionResource.URI.toString().toLowerCase().contains('/fsae-2027-electric-arcadia/')
assert api.layer('SA').system.summary == 'FSAE2027:bootstrap-v1'
def prefix = 'FSAE 2027 '
def definitions = [
    [layer:'OA',kind:'architecture',descriptor:'OAB',title:'OA operational context (candidate)'],
    [layer:'OA',kind:'flow',descriptor:'OAIB',title:'OA activities and interactions (candidate)'],
    [layer:'SA',kind:'architecture',descriptor:'SAB',title:'SA vehicle and external actors (candidate)'],
    [layer:'SA',kind:'flow',descriptor:'SDFB',title:'SA vehicle functions and flows (candidate)'],
    [layer:'LA',kind:'architecture',descriptor:'LAB',title:'LA logical subsystems (candidate)'],
    [layer:'LA',kind:'flow',descriptor:'LDFB',title:'LA logical functions and flows (candidate)'],
    [layer:'PA',kind:'architecture',descriptor:'PAB',title:'PA equipment and deployed behavior (candidate)'],
    [layer:'PA',kind:'flow',descriptor:'PDFB',title:'PA physical functions and flows (candidate)'],
    [layer:'EPBS',kind:'architecture',descriptor:'EPBS Architecture Blank',title:'EPBS subsystem configuration items (candidate)']
]
def existingDiagramNames = api.diagram.list().collect { it.name }.toSet()
assert definitions.every { definition -> !existingDiagramNames.contains(prefix+definition.title) }:
    'Candidate diagrams already exist. Verify the persisted diagrams before creating replacements.'
def result = []
api.batch('Create native FSAE candidate architecture and flow diagrams') {
    definitions.each { definition ->
        def layer = definition.layer
        def flow = definition.kind == 'flow'
        def architecture = api.layer(layer)
        def root = flow ? api.container(layer,'root-function') :
            (layer == 'OA' ? api.container('OA','component') : architecture.system)
        assert root != null
        def diagram = api.diagram.createSafe(definition.descriptor,prefix+definition.title,root)
        assert diagram != null && diagram.target == root:
            'Native diagram target must preserve the requested '+layer+' semantic root'
        def elements = []
        if (flow) {
            elements = root.ownedFunctions.findAll { it.name?.startsWith('FSAE2027:') }
            assert elements.size() >= 15
        } else if (layer == 'OA') {
            elements = api.container('OA','component').ownedEntities.findAll { it.name?.startsWith('FSAE2027:') }
            assert elements.size() >= 5
        } else if (layer == 'SA') {
            elements = [root] + api.container('SA','component').ownedSystemComponents.findAll {
                it != root && it.name?.startsWith('FSAE2027:')
            }
            assert root.ownedSystemComponents.isEmpty(): 'The vehicle system remains a black box'
            assert elements.size() >= 5
        } else if (layer == 'LA') {
            elements = root.ownedLogicalComponents.findAll { it.name?.startsWith('FSAE2027:') } +
                api.container('LA','component').ownedLogicalComponents.findAll { it.actor && it.name?.startsWith('FSAE2027:') }
            assert elements.size() >= 15
        } else if (layer == 'PA') {
            // Display nodes first so deployed behavior views can appear inside equipment containers.
            def children = root.ownedPhysicalComponents.findAll { it.name?.startsWith('FSAE2027:') }
            elements = children.findAll { it.nature == PhysicalComponentNature.NODE } +
                children.findAll { it.nature == PhysicalComponentNature.BEHAVIOR } +
                api.container('PA','component').ownedPhysicalComponents.findAll { it.actor && it.name?.startsWith('FSAE2027:') }
            assert children.size() >= 30
        } else {
            elements = root.ownedConfigurationItems.findAll { it.name?.startsWith('FSAE2027:') }
            assert elements.size() >= 15
        }
        def displayed = []
        elements.each { element ->
            def target = flow || layer == 'OA' ? element : api.ensurePart(element)
            assert target != null && target.eResource() != null
            assert target.eContainingFeature().name != 'ownedExtensions'
            if (!nativeDescendants(diagram).any { it instanceof DSemanticDecorator && it.target == target }) {
                assert api.diagram.add(diagram,target): 'Failed to display native '+layer+' element '+element.name
            }
            displayed << [element:api.id(element),target:api.id(target),name:element.name]
        }
        api.diagram.refresh(diagram)
        assert api.diagram.layout(diagram,flow ? 'layered' : 'grid')
        def descriptor = new DRepresentationQuery(diagram).representationDescriptor
        assert descriptor != null && descriptor.name == prefix+definition.title && diagram.name == descriptor.name
        def decorators = nativeDescendants(diagram).findAll { it instanceof DSemanticDecorator }
        displayed.each { row ->
            def views = decorators.findAll { view -> api.id(view.target) == row.target ||
                (view.target instanceof Part && api.id(view.target.abstractType) == row.element) }
            assert !views.isEmpty(): 'Native diagram must display the requested semantic element '+row.name
            row.native_view_targets = views.collect { api.id(it.target) }.unique()
        }
        decorators.each { view ->
            assert view.target != null && view.target.eResource() != null && !view.target.eIsProxy()
        }
        def edges = decorators.findAll { it instanceof DEdge }
        def expectedExchanges = flow ? nativeDescendants(architecture).findAll {
            it instanceof FunctionalExchange && it.name?.startsWith('FSAE2027:'+layer+' ')
        } : []
        def missingExchangeViews = expectedExchanges.findAll { exchange -> !edges.any { it.target == exchange } }
        def row = [layer:layer,kind:definition.kind,uid:diagram.uid,name:diagram.name,descriptor:diagram.description.name,
            target:api.id(root),displayed:displayed,semantic_views:decorators.size(),visible_edges:edges.size(),
            expected_exchanges:expectedExchanges.size(),missing_exchange_views:missingExchangeViews.collect { api.id(it) },
            candidate_status:'unreviewed',source_coverage_status:'pending']
        result << row
        log.info('FSAE native diagram '+layer+' '+definition.kind+': displayed '+displayed.size()+
            ' semantic elements, '+edges.size()+' visible edges, '+missingExchangeViews.size()+' exchange views requiring review')
        if (!missingExchangeViews.isEmpty()) {
            log.warn('Native exchange view review '+diagram.name+': '+missingExchangeViews.collect { it.name })
        }
    }
}
return [phase:'fsae-diagrams',completion_status:'in_progress',diagrams:result]

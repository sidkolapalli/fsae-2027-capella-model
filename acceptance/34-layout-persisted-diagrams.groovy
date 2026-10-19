import org.eclipse.gmf.runtime.notation.Diagram
import org.eclipse.gmf.runtime.notation.Node
import org.eclipse.gmf.runtime.notation.Bounds
import org.eclipse.sirius.diagram.DNode
import org.eclipse.sirius.diagram.DNodeContainer
import org.eclipse.sirius.diagram.DNodeList

assert session.sessionResource.URI.toString().contains('/FSAE-2027-Electric-Arcadia/')
def diagrams = api.diagram.list().findAll { it.name.startsWith('FSAE 2027 ') }
assert diagrams.size() == 12
def walk = { element ->
    def contents = new java.util.ArrayList()
    def iterator = element.eAllContents()
    while (iterator.hasNext()) contents.add(iterator.next())
    contents
}
def notationOf = { representation ->
    def matches = walk(representation).findAll { it instanceof Diagram && it.element == representation }
    assert matches.size() == 1: 'Exactly one persisted GMF diagram required for '+representation.name
    matches[0]
}
def isShape = { node ->
    node instanceof Node && (node.element instanceof DNode || node.element instanceof DNodeContainer || node.element instanceof DNodeList)
}
def outerShapes = { notation ->
    walk(notation).findAll { node ->
        if (!isShape(node)) return false
        def owner = node.eContainer()
        while (owner != null && owner != notation) {
            if (isShape(owner)) return false
            owner = owner.eContainer()
        }
        owner == notation
    }
}
def coordinates = { notation ->
    outerShapes(notation).collect { node ->
        assert node.layoutConstraint instanceof Bounds
        def b = node.layoutConstraint
        [id:node.element.uid,x:b.x,y:b.y,width:b.width,height:b.height]
    }
}
def rows = []
diagrams.sort { it.name }.each { representation ->
    def notation = notationOf(representation)
    def others = diagrams.findAll { it != representation }.collectEntries { [(it.uid):coordinates(notationOf(it))] }
    def strategy = representation.description.name.contains('Data Flow') || representation.description.name.contains('Interaction') ? 'layered' : 'grid'
    assert api.diagram.layout(representation,strategy)
    others.each { uid,before ->
        def other = diagrams.find { it.uid == uid }
        assert coordinates(notationOf(other)) == before: 'Layout changed an unrelated diagram'
    }
    def positions = coordinates(notation)
    assert !positions.isEmpty()
    def distinctX = positions.collect { it.x }.toSet().size()
    def distinctY = positions.collect { it.y }.toSet().size()
    if (positions.size() > 1) assert distinctX > 1: 'Requested layout did not create multiple native columns'
    for (int i=0; i<positions.size(); i++) {
        for (int j=i+1; j<positions.size(); j++) {
            def a=positions[i], b=positions[j]
            assert a.x+Math.max(a.width,1)<=b.x || b.x+Math.max(b.width,1)<=a.x ||
                a.y+Math.max(a.height,1)<=b.y || b.y+Math.max(b.height,1)<=a.y:
                'Persisted outer shape bounds overlap in '+representation.name
        }
    }
    rows << [name:representation.name,uid:representation.uid,strategy:strategy,
        outer_shapes:positions.size(),distinct_columns:distinctX,distinct_rows:distinctY,
        unrelated_diagrams_unchanged:true,overlapping_outer_shapes:0,positions:positions]
}
return [phase:'native-persisted-diagram-layout',diagrams:rows,checked_diagrams:rows.size(),
    engineering_verification_status:'not_verified',completion_status:'in_progress']

// Additive, repeatable normalization after detailed slices decompose domain functions.
// These interface responsibilities remain candidates; they do not establish compliance.
assert session.sessionResource.URI.toString().contains('/FSAE-2027-Electric-Arcadia/')
assert api.layer('SA').system.summary == 'FSAE2027:bootstrap-v1'
def types = [OA:'OperationalActivity', SA:'SystemFunction', LA:'LogicalFunction', PA:'PhysicalFunction']
def interfaces = [
    ['driver-demand','powertrain','Validated torque demand'],
    ['energy-storage','tractive-distribution','Tractive electrical energy'],
    ['tractive-distribution','powertrain','Motor electrical power'],
    ['energy-storage','shutdown','Battery safety status'],
    ['driver-demand','shutdown','Demand plausibility status'],
    ['brakes','shutdown','Brake safety status'],
    ['shutdown','tractive-distribution','Energization authorization'],
    ['shutdown','status','Tractive and readiness status'],
    ['charging','energy-storage','Charging energy and authorization'],
    ['thermal','energy-storage','Thermal condition'],
    ['low-voltage','shutdown','Low voltage supply']
]
def layers = ['OA','SA','LA','PA']
def fa = org.polarsys.capella.core.data.fa.FaFactory.eINSTANCE
def information = org.polarsys.capella.core.data.information.InformationFactory.eINSTANCE
def report = [phase:'normalize-domain-interface-leaves', created_leaves:[], moved_endpoints:[], removed_parent_allocations:[], realizations:[]]
def componentFor = { layer, domain ->
    if (layer == 'OA') return api.find('Entity','FSAE2027:Vehicle in operational context')
    if (layer == 'SA') return api.layer('SA').system
    if (layer == 'LA') return api.find('LogicalComponent','FSAE2027:'+domain+' Logical subsystem')
    return api.find('PhysicalComponent','FSAE2027:'+domain+' Physical behavior')
}
def endpointFunction = { endpoint -> endpoint instanceof AbstractFunction ? endpoint : endpoint.eContainer() }
def traceExchange = { lower, upper ->
    if (!lower.ownedFunctionalExchangeRealizations.any { it.targetElement == upper }) {
        def trace = fa.createFunctionalExchangeRealization()
        trace.sourceElement = lower
        trace.targetElement = upper
        lower.ownedFunctionalExchangeRealizations.add(trace)
        report.realizations << api.id(trace)
    }
    if (!(upper.source instanceof AbstractFunction)) {
        [[lower.source,upper.source],[lower.target,upper.target]].each { ports ->
            if (!ports[0].ownedPortRealizations.any { it.targetElement == ports[1] }) {
                def trace = information.createPortRealization()
                trace.sourceElement = ports[0]
                trace.targetElement = ports[1]
                ports[0].ownedPortRealizations.add(trace)
                report.realizations << api.id(trace)
            }
        }
    }
}
api.batch('Normalize candidate interface leaves and cross-layer exchange traces') {
    interfaces.each { candidate ->
        def exchangeByLayer = [:]
        def leafByLayer = [:]
        layers.each { layer ->
            def exchange = api.find('FunctionalExchange','FSAE2027:'+layer+' '+candidate[2])
            assert exchange != null: 'Missing original interface '+candidate[2]+' in '+layer
            exchangeByLayer[layer] = exchange
            leafByLayer[layer] = []
            ['source','target'].eachWithIndex { side, index ->
                def domain = candidate[index]
                def name = 'FSAE2027:boundary '+domain+' '+(side == 'source' ? 'Provide ' : 'Accept ')+candidate[2]
                def existing = api.find(types[layer],name)
                def original = endpointFunction(exchange[side])
                def parent = existing != null ? existing.eContainer() : original
                assert parent instanceof AbstractFunction
                assert parent.name.startsWith('FSAE2027:'+domain+' '): 'Unexpected candidate interface ownership'
                def leaf = existing ?: (parent.ownedFunctions.isEmpty() ? parent : null)
                if (leaf == null) {
                    leaf = api.create(types[layer],name,parent)
                    api.set(leaf,'summary','Candidate interface boundary responsibility. Detailed safety behavior, conditions and implementation remain separately reviewed; this leaf carries the preserved subsystem interface.')
                    api.allocate(leaf,componentFor(layer,domain))
                    report.created_leaves << [id:api.id(leaf),layer:layer,name:name]
                }
                assert leaf.ownedFunctions.isEmpty()
                assert leaf.componentFunctionalAllocations.size() == 1
                def endpoint = exchange[side]
                if (layer == 'OA') exchange[side] = leaf
                else if (endpoint.eContainer() != leaf) {
                    (side == 'source' ? leaf.outputs : leaf.inputs).add(endpoint)
                    api.rename(endpoint,(side == 'source' ? 'Provide ' : 'Accept ')+candidate[2])
                }
                leafByLayer[layer] << leaf
                report.moved_endpoints << [exchange:api.id(exchange),side:side,function:api.id(leaf)]
            }
        }
        [['SA','OA'],['LA','SA'],['PA','LA']].each { pair ->
            [0,1].each { index ->
                def lower = leafByLayer[pair[0]][index]
                def upper = leafByLayer[pair[1]][index]
                if (!lower.ownedFunctionRealizations.any { it.targetElement == upper }) api.link(lower,upper,'FunctionRealization')
            }
            traceExchange(exchangeByLayer[pair[0]],exchangeByLayer[pair[1]])
        }
        def laTransport = api.find('ComponentExchange','FSAE2027:LA '+candidate[2]+' transport')
        def paTransport = api.find('ComponentExchange','FSAE2027:PA '+candidate[2]+' transport')
        assert laTransport != null && paTransport != null
        if (!paTransport.ownedComponentExchangeRealizations.any { it.targetElement == laTransport }) {
            def trace = fa.createComponentExchangeRealization()
            trace.sourceElement = paTransport
            trace.targetElement = laTransport
            paTransport.ownedComponentExchangeRealizations.add(trace)
            report.realizations << api.id(trace)
        }
    }
    types.each { layer, type ->
        api.findAll(type).findAll { it.name?.startsWith('FSAE2027:') && !it.ownedFunctions.isEmpty() }.each { parent ->
            parent.componentFunctionalAllocations.toList().each { allocation ->
                report.removed_parent_allocations << [id:api.id(allocation),function:api.id(parent),layer:layer]
                assert api.delete(allocation)
            }
        }
    }
}
return report

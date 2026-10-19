// Repeatable native exchange/port realization and transport allocation repair, protection scope only.
// Design allocation does not constitute physical verification or compliance.
assert session.sessionResource.URI.toString().toLowerCase().contains('/fsae-2027-electric-arcadia/')
assert api.layer('SA').system.summary == 'FSAE2027:bootstrap-v1'
assert api.requirements().find('FSAE2027:VP:derived-module') != null
def stableId = { key -> java.util.UUID.nameUUIDFromBytes(('FSAE2027:VP:' + key).getBytes('UTF-8')).toString() }
def element = { key ->
    def value = api.findById(stableId(key))
    assert value != null && value.eResource() != null && !value.eIsProxy(): 'Missing protection structure ' + key
    value
}
def owners = [loadpath:'loadpath',rollover:'rollover',side:'side',front:'front',accommodate:'accommodate',barrier:'barrier',
    restrain:'restrain',head:'head',pedal:'pedal','brake-demand':'pedal','hydraulic-a':'hydraulic-a','hydraulic-b':'hydraulic-b',
    friction:'friction','protect-brakes':'friction',overtravel:'overtravel','brake-light':'brake-light',inspect:'accommodate']
def routes = [['pedal','brake-demand'],['brake-demand','hydraulic-a'],['brake-demand','hydraulic-b'],['hydraulic-a','friction'],['hydraulic-b','friction'],
    ['brake-demand','brake-light'],['pedal','overtravel'],['accommodate','restrain'],['restrain','rollover'],['restrain','head'],
    ['front','loadpath'],['side','loadpath'],['rollover','loadpath'],['protect-brakes','friction']]
// Resolve everything before mutation, so the facade's index is built only once.
def flows = routes.collectEntries { pair ->
    [(pair.join(':')):['OA','SA','LA','PA'].collectEntries { layer ->
        [(layer):element('flow:' + layer + ':' + pair.join(':'))]
    }]
}
def transports = routes.findAll { owners[it[0]] != owners[it[1]] }.collectEntries { pair ->
    [(pair.join(':')):['LA','PA'].collectEntries { layer -> [(layer):element('transport:' + layer + ':' + pair.join(':'))] }]
}
def bots = ['SA','LA','PA'].collectEntries { [(it):element('bots-trip:' + it)] }
def fa = org.polarsys.capella.core.data.fa.FaFactory.eINSTANCE
def information = org.polarsys.capella.core.data.information.InformationFactory.eINSTANCE
def report = [phase:'realize-protection-exchanges',created_functional_exchange_realizations:0,created_port_realizations:0,
    created_component_exchange_realizations:0,created_transport_allocations:0,created_port_allocations:0,
    verification_status:'not_verified',design_status:'unresolved']
def portRealization = { key, lower, upper ->
    assert lower instanceof org.polarsys.capella.core.data.information.Port
    assert upper instanceof org.polarsys.capella.core.data.information.Port
    if (!lower.ownedPortRealizations.any { it.sourceElement == lower && it.targetElement == upper }) {
        def trace = information.createPortRealization()
        trace.id = stableId('repair:port-realization:' + key)
        trace.sourceElement = lower
        trace.targetElement = upper
        lower.ownedPortRealizations.add(trace)
        report.created_port_realizations++
    }
}
def flowRealization = { key, lower, upper ->
    assert lower.eClass().name == 'FunctionalExchange' && upper.eClass().name == 'FunctionalExchange'
    if (!lower.ownedFunctionalExchangeRealizations.any { it.sourceElement == lower && it.targetElement == upper }) {
        def trace = fa.createFunctionalExchangeRealization()
        trace.id = stableId('repair:flow-realization:' + key)
        trace.sourceElement = lower
        trace.targetElement = upper
        lower.ownedFunctionalExchangeRealizations.add(trace)
        report.created_functional_exchange_realizations++
    }
    // OA flows have activities as direct endpoints; there are no OA function ports to realize.
    if (!(upper.source instanceof AbstractFunction)) {
        assert lower.source instanceof FunctionOutputPort && upper.source instanceof FunctionOutputPort
        assert lower.target instanceof FunctionInputPort && upper.target instanceof FunctionInputPort
        portRealization(key + ':source', lower.source, upper.source)
        portRealization(key + ':target', lower.target, upper.target)
    }
}
def portAllocation = { key, componentPort, functionPort ->
    assert componentPort instanceof ComponentPort && functionPort instanceof FunctionPort
    assert functionPort.eContainer().componentFunctionalAllocations.any { it.sourceElement == componentPort.eContainer() }
    if (!componentPort.ownedPortAllocations.any { it.sourceElement == componentPort && it.targetElement == functionPort }) {
        def allocation = information.createPortAllocation()
        allocation.id = stableId('repair:port-allocation:' + key)
        allocation.sourceElement = componentPort
        allocation.targetElement = functionPort
        componentPort.ownedPortAllocations.add(allocation)
        report.created_port_allocations++
    }
}
api.batch('Realize and allocate bounded protection function and component interfaces') {
    flows.each { route, byLayer ->
        [['SA','OA'],['LA','SA'],['PA','LA']].each { pair ->
            flowRealization(route + ':' + pair[0], byLayer[pair[0]], byLayer[pair[1]])
        }
    }
    [['LA','SA'],['PA','LA']].each { pair -> flowRealization('BOTS:' + pair[0], bots[pair[0]], bots[pair[1]]) }
    transports.each { route, byLayer ->
        def lower = byLayer.PA
        def upper = byLayer.LA
        assert lower.sourcePort instanceof ComponentPort && upper.sourcePort instanceof ComponentPort
        assert lower.targetPort instanceof ComponentPort && upper.targetPort instanceof ComponentPort
        if (!lower.ownedComponentExchangeRealizations.any { it.sourceElement == lower && it.targetElement == upper }) {
            def trace = fa.createComponentExchangeRealization()
            trace.id = stableId('repair:transport-realization:' + route)
            trace.sourceElement = lower
            trace.targetElement = upper
            lower.ownedComponentExchangeRealizations.add(trace)
            report.created_component_exchange_realizations++
        }
        portRealization('transport:' + route + ':source', lower.sourcePort, upper.sourcePort)
        portRealization('transport:' + route + ':target', lower.targetPort, upper.targetPort)
        ['LA','PA'].each { layer ->
            def transport = byLayer[layer]
            def flow = flows[route][layer]
            if (!transport.ownedComponentExchangeFunctionalExchangeAllocations.any { it.sourceElement == transport && it.targetElement == flow }) {
                def allocation = fa.createComponentExchangeFunctionalExchangeAllocation()
                allocation.id = stableId('repair:transport-allocation:' + layer + ':' + route)
                allocation.sourceElement = transport
                allocation.targetElement = flow
                transport.ownedComponentExchangeFunctionalExchangeAllocations.add(allocation)
                report.created_transport_allocations++
            }
            portAllocation(layer + ':' + route + ':source', transport.sourcePort, flow.source)
            portAllocation(layer + ':' + route + ':target', transport.targetPort, flow.target)
        }
    }
}
log.info('Protection native exchange/port realizations and transport allocations added; physical evidence remains NOT_VERIFIED')
return report

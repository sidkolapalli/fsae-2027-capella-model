// Run after 12-normalize-interface-leaves.groovy in the exact disposable acceptance project.
// Adds stored native transport/port traces only; no interfaces, endpoint IDs or physical hardware are replaced.
// Repeatable: existing correctly directed traces are reused. Model structure is not vehicle verification.
assert session.sessionResource.URI.isPlatformResource()
assert session.sessionResource.URI.segment(1) == 'FSAE-2027-Electric-Arcadia'
assert api.layer('SA').system.summary == 'FSAE2027:bootstrap-v1'

def interfaceNames = ['Validated torque demand','Tractive electrical energy','Motor electrical power',
    'Battery safety status','Demand plausibility status','Brake safety status','Energization authorization',
    'Tractive and readiness status','Charging energy and authorization','Thermal condition','Low voltage supply']
def byName = { type ->
    def result = [:]
    api.findAll(type).findAll { it.name?.startsWith('FSAE2027:') }.each { element ->
        assert !result.containsKey(element.name): 'Ambiguous candidate ' + type + ' name: ' + element.name
        result[element.name] = element
    }
    result
}
// Read the model once before writing; avoid rebuilding the model index during the command.
def componentExchanges = byName('ComponentExchange'), functionalExchanges = byName('FunctionalExchange')
def pairs = interfaceNames.collect { name ->
    def byLayer = [:]
    ['LA','PA'].each { layer ->
        def ce = componentExchanges['FSAE2027:' + layer + ' ' + name + ' transport']
        def fe = functionalExchanges['FSAE2027:' + layer + ' ' + name]
        assert ce != null && fe != null: 'Missing normalized ' + layer + ' interface: ' + name
        assert ce.source instanceof ComponentPort && ce.target instanceof ComponentPort
        assert fe.source instanceof FunctionOutputPort && fe.target instanceof FunctionInputPort
        ['source','target'].each { field ->
            def component = ce[field].eContainer(), fn = fe[field].eContainer()
            assert component != null && fn instanceof AbstractFunction && fn.ownedFunctions.isEmpty()
            assert component.ownedFunctionalAllocation.any { it.sourceElement == component && it.targetElement == fn }:
                'Transport port and function port do not share an allocating component for ' + layer + ' ' + name + ' ' + field
            assert ce[field].kind == ComponentPortKind.FLOW
            assert ce[field].orientation == (field == 'source' ? OrientationPortKind.OUT : OrientationPortKind.IN)
        }
        byLayer[layer] = [component:ce,functional:fe]
    }
    [name:name,layers:byLayer]
}
def before = [:]
pairs.each { pair ->
    pair.layers.values().each { item ->
        [item.component,item.functional].each { exchange ->
            before[api.id(exchange)] = [source:api.id(exchange.source),target:api.id(exchange.target),container:api.id(exchange.eContainer())]
        }
    }
}
assert before.size() == 44
def fa = org.polarsys.capella.core.data.fa.FaFactory.eINSTANCE
def information = org.polarsys.capella.core.data.information.InformationFactory.eINSTANCE
def report = [phase:'realize-base-interface-transports',interface_count:pairs.size(),
    created_by_type:[:],reused_by_type:[:],interfaces:[],preserved_exchange_count:before.size(),
    verification_status:'not_verified',compliance_status:'not_claimed']

def storedList = { owner, field ->
    def feature = owner.eClass().getEStructuralFeature(field)
    assert feature != null && feature.changeable && !feature.derived && feature.many:
        'A writable stored native feature is required: ' + owner.eClass().name + '.' + field
    owner.eGet(feature)
}
def trace = { owner, field, source, target, className, factory ->
    def list = storedList(owner,field)
    def matches = list.findAll { item -> item.eClass().name == className && item.sourceElement == source && item.targetElement == target }
    assert matches.size() <= 1: 'Duplicate native ' + className + ' for ' + api.id(source) + ' -> ' + api.id(target)
    if (!matches.isEmpty()) {
        report.reused_by_type[className] = (report.reused_by_type[className] ?: 0) + 1
        return matches[0]
    }
    def relation = factory()
    assert relation.eClass().name == className
    relation.id = java.util.UUID.nameUUIDFromBytes(
        ('FSAE2027:transport-v1:' + className + ':' + api.id(source) + ':' + api.id(target)).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString()
    relation.sourceElement = source; relation.targetElement = target
    list.add(relation)
    report.created_by_type[className] = (report.created_by_type[className] ?: 0) + 1
    relation
}
api.batch('Realize candidate base transports and allocate matching native interface ports') {
    pairs.each { pair ->
        def la = pair.layers.LA, pa = pair.layers.PA
        def entry = [name:pair.name,component_exchange_realization:null,component_port_realizations:[],
            functional_exchange_allocations:[],function_port_allocations:[]]
        def ceTrace = trace(pa.component,'ownedComponentExchangeRealizations',pa.component,la.component,
            'ComponentExchangeRealization',{ fa.createComponentExchangeRealization() })
        entry.component_exchange_realization = api.id(ceTrace)
        ['source','target'].each { field ->
            def portTrace = trace(pa.component[field],'ownedPortRealizations',pa.component[field],la.component[field],
                'PortRealization',{ information.createPortRealization() })
            entry.component_port_realizations << api.id(portTrace)
        }
        ['LA','PA'].each { layer ->
            def ce = pair.layers[layer].component, fe = pair.layers[layer].functional
            def allocation = trace(ce,'ownedComponentExchangeFunctionalExchangeAllocations',ce,fe,
                'ComponentExchangeFunctionalExchangeAllocation',{ fa.createComponentExchangeFunctionalExchangeAllocation() })
            entry.functional_exchange_allocations << [layer:layer,id:api.id(allocation),component_exchange:api.id(ce),functional_exchange:api.id(fe)]
            ['source','target'].each { field ->
                def portAllocation = trace(ce[field],'ownedPortAllocations',ce[field],fe[field],
                    'PortAllocation',{ information.createPortAllocation() })
                entry.function_port_allocations << [layer:layer,side:field,id:api.id(portAllocation),component_port:api.id(ce[field]),function_port:api.id(fe[field])]
            }
        }
        report.interfaces << entry
    }
    // Verify persisted relations directly, without mutating derived views such as allocatedFunctionalExchanges.
    pairs.each { pair ->
        def la = pair.layers.LA, pa = pair.layers.PA
        assert pa.component.ownedComponentExchangeRealizations.count { it.sourceElement == pa.component && it.targetElement == la.component } == 1
        ['source','target'].each { field ->
            assert pa.component[field].ownedPortRealizations.count { it.sourceElement == pa.component[field] && it.targetElement == la.component[field] } == 1
        }
        pair.layers.each { layer,item ->
            assert item.component.ownedComponentExchangeFunctionalExchangeAllocations.count { it.sourceElement == item.component && it.targetElement == item.functional } == 1
            ['source','target'].each { field ->
                assert item.component[field].ownedPortAllocations.count { it.sourceElement == item.component[field] && it.targetElement == item.functional[field] } == 1
            }
            [item.component,item.functional].each { exchange ->
                def original = before[api.id(exchange)]; assert original != null
                assert api.id(exchange.source) == original.source && api.id(exchange.target) == original.target
                assert api.id(exchange.eContainer()) == original.container
            }
        }
    }
}
report.stored_link_counts = [component_exchange_realizations:11,component_port_realizations:22,
    component_exchange_functional_exchange_allocations:22,component_port_function_port_allocations:44]
return report

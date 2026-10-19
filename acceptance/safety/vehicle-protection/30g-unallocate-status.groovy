
// EMF tree iterators also inherit a List used internally as a traversal stack.
// Walk hasNext/next explicitly; Groovy collection extensions can inspect that stack.
def nativeDescendants = { element ->
    def values = new java.util.ArrayList()
    def iterator = element.eAllContents()
    while (iterator.hasNext()) values.add(iterator.next())
    values
}

// Additive bounded candidate design. No hardware choice, physical test or compliance claim.
// Requires the pinned source import, bootstrap and existing shutdown BOTS contact.
assert session.sessionResource.URI.toString().toLowerCase().contains('/fsae-2027-electric-arcadia/')
def req = api.requirements()
assert req.available() && req.viewpointEnabled()
assert api.layer('SA').system.summary == 'FSAE2027:bootstrap-v1'
def prefix = 'FSAE2027:VP '
def sliceId = 'FSAE2027:vehicle-protection-v1'
def phase = 7
def stableId = { key -> java.util.UUID.nameUUIDFromBytes(('FSAE2027:VP:' + key).getBytes('UTF-8')).toString() }
assert req.find('FSAE2027:VP:derived-module') == null:
    'Vehicle-protection draft already applied: verify/extend instead of replaying'
if (phase == 1) {
    assert api.findById(stableId('logical:loadpath')) == null
    assert api.find('LogicalComponent', prefix + 'Primary structure load paths Logical subsystem') == null
}
if (phase == 2) assert api.findById(stableId('function:OA:loadpath')) == null: 'Leaf-function phase already applied'
if (phase == 3) assert api.findById(stableId('flow:OA:pedal:brake-demand')) == null: 'Exchange phase already applied'
if (phase >= 4) assert api.findById(stableId('flow:PA:protect-brakes:friction')) != null: 'Finish the exchange phase first'
def sourceModule = req.find('FSAE-2027-V1:source-module')
assert sourceModule != null
def existingBots = api.findById(java.util.UUID.nameUUIDFromBytes('FSAE2027:SD:node:BOTS'.getBytes('UTF-8')).toString())
assert existingBots != null && existingBots.eClass().name == 'PhysicalComponent'
assert existingBots.nature == PhysicalComponentNature.NODE:
    'Apply the shutdown slice first; this draft reuses its direct BOTS contact'
def shutdownCutout = api.findById(java.util.UUID.nameUUIDFromBytes('FSAE2027:SD:function:SA:cutout'.getBytes('UTF-8')).toString())
assert shutdownCutout != null
def shutdownCutouts = ['SA','LA','PA'].collectEntries { layer ->
    def cutout = api.findById(java.util.UUID.nameUUIDFromBytes(('FSAE2027:SD:function:' + layer + ':cutout').getBytes('UTF-8')).toString())
    assert cutout != null
    [(layer):cutout]
}
def descriptions = [structure:'Support and protect occupants and equipment',
    cockpit:'Accommodate and restrain the driver', brakes:'Decelerate and stop the vehicle',
    status:'Indicate electrical and readiness states']
def domains = descriptions.collectEntries { key, description ->
    def values = [logical:api.find('LogicalComponent', 'FSAE2027:' + key + ' Logical subsystem'),
        behavior:api.find('PhysicalComponent', 'FSAE2027:' + key + ' Physical behavior'),
        equipment:api.find('PhysicalComponent', 'FSAE2027:' + key + ' Equipment'),
        ci:api.find('ConfigurationItem', 'FSAE2027:' + key + ' Subsystem CI')]
    [OA:'OperationalActivity', SA:'SystemFunction', LA:'LogicalFunction', PA:'PhysicalFunction'].each { layer, type ->
        values[layer] = api.find(type, 'FSAE2027:' + key + ' ' + description)
    }
    assert values.values().every { it != null }: 'Missing bootstrap domain ' + key
    [(key):values]
}
def vehicle = api.find('Entity', 'FSAE2027:Vehicle in operational context')
assert vehicle != null
def team = api.find('Entity', 'FSAE2027:Team personnel Operational actor')
assert team != null
def specs = [
    [key:'loadpath', domain:'structure', name:'Primary structure load paths'],
    [key:'rollover', domain:'structure', name:'Roll hoops bracing and helmet envelope'],
    [key:'side', domain:'structure', name:'Conditional side impact structure'],
    [key:'front', domain:'structure', name:'Attenuator anti-intrusion and front load paths'],
    [key:'accommodate', domain:'cockpit', name:'Driver and cockpit geometry'],
    [key:'barrier', domain:'cockpit', name:'Floor firewall and hazard shields'],
    [key:'restrain', domain:'cockpit', name:'Driver harness and seat retention'],
    [key:'head', domain:'cockpit', name:'Head restraint and contact padding'],
    [key:'pedal', domain:'brakes', name:'Mechanical brake pedal box and mount'],
    [key:'hydraulic-a', domain:'brakes', name:'Independent hydraulic circuit A and reserve'],
    [key:'hydraulic-b', domain:'brakes', name:'Independent hydraulic circuit B and reserve'],
    [key:'friction', domain:'brakes', name:'Wheel braking distribution topology TBD'],
    [key:'overtravel', domain:'brakes', name:'Direct brake overtravel detection', existingNode:existingBots],
    [key:'brake-light', domain:'status', name:'Rear brake indication']
]
def functionSpecs = [
    [key:'loadpath', domain:'structure', owner:'loadpath', name:'Carry occupant protection loads through primary structure'],
    [key:'rollover', domain:'structure', owner:'rollover', name:'Maintain protected helmet head and hand envelope during rollover'],
    [key:'side', domain:'structure', owner:'side', name:'Resist side intrusion under the selected chassis construction'],
    [key:'front', domain:'structure', owner:'front', name:'Absorb frontal impact energy and prevent cockpit intrusion'],
    [key:'accommodate', domain:'cockpit', owner:'accommodate', name:'Accommodate prescribed drivers and cockpit inspection geometry'],
    [key:'barrier', domain:'cockpit', owner:'barrier', name:'Separate driver and harness from debris fluids TS and moving hazards'],
    [key:'restrain', domain:'cockpit', owner:'restrain', name:'Retain driver hips torso and limbs within the protective envelope'],
    [key:'head', domain:'cockpit', owner:'head', name:'Limit rearward head travel and absorb helmet contact'],
    [key:'pedal', domain:'brakes', owner:'pedal', name:'Transmit pedal force without obstructive component failure'],
    [key:'brake-demand', domain:'brakes', owner:'pedal', name:'Apply a single mechanical braking demand'],
    [key:'hydraulic-a', domain:'brakes', owner:'hydraulic-a', name:'Deliver independent hydraulic braking through circuit A'],
    [key:'hydraulic-b', domain:'brakes', owner:'hydraulic-b', name:'Deliver independent hydraulic braking through circuit B'],
    [key:'friction', domain:'brakes', owner:'friction', name:'Brake all wheels and retain effective two-wheel braking after failure'],
    [key:'protect-brakes', domain:'brakes', owner:'friction', name:'Protect brake components from drivetrain debris and collision'],
    [key:'overtravel', domain:'brakes', owner:'overtravel', name:'Open and hold direct BOTS contact on excessive pedal travel'],
    [key:'brake-light', domain:'status', owner:'brake-light', name:'Indicate braking visibly to observers behind vehicle'],
    [key:'inspect', domain:'cockpit', owner:'accommodate', name:'Expose occupant protection and brake verification interfaces']
]
def components = [:]
def functions = [:]
def created = [:]
if (phase > 1) {
    specs.each { spec ->
        def value = [:]
        [logical:'logical',behavior:'behavior',node:'node',ci:'ci'].each { field, tag ->
            value[field] = field == 'node' && spec.existingNode != null ? spec.existingNode : api.findById(stableId(tag + ':' + spec.key))
            assert value[field] != null && value[field].eResource() != null: 'Run component phase first: ' + spec.key
        }
        components[spec.key] = value
    }
}
if (phase > 2) {
    functionSpecs.each { spec ->
        def value = [owner:spec.owner]
        ['OA','SA','LA','PA'].each { layer ->
            value[layer] = api.findById(stableId('function:' + layer + ':' + spec.key))
            assert value[layer] != null && value[layer].eResource() != null: 'Run leaf-function phase first: ' + spec.key
        }
        functions[spec.key] = value
    }
}
def state = [slice_id:sliceId, phase:phase, completion_status:'bounded_candidate_design_in_progress', verification_status:'not_verified',
    source_record_count:409, planned_derived_obligation_count:32, applied_derived_obligations:0, component_ids:[:], function_ids:[:],
    derived_requirements:[:], verification_constraints:[:], reused_bots:api.id(existingBots), model_element_count:0]
def identify = { value, key ->
    assert !created.containsKey(key): 'Duplicate new model key ' + key
    assert api.set(value, 'id', stableId(key))
    created[key] = value
    value
}
def textExpression = { text ->
    def value = org.polarsys.capella.core.data.information.datavalue.DatavalueFactory.eINSTANCE.createOpaqueExpression()
    value.languages.add('Text')
    value.bodies.add(text)
    value
}
def constraint = { key, name, text, references ->
    def value = identify(api.create('Constraint', prefix + name, api.layer('SA')), 'constraint:' + key)
    value.ownedSpecification = textExpression(text)
    value.constrainedElements.addAll(references.unique())
    value.summary = 'NOT_VERIFIED. Candidate verification or unresolved design obligation; no test evidence or compliance conclusion.'
    value
}
// Existing shutdown hardware may have its representing instance at the PA root.
// Scan this architecture once; avoid repeated traversal of the SA requirements module.
def existingParts = new java.util.IdentityHashMap()
nativeDescendants(api.layer('PA')).each { item ->
    if (item instanceof Part && item.abstractType != null) {
        if (!existingParts.containsKey(item.abstractType)) existingParts[item.abstractType] = []
        existingParts[item.abstractType] << item
    }
}
def partOf = { component ->
    def value = component.eContainer().ownedFeatures.find { it instanceof Part && it.abstractType == component }
    if (value == null) {
        def candidates = existingParts[component] ?: []
        assert candidates.size() == 1: 'Missing or ambiguous existing PA representing Part for ' + component.name
        value = candidates[0]
    }
    assert value.eResource() != null && value.abstractType == component
    value
}

api.batch('Add bounded vehicle protection and independent mechanical braking candidate') {
    if (phase == 1) {
    specs.each { spec ->
        def logical = identify(api.create('LogicalComponent', prefix + spec.name + ' Logical subsystem', domains[spec.domain].logical), 'logical:' + spec.key)
        def behavior = identify(api.create('PhysicalComponent', prefix + spec.name + ' Physical behavior', domains[spec.domain].behavior), 'behavior:' + spec.key)
        behavior.nature = PhysicalComponentNature.BEHAVIOR
        def node = spec.existingNode
        if (node == null) {
            node = identify(api.create('PhysicalComponent', prefix + spec.name + ' Equipment', domains[spec.domain].equipment), 'node:' + spec.key)
            node.nature = PhysicalComponentNature.NODE
            node.kind = PhysicalComponentKind.HARDWARE
            node.summary = 'Candidate equipment boundary; exact material, dimensions, hardware, construction and proof evidence remain TBD.'
        }
        [logical,behavior].each { it.summary = 'Bounded source-grounded candidate partition. Construction choices and physical verification are unresolved.' }
        api.link(logical, api.layer('SA').system, 'realization')
        api.link(behavior, logical, 'realization')
        def deployment = identify(org.polarsys.capella.core.data.pa.deployment.DeploymentFactory.eINSTANCE.createPartDeploymentLink(), 'deployment:' + spec.key)
        deployment.deployedElement = partOf(behavior)
        deployment.location = partOf(node)
        deployment.location.ownedDeploymentLinks.add(deployment)
        def ci = identify(api.create('ConfigurationItem', prefix + spec.name + ' CI', domains[spec.domain].ci), 'ci:' + spec.key)
        ci.kind = ConfigurationItemKind.HWCI
        ci.itemIdentifier = 'FSAE2027:VP:' + spec.key
        api.link(ci, node, 'PhysicalArtifactRealization')
        api.link(ci, behavior, 'PhysicalArtifactRealization')
        components[spec.key] = [logical:logical, behavior:behavior, node:node, ci:ci]
        state.component_ids[spec.key] = [logical:api.id(logical), behavior:api.id(behavior), node:api.id(node), ci:api.id(ci)]
    }
    }
    if (phase == 2) {
    functionSpecs.each { spec ->
        def value = [owner:spec.owner]
        [OA:'OperationalActivity', SA:'SystemFunction', LA:'LogicalFunction', PA:'PhysicalFunction'].each { layer, type ->
            def function = identify(api.create(type, prefix + spec.name, domains[spec.domain][layer]), 'function:' + layer + ':' + spec.key)
            api.allocate(function, layer == 'OA' ? vehicle : layer == 'SA' ? api.layer('SA').system : components[spec.owner][layer == 'LA' ? 'logical' : 'behavior'])
            function.summary = 'Candidate protection/braking responsibility. Exact construction, conditional applicability and verification remain unresolved.'
            value[layer] = function
        }
        api.link(value.SA, value.OA, 'realization')
        api.link(value.LA, value.SA, 'realization')
        api.link(value.PA, value.LA, 'realization')
        functions[spec.key] = value
        state.function_ids[spec.key] = [OA:api.id(value.OA), SA:api.id(value.SA), LA:api.id(value.LA), PA:api.id(value.PA)]
    }
    def inspectActivity = identify(api.create('OperationalActivity', prefix + 'Inspect protection geometry and inject brake failures', domains.cockpit.OA), 'activity:inspection')
    api.allocate(inspectActivity, team)
    api.link(functions.inspect.SA, inspectActivity, 'realization')
    }
    if (phase >= 4 && phase <= 7) {
    // These marked bootstrap functions are now aggregates. Native allocation belongs on leaves.
    [domains[['structure','cockpit','brakes','status'][phase - 4]]].each { domain ->
        ['OA','SA','LA','PA'].each { layer ->
            def aggregate = domain[layer]
            assert !aggregate.ownedFunctions.isEmpty()
            aggregate.componentFunctionalAllocations.toList().each { allocation ->
                assert allocation.targetElement == aggregate
                assert api.delete(allocation)
            }
            assert aggregate.componentFunctionalAllocations.isEmpty()
        }
    }
    }
    if (phase == 3) {
    // Distinct A/B pressure paths remain independent. Actual wheel split and hardware are TBD.
    def routes = [
        ['pedal','brake-demand','Single mechanical pedal effort'],
        ['brake-demand','hydraulic-a','Mechanical drive to independent circuit A'],
        ['brake-demand','hydraulic-b','Mechanical drive to independent circuit B'],
        ['hydraulic-a','friction','Circuit A pressure and effective wheel-braking output'],
        ['hydraulic-b','friction','Circuit B pressure and effective wheel-braking output'],
        ['brake-demand','brake-light','Brake activation indication trigger details TBD'],
        ['pedal','overtravel','Travel beyond approved normal pedal range'],
        ['accommodate','restrain','Driver position and adjustment envelope'],
        ['restrain','rollover','Restrained occupant and helmet envelope'],
        ['restrain','head','Driver helmet restraint contact geometry'],
        ['front','loadpath','Frontal and off-axis structural load'],
        ['side','loadpath','Side-impact structural load'],
        ['rollover','loadpath','Roll hoop and brace structural load'],
        ['protect-brakes','friction','Protected hydraulic and wheel-brake availability']
    ]
    routes.each { route ->
        ['OA','SA','LA','PA'].each { layer ->
            def exchange = identify(api.link(functions[route[0]][layer], functions[route[1]][layer], 'FunctionalExchange'), 'flow:' + layer + ':' + route[0] + ':' + route[1])
            api.rename(exchange, prefix + layer + ' ' + route[2])
        }
        def from = functions[route[0]].owner
        def to = functions[route[1]].owner
        if (from != to) {
            ['LA','PA'].each { layer ->
                def field = layer == 'LA' ? 'logical' : 'behavior'
                def exchange = identify(api.link(components[from][field], components[to][field], 'ComponentExchange'), 'transport:' + layer + ':' + route[0] + ':' + route[1])
                api.rename(exchange, prefix + layer + ' ' + route[2] + ' interface')
            }
        }
    }
    ['SA','LA','PA'].each { layer ->
        def cutout = shutdownCutouts[layer]
        def exchange = identify(api.link(functions.overtravel[layer], cutout, 'FunctionalExchange'), 'bots-trip:' + layer)
        api.rename(exchange, prefix + layer + ' BOTS direct series contact held open')
    }
    }
    if (phase == 8) {
    domains.values().each { domain ->
        ['OA','SA','LA','PA'].each { layer ->
            assert !domain[layer].ownedFunctions.isEmpty() && domain[layer].componentFunctionalAllocations.isEmpty():
                'Complete all four aggregate-allocation cleanup phases before finishing'
        }
    }
    constraint('HYDRAULIC-INDEPENDENCE', 'Independent brake circuits and unresolved wheel split INVARIANT',
        'One mechanical pedal operates two independent hydraulic circuits with distinct fluid reserves. Any leak/failure anywhere leaves effective braking on at least two wheels. A/B wheel mapping, OEM/shared housing internal separation, any allowed limited-slip differential brake and physical tubing remain TBD. The shared wheel-braking behavior is an abstract boundary, not a junction combining hydraulic circuits. No brake-by-wire path or regenerating drive replaces direct hydraulic braking.',
        [functions['hydraulic-a'].SA,functions['hydraulic-b'].SA,functions.friction.SA,components['hydraulic-a'].node,components['hydraulic-b'].node,components.friction.node])
    constraint('CONSTRUCTION-CHOICES', 'Material-dependent architecture alternatives TBD',
        'Select and justify tube-frame, monocoque or hybrid; steel/aluminum/composite member applications; AIP material/attachment; standard/custom IA and construction; side-impact proof method; harness posture/type; pedal fabrication/material; optional regeneration; circuit split and wheel hardware. Until reviewed, retain all conditional source obligations. The model contains candidate responsibility/equipment boundaries and does not instantiate mutually exclusive material alternatives as built hardware.',
        components.values().collectMany { [it.logical,it.node,it.ci] })
    constraint('DEPENDENCIES', 'Bounded slice dependencies and exclusions OPEN',
        'Complete referenced structural equivalence/material tests, bulkhead/support/bracing/joint rules, cockpit internal section and thermal protection, all harness mounting rules, fastener rules, EV grounding/outboard-motor conditions, drivetrain scatter protection, official event-site anthropometry/standard IA/FAQ, and cross-slice regenerative braking/plausibility interfaces. Selected source clauses only; full V/F/T/event coverage and any compliance conclusion remain open.',
        components.values().collect { it.logical })
    constraint('AGGREGATE-INTERFACES', 'Shared bootstrap aggregate interfaces require leaf mapping OPEN',
        'This slice removes allocations from the decomposed structure/cockpit/brakes/status aggregate functions and allocates only new leaves. Existing bootstrap exchanges whose endpoints still reference aggregate ports require coordinated leaf/delegation mapping by the architecture integration repair; they are not silently converted into BOTS or brake-lamp semantics. Native whole-model validation and that shared integration step are required before claiming a valid complete architecture.',
        domains.values().collectMany { domain -> ['OA','SA','LA','PA'].collect { domain[it] } })

    req.createFolder(req.createModule(api.layer('SA'), 'FSAE2027:VP:derived-module', 'Vehicle protection and mechanical braking candidate obligations'), 'FSAE2027:VP:derived-folder', 'Bounded chassis cockpit and brakes')
    }
    state.model_element_count = created.size()
    log.info('Applied bounded vehicle protection/braking structure phase ' + phase + '; finish every structure and trace phase before verification')
}
assert api.layer('SA').system.ownedSystemComponents.isEmpty() && api.layer('SA').system.ownedFeatures.isEmpty()
return state

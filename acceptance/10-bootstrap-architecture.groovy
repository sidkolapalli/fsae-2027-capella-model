// Run only after creating the exact new acceptance project and enabling Requirements Viewpoint.
// This establishes candidate architecture and unresolved decisions, not reviewed rule coverage.
assert session.sessionResource.URI.toString().toLowerCase().contains('/fsae-2027-electric-arcadia/')
assert api.requirements().available() && api.requirements().viewpointEnabled()
def marker = 'FSAE2027:bootstrap-v1'
def system = api.layer('SA').system
assert system != null
assert system.summary == null || system.summary.trim().isEmpty() || system.summary == marker:
    'The selected model already has a different architecture marker'
assert api.find('LogicalComponent', 'FSAE2027:structure Logical subsystem') == null:
    'Bootstrap already exists. Verify or extend its model instead of overwriting reviewed decisions.'

def domains = [
    [key:'structure', name:'Chassis and crash protection', function:'Support and protect occupants and equipment'],
    [key:'cockpit', name:'Cockpit and driver accommodation', function:'Accommodate and restrain the driver'],
    [key:'brakes', name:'Braking and brake safety', function:'Decelerate and stop the vehicle'],
    [key:'driver-demand', name:'Driver demand and plausibility', function:'Interpret and validate driver torque demand'],
    [key:'powertrain', name:'Electric propulsion and mechanical drive', function:'Convert traction energy into vehicle motion'],
    [key:'energy-storage', name:'Tractive battery and energy storage', function:'Store and supply traction energy'],
    [key:'tractive-distribution', name:'Tractive distribution and isolation', function:'Distribute and isolate tractive electrical energy'],
    [key:'shutdown', name:'Shutdown and electrical protection', function:'Prevent or interrupt unsafe tractive energization'],
    [key:'status', name:'Driver and external status indication', function:'Indicate electrical and readiness states'],
    [key:'charging', name:'Charging and charging protection', function:'Recharge traction energy under protected conditions'],
    [key:'low-voltage', name:'Low voltage power and electronics', function:'Supply and manage low voltage electrical power'],
    [key:'thermal', name:'Thermal and fluid management', function:'Manage component temperatures and fluids'],
    [key:'aerodynamics', name:'Bodywork and aerodynamic devices', function:'Provide aerodynamic and environmental surfaces'],
    [key:'fasteners', name:'Mechanical attachments and retention', function:'Retain critical and noncritical assemblies'],
    [key:'running-gear', name:'Steering suspension wheels and tires', function:'Control direction and maintain wheel contact']
]
def interfaceCandidates = [
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
def decisions = [
    ['DEC-CHASSIS','Choose and substantiate the allowed chassis construction'],
    ['DEC-CELLS','Select exact cells configuration and manufacturer ratings'],
    ['DEC-DRIVE','Select motor inverter and driveline topology'],
    ['DEC-BRAKES','Design brake circuits hardware and validated performance'],
    ['DEC-CHARGER','Select charger equipment limits and protective implementation'],
    ['DEC-THERMAL','Define cooling strategy and worst-case thermal design'],
    ['DEC-CONTROL','Define control hardware software and independent safety implementation'],
    ['DEC-PRESSURE','Determine whether pressurized equipment triggers conditional rules']
]
def state = [project:session.sessionResource.URI.toString(), marker:marker, completion_status:'in_progress',
    domains:[:], actors:[:], decisions:[], interfaces:[], capabilities:[:]]
def interactionFactory = org.polarsys.capella.core.data.interaction.InteractionFactory.eINSTANCE
def deploymentFactory = org.polarsys.capella.core.data.pa.deployment.DeploymentFactory.eINSTANCE
def commonFactory = org.polarsys.capella.core.data.capellacommon.CapellacommonFactory.eINSTANCE

api.batch('Bootstrap 2027 FSAE electric candidate architecture') {
    assert api.rename(system, 'FSAE2027:Electric Vehicle')
    assert api.set(system, 'summary', marker)
    assert system.ownedSystemComponents.isEmpty() && system.ownedFeatures.isEmpty()
    def logicalSystem = api.layer('LA').system
    def physicalSystem = api.layer('PA').system
    def product = api.epbs().system
    assert logicalSystem != null && physicalSystem != null && product != null
    api.rename(logicalSystem, 'FSAE2027:Electric Vehicle Logical System')
    api.rename(physicalSystem, 'FSAE2027:Electric Vehicle Physical System')
    api.rename(product, 'FSAE2027:Electric Vehicle Product')
    api.set(product, 'kind', ConfigurationItemKind.SYSTEM_CI)
    api.set(product, 'itemIdentifier', 'FSAE2027:PRODUCT')
    def vehicle = api.create('OA', 'Entity', 'FSAE2027:Vehicle in operational context')
    api.link(system, vehicle, 'realization')
    if (!logicalSystem.ownedComponentRealizations.any { it.targetElement == system })
        api.link(logicalSystem, system, 'realization')
    if (!physicalSystem.ownedComponentRealizations.any { it.targetElement == logicalSystem })
        api.link(physicalSystem, logicalSystem, 'realization')
    if (!product.ownedPhysicalArtifactRealizations.any { it.targetElement == physicalSystem })
        api.link(product, physicalSystem, 'PhysicalArtifactRealization')
    def module = api.requirements().createModule(api.layer('SA'), 'FSAE-2027-V1:source-module', 'Formula SAE 2027 V1 source obligations')
    state.source_module = api.id(module)

    ['Driver','Team personnel','Competition officials','Emergency responders'].each { role ->
        def oa = api.create('OA', 'OperationalActor', 'FSAE2027:' + role + ' Operational actor')
        def sa = api.create('SA', 'SystemActor', 'FSAE2027:' + role + ' System actor')
        def la = api.create('LA', 'LogicalActor', 'FSAE2027:' + role + ' Logical actor')
        def pa = api.create('PA', 'PhysicalActor', 'FSAE2027:' + role + ' Physical actor')
        [oa,sa,la,pa].each { api.set(it,'human',true) }
        api.set(pa, 'nature', PhysicalComponentNature.NODE)
        api.set(pa, 'kind', PhysicalComponentKind.PERSON)
        api.link(sa, oa, 'realization')
        api.link(la, sa, 'realization')
        api.link(pa, la, 'realization')
        state.actors[role] = [OA:api.id(oa), SA:api.id(sa), LA:api.id(la), PA:api.id(pa)]
    }

    domains.each { domain ->
        def key = domain.key
        def oa = api.create('OA', 'OperationalActivity', 'FSAE2027:' + key + ' ' + domain.function)
        def sa = api.create('SA', 'SystemFunction', 'FSAE2027:' + key + ' ' + domain.function)
        def la = api.create('LA', 'LogicalFunction', 'FSAE2027:' + key + ' ' + domain.function)
        def pa = api.create('PA', 'PhysicalFunction', 'FSAE2027:' + key + ' ' + domain.function)
        def logical = api.create('LogicalComponent', 'FSAE2027:' + key + ' Logical subsystem', logicalSystem)
        def behavior = api.create('PhysicalComponent', 'FSAE2027:' + key + ' Physical behavior', physicalSystem)
        def equipment = api.create('PhysicalComponent', 'FSAE2027:' + key + ' Equipment', physicalSystem)
        api.set(behavior, 'nature', PhysicalComponentNature.BEHAVIOR)
        api.set(equipment, 'nature', PhysicalComponentNature.NODE)
        api.set(equipment, 'kind', PhysicalComponentKind.HARDWARE)
        [logical,behavior,equipment].each { element ->
            api.set(element, 'summary', 'Candidate design partition. Detailed implementation and source clause coverage require semantic review.')
        }
        api.allocate(oa, vehicle)
        api.allocate(sa, system)
        api.allocate(la, logical)
        api.allocate(pa, behavior)
        api.link(sa, oa, 'realization')
        api.link(la, sa, 'realization')
        api.link(pa, la, 'realization')
        api.link(logical, system, 'realization')
        api.link(behavior, logical, 'realization')
        def behaviorPart = api.ensurePart(behavior)
        def equipmentPart = api.ensurePart(equipment)
        def deployment = deploymentFactory.createPartDeploymentLink()
        deployment.deployedElement = behaviorPart
        deployment.location = equipmentPart
        equipmentPart.ownedDeploymentLinks.add(deployment)
        def ci = api.create('ConfigurationItem', 'FSAE2027:' + key + ' Subsystem CI', product)
        api.set(ci, 'kind', ConfigurationItemKind.SYSTEM_CI)
        api.set(ci, 'itemIdentifier', 'FSAE2027:' + key.toUpperCase())
        api.link(ci, behavior, 'PhysicalArtifactRealization')
        api.link(ci, equipment, 'PhysicalArtifactRealization')
        def operationalCapability = api.create('OA', 'OperationalCapability', 'FSAE2027:' + key + ' Operational capability')
        def systemCapability = api.create('SA', 'Capability', 'FSAE2027:' + key + ' System capability')
        def logicalCapability = api.create('LA', 'CapabilityRealization', 'FSAE2027:' + key + ' Logical capability realization')
        def physicalCapability = api.create('PA', 'CapabilityRealization', 'FSAE2027:' + key + ' Physical capability realization')
        [ [operationalCapability,oa], [systemCapability,sa], [logicalCapability,la], [physicalCapability,pa] ].each { pair ->
            def involvement = interactionFactory.createAbstractFunctionAbstractCapabilityInvolvement()
            involvement.involved = pair[1]
            pair[0].ownedAbstractFunctionAbstractCapabilityInvolvements.add(involvement)
        }
        def vehicleInvolvement = OaFactory.eINSTANCE.createEntityOperationalCapabilityInvolvement()
        vehicleInvolvement.involved = vehicle
        operationalCapability.ownedEntityOperationalCapabilityInvolvements.add(vehicleInvolvement)
        def systemInvolvement = CtxFactory.eINSTANCE.createCapabilityInvolvement()
        systemInvolvement.involved = system
        systemCapability.ownedCapabilityInvolvements.add(systemInvolvement)
        [ [logicalCapability,logical], [physicalCapability,behavior], [physicalCapability,equipment] ].each { pair ->
            def involvement = commonFactory.createCapabilityRealizationInvolvement()
            involvement.involved = pair[1]
            pair[0].ownedCapabilityRealizationInvolvements.add(involvement)
        }
        [ [systemCapability,operationalCapability], [logicalCapability,systemCapability], [physicalCapability,logicalCapability] ].each { pair ->
            def capabilityTrace = interactionFactory.createAbstractCapabilityRealization()
            capabilityTrace.targetElement = pair[1]
            capabilityTrace.sourceElement = pair[0]
            pair[0].ownedAbstractCapabilityRealizations.add(capabilityTrace)
        }
        state.domains[key] = [OA:api.id(oa),SA:api.id(sa),LA:api.id(la),PA:api.id(pa),
            logical:api.id(logical),behavior:api.id(behavior),equipment:api.id(equipment),ci:api.id(ci)]
        state.capabilities[key] = [OA:api.id(operationalCapability),SA:api.id(systemCapability),
            LA:api.id(logicalCapability),PA:api.id(physicalCapability)]
    }
    interfaceCandidates.each { candidate ->
        def source = state.domains[candidate[0]]
        def target = state.domains[candidate[1]]
        def interfaces = [name:candidate[2],candidate_status:'unreviewed',functions:[:],components:[:]]
        ['OA','SA','LA','PA'].each { layer ->
            def exchange = api.link(api.findById(source[layer]), api.findById(target[layer]), 'FunctionalExchange')
            api.rename(exchange, 'FSAE2027:' + layer + ' ' + candidate[2])
            interfaces.functions[layer] = api.id(exchange)
        }
        ['LA','PA'].each { layer ->
            def field = layer == 'LA' ? 'logical' : 'behavior'
            def exchange = api.link(api.findById(source[field]), api.findById(target[field]), 'ComponentExchange')
            api.rename(exchange, 'FSAE2027:' + layer + ' ' + candidate[2] + ' transport')
            interfaces.components[layer] = api.id(exchange)
        }
        state.interfaces << interfaces
    }
    decisions.each { decision ->
        def constraint = api.create('Constraint', 'FSAE2027:' + decision[0] + ' UNRESOLVED', api.layer('SA'))
        api.set(constraint, 'summary', decision[1] + '. No numerical design value or compliance claim is assigned until this decision is reviewed.')
        def expression = org.polarsys.capella.core.data.information.datavalue.DatavalueFactory.eINSTANCE.createOpaqueExpression()
        expression.languages.add('Text')
        expression.bodies.add(decision[1] + '. Status: unresolved. Select the design, evaluate all source conditions and record the supporting evidence before closing this obligation.')
        constraint.ownedSpecification = expression
        state.decisions << [decision_id:decision[0],element_id:api.id(constraint),status:'unresolved']
    }
    ['Prepare and document competition entry','Inspect and present the vehicle','Charge and maintain under controlled work practices',
        'Operate dynamic events','Stop recover and respond to emergencies'].each { process ->
        def activity = api.create('OA', 'OperationalActivity', 'FSAE2027:process ' + process)
        api.allocate(activity, api.findById(state.actors['Team personnel'].OA))
        api.set(activity, 'summary', 'Operational process candidate. Administrative inspection and competition clauses still require reviewed requirements and evidence mappings.')
    }
    log.info('Created candidate architecture across all five perspectives; source coverage and verification remain pending')
}
return state

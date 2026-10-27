"""Generate a bounded candidate model from pinned facts, without publishing source prose.

The private paragraphs are needed only to verify hashes. Statements below are authored
interpretations. Construction choices and physical evidence are deliberately unresolved.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent
RULES = HERE.parents[1] / "rules"
SOURCE_HASH = "f71df5552e341dab1b58f1ff47212e7281953d66b9c41833de8347560913ed0e"
SLICE = "FSAE2027:vehicle-protection-v1"


def numbered(prefix, last):
    return [f"{prefix}.{i}" for i in range(1, last + 1)]


def obligation(key, title, rules, functions, statement, conditions, verification, decisions, dependencies=()):
    return dict(key=key, identifier="FSAE2027:VP:obligation:" + key, title=title,
                source_rules=rules, model_keys=functions, statement=statement,
                conditions_and_alternatives=conditions, verification_obligation=verification,
                unresolved_decisions=decisions, dependent_source_rules=list(dependencies))


GROUPS = [
    obligation("ENVELOPE", "Accommodate and protect the complete driver population", numbered("V.2.1", 2),
        ["accommodate", "rollover"],
        "Accommodate the 5th-percentile female through 95th-percentile male population in seating, controls and driver equipment. Keep the driver's head and hands clear of the ground in every rollover orientation. Event-site anthropometry is an external input that has not yet been selected.",
        ["All team drivers and the prescribed anthropometric range; no single nominal driver is substituted."],
        "Review an approved anthropometric dataset and every driver/equipment combination. Check reach and occupant geometry and sweep all rollover orientations, including restrained head and hand envelopes.",
        ["Driver dataset, team driver measurements, equipment and adjustable seating/control geometry TBD."], ["F.1.13"]),
    obligation("PRIMARY-MATERIAL", "Choose and substantiate the primary structure", numbered("F.5.1", 2),
        ["loadpath", "side", "rollover"],
        "The primary structure may use steel tubing/material, an allowed alternative tubing material, composite material or a combination. A mixed tube-frame/monocoque/tube/composite design must satisfy the relevant material rules, applicable structural equivalence and connection requirements. No construction is selected by this candidate model.",
        ["Tube, monocoque and hybrid alternatives remain unresolved.", "The source F.5.1.1 and F.3.2.1 references to F.3.4.4 are retained as published; F.3.5 actually contains the aluminum provisions. Seek official clarification rather than silently changing the reference."],
        "Select construction in the structural equivalence submission; enumerate every material and transition joint and substantiate equivalence, fastening, bonding and monocoque attachments under the applicable referenced clauses.",
        ["Chassis type, material grades, laminates, connections and equivalence evidence TBD."], ["F.2.2.2", "F.4", "F.5.4", "F.5.5", "F.7.8"]),
    obligation("TUBE-APPLICATION", "Apply tubing classes and material restrictions", ["F.3.2.1", "F.3.3.1", "F.3.3.2"],
        ["loadpath", "rollover", "side", "restrain", "front"],
        "Assign steel tube classes by application: A for front hoop, main hoop and shoulder-harness mounting bar; B for front bulkhead, front-hoop bracing, side-impact structure, main-hoop bracing, harness attachment and battery mounting/protection; C for front-bulkhead supports, main-hoop brace supports, shoulder-bar bracing, component protection and structural tubing; D for bent upper side-impact members. Aluminum is prohibited for the main hoop, main-hoop bracing and shoulder-harness mounting bar; it is permitted conditionally for the other listed applications. Tubing below the structural class is nonstructural and cannot be credited for rule compliance.",
        ["All rows of the application table are preserved as constraints, including battery protection which is only an interface to this slice."],
        "Build a member/application/material matrix, confirm each required class and prohibited aluminum application, and exclude nonstructural members from the equivalence/load-path assessment.",
        ["Actual member sizes and application/material matrix TBD."], ["F.3.4", "F.3.5"]),
    obligation("STEEL", "Meet all steel minima and prescribed calculation properties", numbered("F.3.4", 4),
        ["loadpath", "rollover", "side", "front"],
        "For each steel class satisfy all four simultaneous minima (inertia mm^4, area mm^2, outside diameter/square width mm, wall mm): A=(11320,173,25.0,2.0), B=(8509,114,25.0,1.2), C=(6695,91,25.0,1.2), D=(18015,126,35.0,1.2). Example round tubes are A:1.0in x0.095in or25 x2.5mm; B:1.0in x0.065in or25.4 x1.6mm; C:1.0in x0.049in or25.4 x1.2mm; D:1.375in x0.049in or35 x1.2mm. Examples are not additional compulsory sizes. For any steel in SES calculations use continuous/nonwelded E=200GPa(29000ksi), Sy=305MPa(44.2ksi), Su=365MPa(52.9ksi); for discontinuous/welded joint calculations use Sy=180MPa(26ksi), Su=300MPa(43.5ksi). If welded reinforcement is needed, prove equivalence of the welded tube plus reinforcement to the original nonwelded tube. Welded inserts have thicker walls and smaller outer diameter/square side than that original tube and are welded at both ends. Do not mechanically alter structural weld shapes.",
        ["All four class minima apply together; weld and reinforcement conditions are not optional substitutes."],
        "Measure selected tubing and calculate inertia/area. Audit SES material cards, continuous/joint properties, reinforcement equivalence, insert dimensions and both end welds; inspect structural welds for mechanical reshaping.",
        ["Steel grade, purchased dimensions/tolerances, members, joints and reinforcement details TBD."]),
    obligation("ALUMINUM", "Retain conditional aluminum construction and evidence", numbered("F.3.5", 3),
        ["loadpath", "side", "front"],
        "Use aluminum tubing only in applications permitted by the application table. The SES includes material identity/properties with purchase, shipping or donation evidence, equivalence calculations against the steel minima/nonwelded reference in yield/ultimate bending, buckling, tension, buckling modulus and energy dissipation, plus manufacturing process details. Aluminum wall minima are 2.0mm nonwelded and3.0mm welded. SES nonwelded6061-T6 values are E=69GPa(10000ksi), Sy=240MPa(34.8ksi), Su=290MPa(42.1ksi); welded values Sy=115MPa(16.7ksi), Su=175MPa(25.4ksi). Use the alloy's as-welded equivalent yield strength unless detailed proof establishes correct solution treatment/artificial aging and absence of later manufacturing heating. Postweld strengthening evidence identifies the facility, process and fixturing.",
        ["Only if aluminum is selected; no aluminum material or heat-treatment waiver is selected."],
        "Audit material provenance, thicknesses, manufacturing records, reference-property use and every equivalence mode. If claiming postweld strength recovery, review complete treatment/fixturing/thermal history evidence.",
        ["Aluminum applicability, alloy, welding and heat-treatment choices TBD."]),
    obligation("ROLLOVER", "Provide hoops and restrained helmet clearances", numbered("F.5.6", 4),
        ["rollover", "accommodate", "restrain"],
        "Provide front and main hoops triangulated into the primary structure by structural tubing. A hoop bend below its brace is triangulated at a side-view node or is strictly less than25mm from the applicable F.7.8 attachment. With the restraint installed, both the95th-percentile male and every team driver's helmet have at least50mm clearance from the line joining hoop tops and, for rearward main bracing, at least50mm from the line from main-hoop top to brace lower end. With forward main bracing, the helmet lies no farther rearward than the main hoop's rear surface.",
        ["Brace direction controls the helmet envelope; the alternative bend treatment must be explicitly selected."],
        "Inspect hoops and load-path triangulation, measure each bend/attachment and restrained helmet clearance for the anthropometric template and all team drivers in the selected bracing configuration.",
        ["Hoop geometry, brace direction, bend nodes, attachment type and driver envelopes TBD."], ["F.7.8"]),
    obligation("DRIVER-TEMPLATE", "Use the prescribed template and inspection position", ["F.5.6.5", "F.5.6.6"],
        ["accommodate", "rollover", "head"],
        "Represent hips/buttocks and shoulder/cervical regions by two200mm circles and helmeted head by a300mm circle, with tangent neck lines. Centers of the200mm circles are490mm apart; shoulder-to-head centers are280mm apart. Place the seat rearmost and pedals most forward; the lower-circle center is at least915mm from the pedals' rearmost face, the middle circle rests on the seat back and the head circle is no more than25mm from the head restraint in the driving helmet location.",
        ["This inspection template does not replace the full required driver population."],
        "Construct and dimension-check the prescribed template. Record its placement with seat/pedal extremes and verify915mm minimum and25mm maximum gaps before evaluating helmet geometry.",
        ["Seat/pedal adjustment ranges, head-restraint adjustment and physical template evidence TBD."]),
    obligation("FRONT-HOOP", "Constrain front-hoop material and geometry", numbered("F.5.7", 8),
        ["rollover", "accommodate"],
        "Use closed metal tubing of the prescribed class for a front hoop running up from the bottom frame member on one side and down to the bottom member on the other. Multiple pieces require correct triangulation. Its top is at least as high as the steering-wheel top in every angular position. Its rear surface is at most250mm forward of the wheel rim's forwardmost surface, measured horizontally on centerline with steering straight. Above the upper side-impact structure, side-view inclination is strictly below20degrees from vertical. A nonsteel hoop has an accessible4mm inspection hole. Avoid concave top-perimeter sections as the source recommendation.",
        ["Front hoop material remains unselected; the nonsteel inspection-hole condition is retained."],
        "Inspect class/material, multi-piece triangulation and lower endpoints; sweep wheel positions, measure centerline distance and hoop inclination, and inspect the conditional hole and recommended top profile.",
        ["Front hoop material, tube shape and wheel/hoop layout TBD."]),
    obligation("MAIN-HOOP", "Provide the continuous steel main hoop and its bracing", numbered("F.5.8", 4) + numbered("F.5.9", 7),
        ["rollover", "loadpath"],
        "Make the main hoop one uncut continuous closed steel tube of classA, spanning the lowest frame members or monocoque bottom. Above the upper side-impact attachment its side-view inclination is strictly below10degrees from vertical; below it, forward inclination is unrestricted but rearward inclination is at most10degrees. At the bottom-major-structure attachments, front-view inside spacing is at least380mm. Provide left and right straight, unbent closed steel classB braces: forward when the hoop leans forward, rearward when it leans rearward, either direction if vertical. Attach no more than160mm below the hoop top, preferably as near its top as possible, at an included angle of at least30degrees. Securely integrate braces to carry all hoop loads to the major structure without failure.",
        ["Mandatory steel applications cannot be replaced with the permitted aluminum alternatives for other members."],
        "Inspect continuous steel construction, class and brace count; measure angles, spacing and attachment height. Substantiate integration and full load transfer using the applicable chassis/bracing support and joint rules.",
        ["Main hoop/brace geometry, joining method, support design and load evidence TBD."], ["F.6.6", "F.7.7", "F.5.12", "F.5.13"]),
    obligation("SIDE-TUBE", "Retain the tube-frame side-impact alternative", numbered("F.6.4", 6),
        ["side", "loadpath"],
        "For a tube-frame side-impact structure use closed tubing of the applicableB orD class, with at least three members on each side of the seated driver. Multiple pieces require straight unbent tubes and correct triangulation. The upper member joins the main and front hoops with its entire top edge in a ground-parallel290–345mm band above the lower member's lowest bottom point. The lower member joins both hoop bottoms. A diagonal ahead of the main hoop and behind the front hoop connects upper/lower members and completely triangulates their bays.",
        ["Conditional tube-frame alternative; monocoque or hybrid choice remains unresolved."],
        "Inspect member count/class and straight multi-piece joints; measure the complete upper-edge band, hoop connections and bay triangulation for each side.",
        ["Chassis construction, side-impact member sections, bends and nodes TBD."]),
    obligation("SIDE-MONOCOQUE", "Retain monocoque side-impact equivalence and physical tests", numbered("F.7.5", 5),
        ["side", "loadpath"],
        "For a monocoque define the side-impact zone between hoops as at least290mm of vertical height above the floor bottom plus half the horizontal floor. Establish equivalence to three classB steel tubes. The vertical portion from floor bottom to345mm above the floor's lowest bottom point is equivalent to at least two classB steel tubes and has no side-view openings between hoops. Calculate horizontal-floor equivalence underF.4.3. For a25mm-diameter laminate section show perimeter shear of at least7.5kN by the F.4.2.5 physical test and include results in the SES.",
        ["Conditional monocoque alternative; no fabricated laminate strength is asserted."],
        "Review the selected side/floor geometry and F.4.3 equivalence calculations, inspect absence of vertical-side openings and obtain the specified laminate perimeter-shear test/SES records.",
        ["Laminate/core geometry, equivalence data, coupons and SES evidence TBD."], ["F.4.2.5", "F.4.3"]),
    obligation("FRONT-STACK", "Place the attenuator and AIP in a protected load path", numbered("F.8.1", 2),
        ["front", "loadpath"],
        "Locate the impact attenuator ahead of the front bulkhead with an anti-intrusion plate between them. Both attenuator-to-AIP and AIP-to-bulkhead attachments carry transverse and vertical loads from off-axis impacts.",
        ["All attenuator/AIP construction and attachment variants."],
        "Review the physical stack and establish transverse/vertical load paths at both attachments under off-axis impact cases.",
        ["Front bulkhead, AIP, attenuator and off-axis load-path design TBD."]),
    obligation("AIP", "Retain AIP material and attachment alternatives", numbered("F.8.2", 3),
        ["front", "loadpath"],
        "Choose a solid steel AIP at least1.5mm thick, solid aluminum at least4.0mm thick, or a composite meetingF.8.3. For welding, the profile reaches or exceeds the bulkhead tube centerline on every side; for bolting/bonding/laminating it reaches or exceeds the complete outer bulkhead periphery. Document direct attachment in SES. Weld lengths are at least25mm with interrupted weld/space ratio at least1:1. Bolting uses at least eight critical fasteners at least8mm or5/16in diameter, with bolt centers at least50mm apart, and15kN pullout/bending capacity per attachment. Tearout is15kN per bolt or total mounting tearout exceeds the F.8.5.3.b attenuator attachment strength. Bonding requires no bulkhead openings, bonding over the AIP's entire surface and shear/peel strengths strictly above120kN. Laminating places the AIP ahead of the outer bulkhead skin, encloses it fully and provides shear capability strictly above120kN.",
        ["Material and attachment alternatives are constraints awaiting a decision, not multiple simultaneously selected designs."],
        "Select material/attachment, measure the corresponding profile/thickness/fastener/weld geometry and substantiate conditional strength and SES documentation for the selected method. Record the rejected alternatives with rationale.",
        ["AIP material, dimensions and attachment method TBD."], ["T.8.2", "F.8.5.3"]),
    obligation("AIP-COMPOSITE", "Retain composite AIP strength and allowed substantiation paths", numbered("F.8.3", 2),
        ["front", "loadpath"],
        "A composite AIP must survive frontal impact and withstand at least120kN static load distributed over the minimum200mm by100mm attenuator area. Verify either by physical testing attached to a representative intended chassis section with fixture equivalent to the baseline bulkhead or the first50mm of the chassis (data valid only one competition year), or by F.4.2.2/F.4.2.5 laminate tests with three-point-bending and perimeter-shear calculations.",
        ["Only if composite AIP is selected."],
        "Select and substantiate one allowed proof route; check load area, representative fixture and one-year validity for physical tests, or the referenced laminate tests plus both required calculations.",
        ["Composite AIP selection, laminate/test data and proof route TBD."], ["F.4.2.2", "F.4.2.5"]),
    obligation("IA-SELECTION", "Retain standard/custom attenuator and bulkhead-support alternatives", numbered("F.8.4", 3),
        ["front", "loadpath"],
        "Select an approved event-site standard attenuator or design/test a custom one. A custom IA has fore/aft length at least200mm and height at least100mm by width at least200mm for at least200mm ahead of the bulkhead. Bond all segmented foam pieces against sliding/parallelogram distortion; place continuous panels between segments of multisegment honeycomb. If the bulkhead outside profile exceeds400mm by350mm or standard honeycomb is used, provide a full-opening diagonal/X support of classC or equivalent for monocoques, with each end carrying at least30kN in any direction, or perform F.8.8.6/.7 tests proving permanent AIP deflection is at most25mm.",
        ["Standard/custom, foam/honeycomb/other, bulkhead profile and support/test alternatives remain unresolved."],
        "Pin the event-site approval/specification if standard is chosen; otherwise inspect custom dimensions/segments and obtain applicable tests. Evaluate the bulkhead/honeycomb trigger and establish the selected support or physical-test alternative.",
        ["Exact approved standard IA or custom construction, bulkhead size and additional support/test choice TBD."], ["F.2.3.1", "F.8.8.6", "F.8.8.7"]),
    obligation("IA-ATTACHMENT", "Constrain attenuator attachment and position", numbered("F.8.5", 7),
        ["front", "loadpath", "pedal"],
        "Document IA attachment in SES. Standard/custom foam or honeycomb attaches by bonding; custom other construction may bond, weld or bolt. Bonds satisfyF.5.5 and shear strength strictly exceeds every applicable threshold:95kN for foam,38.5kN for honeycomb, and the maximum compressive force for a custom IA. Bond all foam surface; only precrushed honeycomb area counts for bond equivalence. Welds may be continuous/interrupted; interrupted ratio is at least1:1 and each length is strictly greater than25mm. Bolting directly to primary structure uses at least eight critical fasteners of at least8mm or5/16in diameter, center separation at least50mm and each attachment15kN pullout/tearout/bending. Bottom leading edge is at most175mm above the lower side-impact structure's lowest bottom point. A custom IA has an area at least200mm long and200mm wide intersecting a ground-parallel plane at or below that175mm limit. Center IA laterally; standard honeycomb orientation is200mm wide by100mm high; standard foam may be lateral or vertical.",
        ["IA/AIP choices remain unresolved. Preserve the different inequalities: AIP weld length >=25mm versus IA weld length >25mm."],
        "For the chosen IA variant inspect the allowed attachment, strength/provenance, weld/bolt/bond details, low-mounted area and orientation; include the exact method in SES.",
        ["Attenuator type, attachment strengths, mounting height and orientation TBD."], ["F.5.5", "T.8.2"]),
    obligation("FRONT-OBJECTS", "Prevent front intrusion and preserve pedal clearance", numbered("F.8.6", 3),
        ["front", "pedal", "barrier"],
        "Ahead of the AIP in front view permit only IA, fastener heads and lightweight bodywork/nosecones; rearward nuts are recommended. Front wing/mounts must move entirely aft of the AIP without contacting the front bulkhead in a frontal impact; their attachment locations aft of AIP are recommended and tabs extend at most25mm ahead of it. At every pedal travel/adjustment position maintain at least25mm clearance to the AIP rear face, all front-bulkhead structure and all noncrushable items in primary structure, including batteries/master cylinders/reservoirs.",
        ["Optional wing/bodywork installations trigger their attachment/motion conditions; pedal adjustment sweep is mandatory."],
        "Inventory front-view objects, inspect tab protrusion and evaluate wing/mount crash movement if present. Sweep pedal adjustments/full travel and measure clearance to every protected structure/noncrushable object.",
        ["Front packaging, aerodynamic devices, adjustment ranges and collision/motion evidence TBD."], ["F.6.1", "F.7.2", "F.8.4.3"]),
    obligation("FRONT-FORCE", "Combine attenuator and other frontal-object forces", numbered("F.8.7", 5),
        ["front", "loadpath"],
        "The combined IA assembly force and force to crush/detach other objects ahead of AIP cannot exceed the F.8.8.2 peak deceleration; leave out outboard wheel assemblies and any lightweight bodywork or nosecone. Use peak loads95kN for standard foam,60kN for standard honeycomb or the measured peak for a tested IA. Demonstrate the total by testing IA and noncrushable objects together, or testing objects alone and adding the specified IA peak; alternatively calculate mounting failure from link buckling, fastener shear or tearout and add the corresponding IA peak.",
        ["The applicable material-dependent peak must be selected after IA choice, not combined or universally substituted."],
        "Inventory included/excluded frontal objects and select an allowed test/calculation route. Check each failure mode and the correctly matched attenuator peak against the functional deceleration reference.",
        ["Front objects/mount failures, IA peak, proof route and combined force evidence TBD."]),
    obligation("IA-PERFORMANCE", "Keep the functional impact reference separate from test criteria", ["F.8.8.1", "F.8.8.2"],
        ["front", "loadpath"],
        "Include an IAD report in SES. At the rule's reference total vehicle mass300kg and impact speed7.0m/s, the IA functional requirement is average deceleration at most20g, peak at most40g and absorbed energy strictly above7350J. The source explicitly identifies these as functional requirements rather than test requirements. This does not assign a300kg design mass to the actual car.",
        ["Reference impact conditions are preserved, while actual vehicle mass and the substantiation method remain separate design/evidence decisions."],
        "Review the IAD functional demonstration and its mass/speed/energy/deceleration derivation; assess test interpretation under the remaining F.8.8 clauses before claiming acceptance.",
        ["Actual vehicle design mass, IA performance and complete IAD evidence TBD."]),
    obligation("IA-EVIDENCE", "Plan the applicable impact report, fixture, conduct and analysis", numbered("F.8.8", 8)[2:],
        ["front", "inspect"],
        "For standard IA, omit test-data submission but include all other section requirements, photos of the actual IA and evidence against its specification (supplier receipt/packing slip is allowed). Nonstandard IAD includes functional test data, energy/deceleration calculations, test schematic and photos annotated with pre/post heights; IA testing is valid only one competition year. During tests use intended IA-to-AIP attachment and a representative chassis section/fixture equivalent in strength/stiffness to baseline bulkhead, not a solid bulkhead-shaped block. Leave at least50mm fixture clearance behind AIP; permanent deflection anywhere is at most25mm beyond its pretest position. Composite IA requires dynamic testing; other constructions may use dynamic or quasi-static. Dynamic tests occur at a dedicated facility under professional/faculty supervision, not a team-built dynamic apparatus; quasi-static tests may use university facilities with the source's due-care advice retained. Calculate average dynamic deceleration from raw unfiltered data. If peaks exceed40g, the permitted filter is CFC60(100Hz) underSAEJ211 or a100Hz third-order low-pass Butterworth(-3dB at100Hz).",
        ["Standard/custom and composite/noncomposite alternatives determine the evidence obligations; no test or approval is performed by model creation."],
        "Develop the applicable IAD package; review year validity, intended assembly and representative fixture, clearances/deflection, facility/supervision and test data. Reproduce average from raw data and document any allowed filtering of peaks.",
        ["IA selection, dedicated facility, fixtures, current-year test data and official standard specification TBD."], ["F.8.8.2"]),
    obligation("COCKPIT-OPENING", "Keep the official cockpit opening gauge path accessible", numbered("T.1.1", 4),
        ["accommodate", "inspect"],
        "Pass the exact page56 cockpit-opening gauge horizontally/parallel to ground, inserted vertically from above intervening structure/bodywork between hoops, allowing fore/aft translation, until both at least25mm below the side-impact top's lowest point and at most320mm above the cockpit's lowest point. Gauge drawing dimensions are600mm overall fore/aft span, front transverse400mm, rear transverse350mm,100mm rear bevel offset, and75/100mm top-profile offsets; use the actual official outline rather than treating those dimensions as a rectangular substitute. Wheel/column/seat/padding removal is permitted, but retain the firewall; retain shifter/shift/clutch unless integral with the removed wheel. Routing cannot obstruct the gauge. In practice the column remains and may be maneuvered around, never around fixed supports; an optional front-center column slot is allowed.",
        ["Gauge outline and orientation are checked visually against the source drawing; T.1.2 internal cross-section remains outside this bounded interpretation."],
        "Construct the exact official gauge and document permitted/forbidden removals, unchanged firewall, routing and fixed supports. Demonstrate insertion to both limits without replacing the prescribed geometry.",
        ["Cockpit opening/bodywork geometry, removable equipment and physical gauge demonstration TBD."], ["F.6.4", "F.7.5.1", "T.1.2"]),
    obligation("COCKPIT-PROTECTION", "Contain the occupant and shield moving/sharp hazards", numbered("T.1.3", 4) + ["T.1.5.1"],
        ["accommodate", "barrier", "loadpath"],
        "Contain hips entirely within major chassis structure. With feet on pedals, feet/legs ahead of the front hoop stay inside/below structural tubing or equivalent in front/side views. Shield every moving suspension/steering part and sharp edge in the cockpit from the front hoop to a plane100mm behind pedals using solid material, including springs, dampers, rockers, antiroll bars, rack and column CV joints; make covers removable for mounting-point inspection. Protect the seat by keeping its lowest point no lower than the top of the lowest structural tube (or equivalent) in side view, or by a longitudinal classB side-impact tube below it.",
        ["Seat protection alternatives remain unresolved; shields are functional hazards barriers, not permanent inspection obstructions."],
        "Check hips/feet/legs for all drivers and pedal settings, inspect the full hazard zone and removable shields, and measure the selected seat-protection alternative.",
        ["Cockpit fit, hazard shields, seat and protection layout TBD."]),
    obligation("FLOOR", "Exclude track debris with a continuous floor closeout", numbered("T.1.7", 4),
        ["barrier"],
        "Provide a solid nonbrittle floor closeout from the feet to the firewall to keep track debris out; gaps between multiple panels are at most3mm.",
        ["All vehicles; panel count and material remain unresolved."],
        "Inspect material, foot-to-firewall coverage and every panel gap; demonstrate debris exclusion and attachment integrity under the selected environment.",
        ["Floor material, joints and attachment design TBD."]),
    obligation("FIREWALL", "Isolate occupants and harness from fluids, batteries and TS", numbered("T.1.8", 6),
        ["barrier", "accommodate"],
        "Separate the driver compartment and every harness portion from fuel-system components if present, engine oil if present, liquid cooling and lithium batteries; for EV additionally separate all TS components except the specified outboard wheel motors and their cables at wheels/front arms. Extend the firewall so no body point less than100mm above the tallest driver's helmet bottom has direct sight to those hazards. Use a tightly mounted rigid nonflammable impermeable surface. For EV/lithium batteries, the battery/TS-side firewall layer is aluminum (not aluminum tape) and grounded perEV.7.7; the tractive battery container cannot serve as firewall. Seal the whole surface, edges, joints, passthroughs and floor against fluids; overlap/seal multi-panel joints, keep inter-firewall sealing unstressed, use passthrough grommets and environmentally rated sealants/adhesives.",
        ["Human-driven EV baseline; conditional oil/fuel/cooling/lithium hazards must be inventoried, not silently declared absent.", "Only the specific outboard-motor/cable exception is retained; it does not exempt the rest of TS."],
        "Inventory all hazards/harness paths and inspect sightlines for tallest driver, layers, ground path, container separation, mounts and complete seals/grommets. Substantiate material flammability/permeability and environmental ratings.",
        ["Firewall geometry, aluminum layer/grounding, fluids/battery/cooling inventory and seals TBD."], ["EV.4.1.3", "EV.7.7"]),
    obligation("HARNESS", "Retain certified harness and posture-dependent alternatives", ["T.2.1"] + numbered("T.2.2", 6) + numbered("T.2.3", 3),
        ["restrain", "accommodate"],
        "Select a5-,6- or7-point harness certified to SFI16.1, SFI16.5 orFIA8853/2016. Keep original labels/specification/expiration, use within the labeled expiration year (expiry on/before31December of the competition calendar year is permitted), keep new/like-new condition without wear/cuts/chafing, follow manufacturer installation/threading and never modify supplied hardware. Five points are two lap/two shoulder/one antisubmarine belt; six use two lap/two shoulder/two leg or antisubmarine belts; seven add a negative-g/Z belt. Template seat-back angle at most30degrees is upright, strictly above30degrees is reclined; the chest-to-groin line follows shoulder belts to release buckle in side view. Reclined requires6/7 points and either quick-adjust antisubmarine belts or two installed sets. All lap belts have tilt-lock quick adjusters; pull-up over pull-down is recommended. Use separate over-shoulder straps, neverY straps; H configuration is allowed.",
        ["Posture, exact harness and certificate/specification remain unselected. Belt mount strength/angles are not claimed by this specification-only group."],
        "Select posture and harness, audit original manufacturer certification/expiration/condition/instructions/unmodified hardware, count belt paths and inspect required adjusters/strap geometry. Review all referenced mounting clauses separately.",
        ["Driving posture, harness model, expiration, manufacturer documentation and mounting design TBD."], ["T.2.4", "T.2.5", "T.2.6", "T.2.7"]),
    obligation("HEAD", "Provide and substantiate adjustable head restraint", numbered("T.2.8", 7),
        ["head", "restrain", "rollover"],
        "Limit rearward head movement with a vertical/near-vertical head restraint entirely inside the rollover envelope or permitted head-restraint protection. Do not attach other items to its material/structure or place items25mm or less away. Attach near top and bottom with strength at any load point strictly above900N rearward and300N lateral/vertical. For every driver, helmet rear gap is at most25mm and contact at least50mm from every edge. Approximate100mm longitudinal adjustment or multiple thicknesses is a source accommodation suggestion. Padding satisfiesSFI45.2 or specified blueCONFOR CF45/CF45M underFIAList17, is at least38mm thick and15cm wide, and has either area at least235cm^2 plus total height adjustment at least17.5cm or height at least28cm. Cover it with thin flexible material and an approximately20mm inspection hole on a nonfront surface.",
        ["All drivers; material/area-adjustment versus fixed-height alternative and protection-envelope choice remain unresolved."],
        "Inspect approved padding/cover/hole, containment and item exclusion; measure fit for all drivers and the selected geometry alternative. Substantiate mounts under loads applied anywhere with the strict strength inequalities.",
        ["Head-restraint material, geometry/adjustment, mounts, proof loads and envelope alternative TBD."], ["F.1.13", "F.5.10"]),
    obligation("ROLL-PADDING", "Pad all helmet-contactable chassis members", ["T.2.9"],
        ["rollover", "head"],
        "Cover every roll bar, brace or chassis portion a driver's helmet could touch with at least12mm padding meetingSFI45.1 orFIA8857-2001.",
        ["Contact envelope for all required drivers and operating/incident attitudes; uncontactable areas need a substantiated exclusion."],
        "Sweep helmet contact envelopes and inspect complete padding coverage, thickness and material specification.",
        ["Helmet envelope, padding selection and coverage evidence TBD."]),
    obligation("BRAKE-CORE", "Maintain hydraulic stopping under a circuit failure", numbered("T.3.1", 10),
        ["brake-demand", "hydraulic-a", "hydraulic-b", "friction", "protect-brakes"],
        "Provide brakes on all four wheels operated by a single control and able to lock all four. Use two independent hydraulic circuits; leakage/failure at any point leaves effective braking on at least two wheels. Each circuit has a separate fluid reserve via separate reservoirs or an OEM-style reservoir. A single brake on a limited-slip differential is permitted, not selected. Prohibit brake-by-wire and unarmored plastic lines. Use scatter shielding against drivetrain failure and minor collisions. Sprung brake components in side view do not project below the chassis lower surface. Treat every brake-system fastener as critical underT.8.2.",
        ["Circuit wheel assignment and differential versus wheel-brake hardware remain unresolved; a common electronics/regen path cannot replace hydraulic braking.", "Separate reserves in an OEM-style housing do not waive hydraulic circuit independence."],
        "Inspect single mechanical control, hydraulic separation/reserves/materials, critical fasteners, shielding and sprung packaging. Demonstrate four-wheel lock and inject every credible leak/failure to prove effective braking on at least two wheels.",
        ["Circuit split, reservoirs, cylinders/calipers/differential, lines, shields, fasteners and performance evidence TBD."], ["T.5.2", "T.8.2"]),
    obligation("PEDAL", "Protect mechanical pedal strength and operation", numbered("T.3.2", 3),
        ["pedal", "brake-demand", "hydraulic-a", "hydraulic-b"],
        "Pedal material/manufacture is fabricated steel/aluminum or machined steel/aluminum/titanium. Pedal and associated parts withstand at least2000N without failure of brakes, box, chassis mount or adjustment. The source explicitly says this is not a design criterion and allows seated-official maximum-force pedal testing; a2000N calculation alone is not accepted as completed design verification. Failure of non-loadbearing brake/box parts must never obstruct pedal or brake operation.",
        ["Material and fabrication route are coupled alternatives; fabricated titanium is not an allowed listed option."],
        "Review selected material/process, load path and hazards from non-loadbearing failures. Substantiate the minimum strength and inspection pedal-test conditions with documented test/analysis evidence, not an invented universal design margin.",
        ["Pedal fabrication/material, box/mount/adjustment, human force envelope and proof evidence TBD."]),
    obligation("BRAKE-REGEN", "Keep regenerative braking optional and hydraulic actuation direct", ["T.3.2.4"],
        ["brake-demand", "friction"],
        "For this EV the first90percent of pedal travel may regenerate without hydraulic actuation; it is an option. The remaining travel directly operates hydraulics, and regeneration may remain active. No90percent deadband or regeneration strategy is selected until the complete brake/powertrain design is reviewed.",
        ["If regeneration is used, coordinate the low-speed inhibition and brake/accelerator plausibility in the separately owned driver-demand slice."],
        "Decide whether and how regen is used; sweep pedal travel and prove direct hydraulic actuation in the remaining range even with disabled/failed regeneration. Review the referenced EV regenerative braking and conflict safeguards.",
        ["Regen adoption, pedal transition, hydraulic timing and cross-slice interface contract TBD."], ["EV.4.6", "EV.3.3.3", "EV.4.7"]),
    obligation("BOTS", "Open and retain the direct overtravel shutdown contact", numbered("T.3.3", 5),
        ["overtravel", "pedal"],
        "Travel beyond the normal brake-pedal range operates BOTS. Use a mechanicalSPST two-position push-pull, push-rotate or flip switch that holds OFF; OFF opens the EV shutdown circuit. The driver cannot reset it. Implement the circuit with analog components, excluding programmable logic/ECU/similar digital controllers. The model reuses the shutdown slice's physical BOTS contact rather than adding a parallel or duplicate contact.",
        ["Human-driven EV: EV.8.2.2 applies; the published IC reference is retained as nonapplicable to this electric baseline."],
        "Force brake overtravel, inspect mechanicalSPST/contact holding and analog/direct wiring, prove the installed series shutdown opens and verify no seated/restrained driver can reset the contact.",
        ["Overtravel threshold, normal travel/adjustment limits, switch rating/location and reset-access geometry TBD."], ["EV.8.2.2"]),
    obligation("BRAKE-LIGHT", "Signal braking with the required rear visibility and geometry", numbered("T.3.4", 4),
        ["brake-light", "brake-demand"],
        "Provide a rear brake lamp plainly visible in very bright sunlight. It is red on black, rectangular/triangular/near-round with lit area at least15cm^2, vertically between wheel centerline and driver's shoulder and approximately on lateral centerline. Undiffused LEDs are at most20mm apart; a single LED line is at least150mm long. These alternatives do not override minimum lit area/visibility.",
        ["Lamp shape, LED/diffuser technology, brightness and trigger circuit remain unresolved."],
        "Inspect color/background, shape/lit area and installation heights/centerline. Apply the conditional LED spacing/line-length checks and demonstrate visibility from rear in very bright sunlight with the chosen brake activation logic.",
        ["Exact lamp, wiring/activation, diffuser/LED layout and sunlight visibility evidence TBD."]),
]


def groovy(value):
    if isinstance(value, dict):
        return "[" + ",".join(groovy(str(k)) + ":" + groovy(v) for k, v in value.items()) + "]"
    if isinstance(value, list):
        return "[" + ",".join(map(groovy, value)) + "]"
    if isinstance(value, str):
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "\\r").replace("\n", "\\n") + "'"
    return json.dumps(value)


def readable(text):
    text = re.sub(r"(?<=[a-z])(?=\d)", " ", text)
    text = re.sub(r"(?<=[a-z])(?=(?:F|T|EV)\.\d)", " ", text)
    text = re.sub(r"(?<=\d)(?=(?:mm|GPa|MPa|kN|kg|Hz|cm|ksi|in|N|J|g|m/s|degrees|percent)\b)", " ", text)
    for old, new in [("classA", "class A"), ("classB", "class B"), ("classC", "class C"), ("classD", "class D"),
                     ("mechanicalSPST", "mechanical SPST"), ("SFI16", "SFI 16"), ("SFI45", "SFI 45"),
                     ("FIA8853", "FIA 8853"), ("FIA8857", "FIA 8857"), ("FIAList", "FIA List"),
                     ("blueCONFOR", "blue CONFOR"), ("SAEJ211", "SAE J211"), ("CFC60", "CFC 60")]:
        text = text.replace(old, new)
    return text


def build(private_source):
    pdf = private_source.with_name("FSAE_Rules_2027_V1.pdf")
    assert pdf.is_file() and hashlib.sha256(pdf.read_bytes()).hexdigest() == SOURCE_HASH, "Pinned source PDF mismatch"
    inventory = json.loads((RULES / "normalized-clauses.json").read_text(encoding="utf-8"))
    raw = {r["clause_id"]: r["text"] for r in json.loads(private_source.read_text(encoding="utf-8"))}
    records = inventory["clauses"]
    selected_rules = {rule for group in GROUPS for rule in group["source_rules"]}
    assert len(selected_rules) == sum(len(group["source_rules"]) for group in GROUPS), "Overlapping rule ownership"
    rows = [r for r in records if r.get("source_rule_id") in selected_rules]
    assert selected_rules == {r["source_rule_id"] for r in rows}, "Missing exact rule ID"
    for row in rows:
        assert row["source_sha256"] == SOURCE_HASH
        assert hashlib.sha256(raw[row["clause_id"]].encode("utf-8")).hexdigest() == row["source_text_sha256"], row["clause_id"]
    groups = []
    for original in GROUPS:
        group = dict(original)
        for field in ("statement", "verification_obligation"):
            group[field] = readable(group[field])
        for field in ("conditions_and_alternatives", "unresolved_decisions"):
            group[field] = [readable(s) for s in group[field]]
        group["source_clauses"] = [{field: row[field] for field in ("clause_id", "source_rule_id", "source_pages", "source_sha256", "source_text_sha256", "numeric_facts")}
                                   for row in rows if row["source_rule_id"] in group["source_rules"]]
        group.update(interpretation_status="bounded_source_interpretation_requires_design_review", coverage_status="draft_not_applied", verification_status="not_verified", design_status="unresolved")
        groups.append(group)
    report = dict(schema_version=1, slice_id=SLICE, source_document_id="FSAE-2027-V1", source_sha256=SOURCE_HASH,
                  source_record_count=len(rows), selected_rule_count=len(selected_rules), source_pages=sorted({p for r in rows for p in r["source_pages"]}),
                  source_scope_rules=sorted(selected_rules), obligations=groups,
                  visual_checks={"26": "F.3.2 application and F.3.4 steel tables", "32": "rollover inequalities", "33": "driver template dimensions and placement", "47": "IA position/front objects", "56": "cockpit opening gauge exact outline", "66": "brake pedal options, BOTS and lamp dimensions"},
                  source_figure_provenance=[dict(source_rule_id="T.1.1.1", source_pages=[56], source_sha256=SOURCE_HASH, status="official_gauge_outline_visually_checked_not_a_new_rectangle")],
                  completion_status="bounded_candidate_with_unresolved_construction_and_evidence", verification_status="not_verified",
                  exclusions=["This is not full V/F/T, full vehicle safety or full event-rule coverage.", "F.4 composite testing, other F.5 joints/braces, F.6/F.7 bulkheads/supports, T.1.2 cross-section, T.1.6 thermal protection and T.2.4–T.2.7 harness mounting remain dependent unmodeled work.", "Driver-demand owns APPS/BSE/EV.4.7/BSPD; shutdown owns EV.8 and the shared BOTS physical series contact.", "Event-site anthropometry, approved IA and specification/FAQ inputs require official pinning before a hardware choice."])
    (HERE / "obligations.json").write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    # This is a planned source-scope delta, not a claim about the current native session.
    scope_sets, rule_sets, derived_counts = {}, {}, {}
    for name, filename, source_field in [("shutdown", "shutdown-obligations.json", "source_clauses"),
                                          ("driver-demand", "obligations.json", "source_refs")]:
        peer = json.loads((HERE.parent / name / filename).read_text(encoding="utf-8"))
        references = [source for group in peer["obligations"] for source in group[source_field]]
        assert peer["source_sha256"] == SOURCE_HASH
        scope_sets[name] = {source["clause_id"] for source in references}
        rule_sets[name] = {source["source_rule_id"] for source in references}
        derived_counts[name] = len(peer["obligations"])
    before = set.union(*scope_sets.values())
    scope_sets["vehicle-protection"] = {r["clause_id"] for r in rows}
    rule_sets["vehicle-protection"] = selected_rules
    derived_counts["vehicle-protection"] = len(groups)
    after = set.union(*scope_sets.values())
    total = len(records) + len(inventory.get("source_objects", []))
    rule_inventory = json.loads((RULES / "rule-inventory.json").read_text(encoding="utf-8"))
    rules_with_paragraphs = {r["source_rule_id"] for r in records}
    delta = dict(schema_version=1, source_sha256=SOURCE_HASH, status="expected_after_all_native_slice_batches_and_structure_checks_pass",
                 counting_basis="Distinct exact source-record IDs assigned to candidate slices; not physical verification or full semantic review",
                 numbered_rule_paragraphs=len(records), unnumbered_source_objects=len(inventory.get("source_objects", [])), total_source_records=total,
                 inventory_rule_nodes=len(rule_inventory["rule_nodes"]), rule_ids_with_direct_paragraphs=len(rules_with_paragraphs),
                 slices={name:dict(source_records=len(scope_sets[name]), source_rule_ids=len(rule_sets[name]), derived_obligations=derived_counts[name]) for name in scope_sets},
                 previous_candidate_scope_records=len(before), expected_candidate_scope_records=len(after), additional_protection_records=len(after - before),
                 protection_overlap_with_previous=len(before & scope_sets["vehicle-protection"]), source_records_outside_three_slice_scope=total - len(after),
                 expected_selected_source_rule_ids=len(set.union(*rule_sets.values())), expected_derived_obligations=sum(derived_counts.values()),
                 physical_verification_status="not_verified", compliance_status="not_established",
                 interpretation_note="Protection uses a scoped bounded interpretation and leaves global source statuses intact; conditional construction and referenced dependent rules still require design review.")
    (HERE / "expected-scope-delta.json").write_text(json.dumps(delta, indent=2) + "\n", encoding="utf-8")
    # Numeric facts remain in the public provenance JSON. Embed only needed execution metadata.
    execution_groups = []
    for group in groups:
        g = {key: group[key] for key in ("key", "identifier", "title", "model_keys", "statement", "conditions_and_alternatives", "verification_obligation", "unresolved_decisions", "dependent_source_rules")}
        g["source_clauses"] = [[s["clause_id"].removeprefix("FSAE-2027-V1:"), s["source_text_sha256"]] for s in group["source_clauses"]]
        execution_groups.append(g)
    verify_groups = [{key: g[key] for key in ("key", "identifier", "model_keys", "source_clauses")} for g in execution_groups]
    structure_outputs = ["30a-components.groovy", "30b-functions.groovy", "30c-exchanges.groovy", "30d-unallocate-structure.groovy",
                         "30e-unallocate-cockpit.groovy", "30f-unallocate-brakes.groovy", "30g-unallocate-status.groovy", "30h-finish-structure.groovy"]
    batches = [("30-model-vehicle-protection.template.txt", output, [], phase, 0, 0) for phase, output in enumerate(structure_outputs, 1)]
    batches += [("31-trace-vehicle-protection.template.txt", "31-trace-vehicle-protection-a.groovy", execution_groups[:16], 1, 0, 16),
               ("31-trace-vehicle-protection.template.txt", "32-trace-vehicle-protection-b.groovy", execution_groups[16:], 2, 16, len(groups)),
               ("40-verify-vehicle-protection.template.txt", "40-verify-vehicle-protection.groovy", verify_groups, 0, 0, 0)]
    for template, output, batch_groups, number, previous, after in batches:
        script = (HERE / template).read_text(encoding="utf-8").replace("@@GROUPS@@", groovy(batch_groups)).replace("@@SOURCE_HASH@@", SOURCE_HASH).replace("@@SOURCE_COUNT@@", str(len(rows))).replace("@@GROUP_COUNT@@", str(len(groups))).replace("@@BATCH_NUMBER@@", str(number)).replace("@@STRUCTURE_PHASE@@", str(number)).replace("@@PREVIOUS_COUNT@@", str(previous)).replace("@@AFTER_COUNT@@", str(after))
        assert "@@" not in script
        assert len(script) < 100000, "Native executor input limit"
        (HERE / output).write_text(script, encoding="utf-8")
    (HERE / "30-model-vehicle-protection.groovy").write_text("// Superseded after a native timeout; deliberately refuses a monolithic replay.\nassert false: 'Run 30a-components through 30h-finish-structure in order, then both trace batches'\n", encoding="utf-8")
    print(json.dumps(dict(source_records=len(rows), selected_rules=len(selected_rules), derived_obligations=len(groups))))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--private-source", required=True, type=Path)
    build(parser.parse_args().private_source)

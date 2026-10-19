# Driver demand and independent BSPD candidate slice

This slice turns 57 exact paragraphs from the pinned Formula SAE 2027 V1 rulebook into 15 interpreted candidate obligations. It covers selected EV.3 torque/direction/regeneration obligations, the analogue APPS/BSE variant referred to by EV.4.5/4.6, EV.4.7 brake/accelerator conflict, and all EV.8.7 BSPD paragraphs. `obligations.json` records each clause ID, document and paragraph hashes, page references, conditions, meaningful numerical facts, model owner and proposed verification work. It contains paraphrases; private source paragraphs stay outside the repository.

The scripts create a design candidate, not tested control software or a compliant vehicle. All verification remains `not_verified`, evidence `not_available`, and compliance `not_claimed`. Source coverage becomes `allocated` only for these 57 selected paragraphs after the complete atomic command succeeds. No other rule paragraph is promoted. The optional three-sensor mode, digital sensor variants, EV.3 metering/power/voltage/penalties, and full cross-reference clauses remain outside this slice.

## Apply to the disposable acceptance model

1. Open the exact Eclipse project `FSAE-2027-Electric-Arcadia`, enable Requirements Viewpoint, and run the existing architecture bootstrap and all source-inventory import batches.
2. Apply the EV.8 shutdown slice first. This script requires its OA/SA/LA/PA functions `FSAE2027:SD Accept independent shutdown trips` and `FSAE2027:SD Remove motor torque on shutdown` before making any changes.
3. Review and run `30-driver-demand.groovy` through the plugin's normal reviewed Groovy execution path. It verifies the exact platform project segment, bootstrap marker and every selected source hash, then applies one additive command. It refuses a repeated completed slice. Derived requirements are in the separate SA module `FSAE2027:DD:module`; the original source module retains its exact inventory.
4. Run `40-verify-driver-demand.groovy`. This read-only verifier checks native functions, allocations, deployments, configuration-item realizations, shutdown interfaces, state-machine containment and native satisfy/derive/verify relations. Passing it establishes model structure only. Preserve its graph/result as an acceptance record, then save and reopen the model and repeat it.

The draft adds 64 functions across OA/SA/LA/PA, 88 functional exchanges, seven child logical/physical partitions and seven HWCI items. Three explicit powertrain boundary functions distinguish speed observation, independent current observation and execution of the permitted motor command. Every DD exchange uses leaf endpoints and native exchange/port realization traces across perspectives. Physical behavior is explicitly deployed onto equipment using native PartDeploymentLinks. Separate spring, APPS channel, inspection connector, brake sensing/bias, BSPD sensor and inspection hardware nodes identify concrete design obligations. EPBS describes candidate items; no supplier, part number, rating or validated implementation is invented.

APPS/BSE faults and brake/accelerator conflict feed motor-power inhibition directly. The BSPD is a separate nonprogrammable hardware candidate that feeds the shutdown slice's independent trip input. The shutdown slice's motor-removal function also feeds the common power-inhibit gate. Five native StateMachines separately describe APPS timing, BSE timing, the brake-conflict latch, and logical/physical BSPD hardware behavior. Each machine owns exactly one initial state, matching Capella's native validation rule. Their text guards are engineering abstractions; they do not execute a controller. Global shutdown latching/manual reset is owned by the EV.8 shutdown slice.

Function availability is persisted through native `AbstractFunction.availableInStates`. The brake-conflict, APPS and BSE monitors remain available throughout their corresponding regions; the BSPD monitor remains active after a trip, and its trip-output function is available only in the trip-requested state. The derived, read-only `State.availableAbstractFunctions` feature is never mutated. The verifier checks the stored reverse links and their exact state scope.

## Repair an existing v1 candidate

An earlier slice version retained aggregate exchange endpoints and put the three independent monitor regions into one state machine. Native Capella validation rejected those patterns. For an already-created candidate, apply the shutdown slice's `35-repair-shutdown-semantics.groovy` and `36-repair-shutdown-realizations.groovy` first, then this directory's `35-repair-driver-demand-native-semantics.groovy`; do not rerun creation. The DD migration preserves existing exchanges and function-port identifiers, reparents those ports to explicit leaves, and moves existing regions with their states/transitions/guards intact into independent machines. It keeps previous native requirement traces and appends new boundary/machine owners. Repeating the migration reuses existing leaves and traces.

The acceptance project's shared bootstrap repair separately removes allocations from non-leaf aggregates and delegates its base interfaces to explicit boundary leaves. This DD migration deliberately leaves those shared allocations and base exchanges to that repair. Run the updated DD verifier and Capella's full semantic validation after all three repairs. Structure checks and native validation are distinct from physical verification.

After `../../12-normalize-interface-leaves.groovy`, review and run `../../13-realize-interface-transports.groovy` for the 11 base LA/PA interfaces. It checks that each functional endpoint is a leaf allocated to the corresponding component-port owner, then fills/reuses native PA-to-LA component-exchange and component-port realizations, 22 component-exchange-to-functional-exchange allocations and 44 component-port-to-function-port allocations. It preserves and verifies the 44 existing component/functional exchange IDs, endpoints and containers. Its return value lists actual trace IDs and created/reused counts; a repeated run should create no additional links. The script asserts stored links directly rather than editing derived allocation views. Run native semantic validation and save/reopen checks afterward. These transport mappings do not specify electrical wiring, protocols, signal units, supplier hardware or proven performance.

## Decisions and verification still required

The model retains strict source boundaries: APPS deviation `>10%` travel, APPS/BSE persistence `>100 ms`, brake conflict APPS `>25%`, conflict release APPS `<5%`, and BSPD simultaneous-condition persistence `>0.5 s`. BSPD current is expressed as `5000 W / U_nominal`; nominal voltage is unresolved. The cited 0.5/4.5 V sensor values are examples, not selected universal limits. Mechanical-brake engagement is distinct from arbitrary nonzero brake-pedal travel.

Sensor ranges/calibration, mechanical engagement, hard-braking threshold, timer tolerances, recovery after sensor faults, invalid-speed behavior, physical ratings and inspection isolation remain unresolved. Inhibited startup, conservative inclusive low-speed regeneration boundaries and conservative fault recovery are candidate engineering choices requiring review. Native guard constraints explicitly retain these uncertainties. Actual fault-injection, bench measurements, spring disconnection tests, installed BSPD testing without motor power, jack accessibility and inspection evidence must be recorded before any verification status changes.

## Reproduce and check the draft

From this directory:

```powershell
python prepare_slice.py --private-source "<local-path>"
python -m unittest -v test_driver_demand.py
```

Generation requires the private paragraph data and checks its SHA-256 values against the pinned public inventory. Edit the `.template.groovy` files and generator interpretation data, then regenerate; do not edit generated scripts independently. Compile all changed scripts with the plugin's actual secure Groovy compiler. Use creation for a fresh candidate or the additive repair for an existing candidate, then run the native verifier. Unit checks verify provenance and generation consistency, not model persistence or vehicle behavior.

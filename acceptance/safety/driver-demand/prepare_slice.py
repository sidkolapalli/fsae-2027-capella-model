"""Build a pinned, paraphrased driver-demand candidate slice; never publishes source paragraphs."""
import argparse
import hashlib
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
RULES = HERE.parents[1] / "rules"
SOURCE_HASH = "f71df5552e341dab1b58f1ff47212e7281953d66b9c41833de8347560913ed0e"

GROUPS = [
    dict(id="DD-01", title="Bound wheel torque and prevent powered reverse",
         rules=["EV.3.1.1", "EV.3.1.3"], functions=["torque-bound", "forward-only", "motor-command"], owner="driver-demand / powertrain",
         statement="Do not command motor power for reverse driving. Any torque-adjusting controller may reduce total driver-requested wheel torque, but must not increase it.",
         conditions=["Human-driven electric baseline; any algorithm or ECU able to adjust wheel torque."], numeric=[],
         checks="Review every torque-adjusting path and reverse command path. Bench-sweep driver requests, limiting interventions and direction requests; confirm requested total torque is never increased and powered reverse is rejected."),
    dict(id="DD-02", title="Inhibit low-speed energy regeneration",
         rules=["EV.3.3.3"], functions=["regen-inhibit", "vehicle-speed"], owner="powertrain",
         statement="Suppress regeneration in the source's 0 to 5 km/h vehicle-speed interval. Boundary treatment and speed-signal validity require design review; this candidate conservatively includes both endpoints.",
         conditions=["Regenerative powertrain operation; speed measurement and invalid-speed behavior remain engineering decisions."],
         numeric=[dict(quantity="speed interval", lower=0, upper=5, unit="km/h", role="source interval; inclusive endpoints are a conservative candidate interpretation")],
         checks="Review interval endpoint interpretation. Bench-test below, at and above 0 and 5 km/h, including invalid speed input, and measure absence of regenerative energy within the approved inhibited interval."),
    dict(id="DD-03", title="Mechanical pedal return independent of APPS internals",
         rules=["T.4.2.1"], functions=["pedal-return", "apps-input"], owner="driver-demand pedal assembly",
         statement="The accelerator pedal operates the APPS. Provide two pedal-return springs; each must return the pedal to zero travel with the other disconnected. Internal APPS springs do not count as the pedal-return springs.",
         conditions=["All accelerator pedal implementations."],
         numeric=[dict(quantity="independent pedal-return springs", value=2, unit="count", operator="required pair"), dict(quantity="released pedal travel", value=0, unit="%", operator="equals")],
         checks="Inspect two pedal springs and their independence from APPS internals. Disconnect each spring in turn and demonstrate the remaining spring returns the pedal to zero travel throughout the approved operating conditions."),
    dict(id="DD-04", title="Independent APPS channels and distinguishable transfer functions",
         rules=["T.4.2.2", "T.4.2.3"], functions=["apps-input", "apps-fault"], owner="driver-demand APPS channels A and B",
         statement="Use at least two electrically separate APPS sensors; two separate sensors in one OEM housing are permitted. Channel gradients and/or offsets differ. Bias open inputs to zero pedal travel. Above ten percent pedal position, each sensor output differs from every other by at least ten percent. Opposite slopes do not excuse this separation requirement. Calibration into comparable pedal-travel values is an unresolved design obligation.",
         conditions=["This candidate selects two analogue channels. It does not select the optional three-sensor degraded mode."],
         numeric=[dict(quantity="electrically separate APPS sensors", value=2, unit="count", operator=">="), dict(quantity="pedal-position condition", value=10, unit="%", operator=">"), dict(quantity="channel-output separation", value=10, unit="%", operator=">=")],
         checks="Inspect electrical independence and bias circuitry. Measure raw transfer functions over pedal travel, demonstrate required separation and zero-travel open-input behavior, and review calibrated comparison to avoid confusing raw-channel separation with plausibility deviation."),
    dict(id="DD-05", title="Detect APPS faults and inhibit motor power",
         rules=["T.4.2.4", "T.4.2.5", "T.4.2.9", "T.4.2.10"], functions=["apps-fault", "motor-inhibit"], owner="driver-demand monitoring / powertrain inhibit enforcement",
         statement="Treat deviation greater than ten percent pedal travel, sensor failures and wiring failures as APPS implausibility. If implausibility persists longer than one hundred milliseconds, stop all motor power immediately. In an EV, stopping power through the motor controller is sufficient; opening the shutdown circuit is not required by this APPS clause. Analogue open/short faults outside the actual operating range must be detected using pull-up/down circuitry. The example 0.5/4.5 V values are not adopted as universal design thresholds. Proposing a deviation threshold above ten percent requires the stated form justification and approval review.",
         conditions=["Analogue APPS candidate; actual sensor operating range remains unresolved."],
         numeric=[dict(quantity="APPS calibrated deviation", value=10, unit="% pedal travel", operator=">"), dict(quantity="implausibility persistence", value=100, unit="ms", operator=">"), dict(quantity="illustrative analogue fault range", lower=0.5, upper=4.5, unit="V", role="source example only; not selected operating range")],
         checks="Fault-inject each open, short, out-of-range and channel-deviation condition. Check strict threshold and persistence boundaries and complete motor-power removal after qualification. Document actual ranges, bias response and any proposed larger deviation threshold approval."),
    dict(id="DD-06", title="Direct APPS signal path and individual inspection disconnects",
         rules=["T.4.2.7", "T.4.2.8"], functions=["apps-input", "inspection"], owner="driver-demand sensing / technical inspection process",
         statement="Transmit APPS signals directly to the controller using an allowed analogue or digital path. This candidate uses analogue paths and individually detachable APPS connectors so inspection can disconnect each channel; an inline switchable breakout is the source alternative.",
         conditions=["Candidate analogue transmission; digital failure-mode obligations remain outside this selected variant."], numeric=[],
         checks="Inspect each channel's direct signal route and detachable connector. During technical inspection disconnect each channel independently and demonstrate the monitor and inhibit functions."),
    dict(id="DD-07", title="Measure brake demand",
         rules=["T.4.3.1"], functions=["brake-input"], owner="braking / driver-demand sensing",
         statement="Provide a brake pedal-position or brake-pressure sensor/switch. This candidate models brake-pressure sensing without selecting a physical pressure threshold or hardware rating.",
         conditions=["Brake encoder candidate; hard-braking and mechanical-brake-engagement criteria must be substantiated separately."], numeric=[],
         checks="Inspect the selected sensing principle and calibrate its relation to brake demand. Establish the mechanical-brake-engagement and hard-braking criteria from the actual brake design; record supporting evidence."),
    dict(id="DD-08", title="Detect BSE faults and inhibit motor power",
         rules=["T.4.3.3", "T.4.3.4"], functions=["brake-input", "brake-fault", "motor-inhibit"], owner="braking sensor / driver-demand monitor / powertrain",
         statement="Send BSE signals directly to a controller through an allowed path. Detect sensor/wiring faults persisting longer than one hundred milliseconds, treat them as implausibility and immediately stop all motor power. EV motor-controller power removal is sufficient; complete tractive-system deactivation is not required by this clause. For the analogue candidate detect open/short signals outside its actual operating range, with bias resistors ensuring open-circuit detection. Source example 0.5/4.5 V values are not adopted as hardware limits.",
         conditions=["Candidate analogue BSE. Digital BSE requirements and its form applicability remain unallocated."],
         numeric=[dict(quantity="BSE fault persistence", value=100, unit="ms", operator=">"), dict(quantity="illustrative analogue fault range", lower=0.5, upper=4.5, unit="V", role="source example only")],
         checks="Inspect direct routing and bias circuitry. Fault-inject each open, short, out-of-range and wiring failure; check persistence boundaries and complete motor-power removal. Establish actual sensor ranges and detection coverage."),
    dict(id="DD-09", title="Inspect BSE without disturbing other connections",
         rules=["T.4.3.2"], functions=["brake-input", "inspection"], owner="braking sensor / technical inspection process",
         statement="Allow inspection to disconnect BSE signals to the main ECU without changing unrelated connections. The candidate uses dedicated detachable connectors; the source also permits a switchable inline breakout.",
         conditions=["All BSE signal connections used by the chosen implementation."], numeric=[],
         checks="Disconnect each BSE signal through its dedicated inspection interface; demonstrate fault response and verify all unrelated connections remain intact."),
    dict(id="DD-10", title="Latch simultaneous brake and accelerator conflict",
         rules=["EV.4.7.1", "EV.4.7.2"], functions=["brake-conflict", "motor-inhibit", "inspection"], owner="driver-demand brake/APPS latch / powertrain",
         statement="Monitor mechanical-brake engagement together with APPS travel greater than twenty-five percent. When both occur, immediately and completely remove motor power. Keep that inhibit active until APPS travel is below five percent, regardless of brake operation. Demonstrate the complete behavior at technical inspection.",
         conditions=["Mechanical brakes engaged, not simply any nonzero pedal travel; EV.4.6/T.3.2.4 are retained as source dependencies."],
         numeric=[dict(quantity="conflict APPS threshold", value=25, unit="% pedal travel", operator=">"), dict(quantity="inhibit release APPS threshold", value=5, unit="% pedal travel", operator="<")],
         checks="Exercise mechanical-brake engagement with APPS below, at and above twenty-five percent. Demonstrate complete motor-power removal and persistence despite brake release until APPS is strictly below five percent; check the five-percent boundary."),
    dict(id="DD-11", title="BSPD independence from programmable pedal plausibility",
         rules=["EV.8.7.1"], functions=["bspd-monitor", "bspd-trip"], owner="shutdown independent BSPD",
         statement="Provide a standalone nonprogrammable circuit that detects simultaneous braking and high motor-power output, in addition to the APPS/brake pedal plausibility function. This candidate gives the BSPD a separate physical circuit and separately modeled sensor-input checks; it does not replace it with ECU firmware.",
         conditions=["Independent BSPD architecture and independence evidence still require design review."], numeric=[],
         checks="Review schematic, signal dependencies, power/fault paths and component implementation. Demonstrate that programmable APPS/BSE plausibility logic is not the BSPD implementation and cannot defeat its shutdown request."),
    dict(id="DD-12", title="BSPD trip on persistent brake/high-power conflict or input fault",
         rules=["EV.8.7.2", "EV.8.7.3"], functions=["bspd-monitor", "bspd-trip", "tractive-current"], owner="shutdown independent BSPD / shutdown circuit interface",
         statement="Open the shutdown circuit when hard braking coincides for longer than half a second with tractive current corresponding to five kilowatts delivered to motors at nominal battery voltage. Also open the shutdown circuit for any open or short on a BSPD sensor input. The symbolic current threshold is 5000 W divided by nominal battery voltage; the voltage, hard-braking criterion and actual circuit design remain unresolved. Global trip latching/manual-reset behavior belongs to the separate shutdown slice.",
         conditions=["Both hard braking and threshold-level tractive current for the persistence trip; any BSPD sensor input open/short for the fault trip."],
         numeric=[dict(quantity="simultaneous condition persistence", value=0.5, unit="s", operator=">"), dict(quantity="power equivalent at nominal battery voltage", value=5, unit="kW", role="current-threshold reference, not a measured-power comparator"), dict(quantity="BSPD current threshold", expression="5000 W / U_nominal", role="derived relation; U_nominal unresolved")],
         checks="Review nominal-voltage threshold calculation and hard-braking input. Stimulate just below, at and above the current threshold and half-second boundary. Inject open/short faults on every input and demonstrate opening of the entire installed shutdown path."),
    dict(id="DD-13", title="Demonstrate complete BSPD without energizing motors",
         rules=["EV.8.7.4"], functions=["inspection", "bspd-monitor", "bspd-trip"], owner="technical inspection process / BSPD test interface",
         statement="Demonstrate the complete installed BSPD including its current sensor without delivering power to vehicle motors. Use a separate wire and external supply to simulate tractive current while the brake pedal is pressed.",
         conditions=["Installed-vehicle test; isolated stimulus is not proof obtained by the model script."], numeric=[],
         checks="Prepare an approved safe inspection procedure, isolate motor power and record that no motor power is delivered. Apply external current through the separate stimulus wire while pressing the brake; verify current-sensor response and full BSPD shutdown operation."),
    dict(id="DD-14", title="Provide BSPD current-stimulus provision",
         rules=["EV.8.7.5"], functions=["inspection", "bspd-monitor"], owner="BSPD test wiring / configuration management",
         statement="Provide a wire or coil that simulates tractive current at the BSPD current sensor and two external-supply connection points meeting the separate connection-point requirements.",
         conditions=["Physical test provision; actual wire/coil and supply design remain unresolved."],
         numeric=[dict(quantity="external supply connection points", value=2, unit="count", operator="equals")],
         checks="Inspect stimulus wiring/coil routing through the actual current sensor and the two connection points. Demonstrate the inspection setup exercises the installed BSPD sensing chain rather than bypassing it."),
    dict(id="DD-15", title="Accessible BSPD inspection connection points",
         rules=["EV.8.7.6"], functions=["inspection"], owner="BSPD test connection equipment / inspection process",
         statement="Provide two four-millimetre shrouded banana jacks, neither red nor black, labelled CS+ and CS-. They must be directly accessible during active inspection, either on the vehicle or through an adaptor/breakout. This candidate models vehicle-mounted points; final placement and permissible colour selection remain unresolved.",
         conditions=["Chosen vehicle-mounted candidate; adaptor/breakout is the source alternative."],
         numeric=[dict(quantity="test connection points", value=2, unit="count", operator="equals"), dict(quantity="shrouded banana jack diameter", value=4, unit="mm", operator="equals")],
         checks="Measure jack size and inspect shrouding, colour exclusions and polarity labels. Demonstrate direct accessibility with the actual active-inspection configuration."),
]


def groovy(value):
    if value is None: return "null"
    if value is True: return "true"
    if value is False: return "false"
    if isinstance(value, (int, float)): return str(value)
    if isinstance(value, str):
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r") + "'"
    if isinstance(value, list): return "[" + ",".join(map(groovy, value)) + "]"
    if isinstance(value, dict): return "[" + ",".join(groovy(key) + ":" + groovy(item) for key, item in value.items()) + "]" if value else "[:]"
    raise TypeError(type(value))


def prepare(private_source):
    manifest = json.loads((RULES / "source-manifest.json").read_text(encoding="utf-8"))
    assert manifest["main_rulebook"]["sha256"] == SOURCE_HASH
    inventory = json.loads((RULES / "normalized-clauses.json").read_text(encoding="utf-8"))
    texts = {item["clause_id"]: item["text"] for item in json.loads(private_source.read_text(encoding="utf-8"))}
    groups, used = [], set()
    for spec in GROUPS:
        group = dict(spec)
        group["source_refs"] = []
        for clause in inventory["clauses"]:
            if clause["source_rule_id"] not in spec["rules"]: continue
            identifier = clause["clause_id"]
            assert identifier not in used
            assert clause["source_sha256"] == SOURCE_HASH, identifier
            assert clause["source_document_id"] == "FSAE-2027-V1", identifier
            assert hashlib.sha256(texts[identifier].encode()).hexdigest() == clause["source_text_sha256"], identifier
            used.add(identifier)
            group["source_refs"].append({key: clause[key] for key in
                ("clause_id", "source_rule_id", "source_sha256", "source_text_sha256", "source_pages")})
        assert {ref["source_rule_id"] for ref in group["source_refs"]} == set(spec["rules"])
        group.update(classification="technical_design_and_verification" if spec["id"] not in {"DD-06", "DD-09", "DD-13", "DD-15"} else "design_and_inspection_process",
                     interpretation_status="interpreted_candidate", coverage_status="prepared_not_applied",
                     verification_method="inspection_review_and_fault_injection" if spec["id"] not in {"DD-01", "DD-02"} else "analysis_and_bench_test",
                     verification_status="not_verified", evidence_status="not_available", compliance_status="not_claimed")
        groups.append(group)
    result = dict(schema_version=1, slice_id="FSAE2027:driver-demand-v1", source_document_id="FSAE-2027-V1",
                  source_sha256=SOURCE_HASH, project="FSAE-2027-Electric-Arcadia", obligations=groups,
                  source_paragraph_count=len(used), verification_status="not_verified", compliance_status="not_claimed",
                  dependencies=["EV.4.5 -> T.4.2", "EV.4.6 -> T.4.3", "EV.4.7 -> T.3.2.4", "EV.8.7 -> EV.8.2.2"],
                  unallocated_scope=["EV.3 energy meter, maximum power/voltage, violation criteria and penalties",
                      "T.4.2.6 optional three-sensor degraded operation", "Digital APPS/BSE variants and associated forms",
                      "EV.4.5/EV.4.6 referral clauses as a whole", "T.3.2.4 brake/regeneration distribution as a whole",
                      "Final sensor ranges, nominal battery voltage, hard-braking criterion, hardware ratings and fault recovery approvals"])
    (HERE / "obligations.json").write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    model_groups = [dict(group,
                        conditions_json=json.dumps(group["conditions"], ensure_ascii=False, separators=(",", ":")),
                        numeric_json=json.dumps(group["numeric"], ensure_ascii=False, separators=(",", ":")),
                        source_ids_json=json.dumps([ref["clause_id"] for ref in group["source_refs"]], separators=(",", ":")))
                    for group in groups]
    for name in ("30-driver-demand", "35-repair-driver-demand-native-semantics", "40-verify-driver-demand"):
        template = (HERE / f"{name}.template.groovy").read_text(encoding="utf-8")
        assert template.count("/* OBLIGATIONS */") == 1
        (HERE / f"{name}.groovy").write_text(template.replace("/* OBLIGATIONS */", "def obligations = " + groovy(model_groups)), encoding="utf-8")
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--private-source", type=Path, required=True)
    args = parser.parse_args()
    result = prepare(args.private_source)
    print(f"Prepared {len(result['obligations'])} candidate obligations from {result['source_paragraph_count']} checksum-checked source paragraphs; no physical verification performed.")

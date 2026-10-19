"""Generate a bounded shutdown draft from the pinned clause hashes; no model writes."""
import argparse
import hashlib
import json
from pathlib import Path
import sys

HERE = Path(__file__).parent
RULES = HERE.parents[1] / "rules"
sys.path.insert(0, str(RULES))
from prepare_batches import groovy

PINNED_SHA = "f71df5552e341dab1b58f1ff47212e7281953d66b9c41833de8347560913ed0e"

# Each source paragraph is owned by one interpretation group. BSPD internals are a separate slice.
GROUPS = [
    ("SERIES", ["EV.8.1.1"], "Connect the BMS, IMD, BSPD interface, required interlocks, GLVMS, TSMS, three emergency buttons, BOTS and inertia switch in one series shutdown-current path.", ["series", "accept"], "Inspect the complete physical series chain and open each channel independently; record loss of isolation/precharge coil current.", []),
    ("COIL-CURRENT", ["EV.8.1.2", "EV.8.1.3", "EV.8.1.6"], "Carry isolation and precharge relay drive current directly through the high-side shutdown path; emergency buttons, BOTS, master switches and interlocks carry that current themselves.", ["series", "cutout", "coils"], "Measure coil drive paths and show there is no low-side substitution, supervisory-only contact or bypass feed.", ["Coil current and contact ratings TBD; select hardware after worst-case drive-current analysis."]),
    ("NORMALLY-OPEN", ["EV.8.1.4"], "Use normally-open BMS, IMD and BSPD shutdown outputs so loss of their sustaining drive opens the path.", ["series", "bms", "imd", "accept"], "Remove power and sustaining drive separately from each protective output; observe the open current path.", ["Relay/contact output topology and parts TBD."]),
    ("INDEPENDENCE", ["EV.8.1.5"], "BMS, IMD and BSPD interrupt the shutdown path independently; a fault cannot backfeed power into the path.", ["series", "bms", "imd", "cutout"], "Inject channel, supply and wiring faults; show independent opening and absence of backfeed.", ["Independent hardware schematic and common-cause/backfeed analysis TBD."]),
    ("INSPECTION", ["EV.8.1.7"], "Provide a technical-inspection demonstration of every shutdown component, feature and effect.", ["demonstrate", "series"], "Execute and record a complete vehicle-level demonstration checklist without claiming an unperformed test passed.", ["Technical-inspection procedure, fixtures and evidence TBD."]),
    ("TRIP-CAUSES", ["EV.8.2.1"], "Open the shutdown circuit upon operation or detection by any required shutdown component and upon any GLV-system shutdown.", ["accept", "cutout", "glv", "switches", "interlocks"], "Inject each independent trip source and GLV loss; prove interruption without relying on software arbitration.", []),
    ("SHUTDOWN-EFFECTS", ["EV.8.2.2"], "An open shutdown circuit stops tractive battery current immediately, disables the tractive system, removes motor torque so motors spin freely, and reduces TS voltage to the T.9.1.2 low-voltage class within five seconds.", ["cutout", "coils", "current", "discharge", "torque"], "Measure battery current, motor torque/free-spin and residual voltage after every trip; prove low voltage within 5 s using the applicable T.9.1.2 definition.", ["Discharge circuit, energy, isolation-relay arrangement and verified timing margins TBD; T.9.1.2 and EV.5.4.3 dependencies need review."]),
    ("MANUAL-LATCH", ["EV.8.2.3"], "Latch BMS, IMD or BSPD trips disabled until a person directly at the vehicle manually resets them. The driver cannot reactivate TS; emergency-button or TSMS operation cannot bypass the latch.", ["latch", "reset", "cutout"], "Trip each protective device; try driver controls, TSMS, button toggles, remote reset and GLV power cycling. Only a permitted local manual reset after clearing faults releases the latch.", ["Reset location and independent fail-safe latch hardware TBD; resetting does not itself authorize TS activation."]),
    ("BMS-MONITORING", ["EV.8.3.1"], "Monitor battery voltage and temperature whenever TS is active or the tractive battery is connected to a charger.", ["voltage", "temperature", "bms"], "Test both active-TS and charger-connected modes, including TS-open charging; inject missing samples in each mode.", ["Charging integration EV.9.3 and active-state definition EV.12.5 remain dependencies."]),
    ("MODULE-ISOLATION", ["EV.8.3.2"], "Provide galvanic isolation at every BMS module-to-module boundary and substantiate it in the ESF.", ["bms", "voltage", "temperature"], "Inspect the module-boundary isolation plan and perform appropriate isolation tests using the approved ESF design.", ["Battery module configuration, isolation barriers/ratings and approved ESF TBD."]),
    ("BALANCE-INHIBIT", ["EV.8.3.3"], "Prevent cell balancing whenever the shutdown circuit is open, including charging.", ["balance", "cutout"], "Open every shutdown path while requesting balancing in active/charging modes; observe balancing current cease.", ["Balancing hardware and charging interface EV.9.4 TBD."]),
    ("BMS-FAULTS", ["EV.8.3.4"], "Detect invalid cell voltage, tripped voltage-sense protection, invalid cell temperature, absent/interrupted measurements and BMS internal faults.", ["voltage", "temperature", "sense-fuse", "bms-health", "bms"], "Inject each listed fault and distinguish fault detection from a merely plausible last stored sample.", ["Selected-cell limits, measurement accuracy, sampling and diagnostic implementation TBD."]),
    ("BMS-TRIP-INDICATION", ["EV.8.3.5"], "Every detected BMS fault opens shutdown and latches the BMS warning and TS-status indication until manual BMS reset.", ["bms", "accept", "latch", "indicate"], "Inject every BMS fault; verify circuit opening and both indications remain asserted until permitted reset.", ["TS status indication EV.6.1.6e implementation is a dependent slice."]),
    ("BMS-LAMP", ["EV.8.3.6", "EV.8.3.7"], "Provide a red driver-visible BMS-marked warning in bright sunlight and perform the EV.6.3 lamp check.", ["indicate", "lamp-check"], "Inspect marking/color and seated-driver visibility in bright sunlight; execute the EV.6.3 lamp-check sequence.", ["Lamp hardware, mounting, brightness and detailed EV.6.3 sequence TBD."]),
    ("CELL-VOLTAGE", ["EV.8.4.1"], "Measure each cell voltage; directly parallel-connected cells may share one measurement.", ["voltage", "bms"], "Compare every electrical cell group against the sensing-channel map, including conditional direct-parallel grouping.", ["Exact cells and series/parallel configuration TBD."]),
    ("CELL-VOLTAGE-LIMIT", ["EV.8.4.2"], "Maintain cell voltage within the selected manufacturer's minimum/maximum limits with measurement accuracy included.", ["voltage", "bms"], "Apply worst-case measurement error and boundary voltages against the selected-cell datasheet; demonstrate trip behavior.", ["Cell datasheet values and voltage measurement uncertainty TBD; no numerical cell limits are invented."]),
    ("SENSE-PROTECTION", ["EV.8.4.3"], "Protect every BMS voltage-sense connection at the directly cell-tab-connected conductor/trace; protection voltage rating is at least maximum module voltage.", ["sense-fuse", "voltage"], "Inspect cell-tab protection placement, ratings and the blown/tripped-protection fault path.", ["Module voltage, protection devices, exact tab/trace layout and fault-current ratings TBD."]),
    ("CELL-TEMPERATURE", ["EV.8.5.1", "EV.8.5.2"], "Measure cell temperatures and keep them strictly below the lower of the cell-datasheet maximum and 60 degrees C, including measurement accuracy.", ["temperature", "bms"], "Test boundary temperatures with uncertainty; confirm the manufacturer's limit controls whenever it is below 60 degrees C.", ["Cell temperature limit, sensing uncertainty and thermal placement TBD."]),
    ("SENSOR-LOCATION", ["EV.8.5.3", "EV.8.5.4"], "Sense at each applicable cell's negative terminal with direct contact to that terminal or its negative-terminal busbar less than 10 mm from the specified weld/clamp location.", ["temperature"], "Inspect sensor contact and negative-terminal location; measure the strict less-than-10-mm alternative where used.", ["Physical cells, weld/clamp geometry and sensor installation TBD."]),
    ("LITHIUM-COVERAGE", ["EV.8.5.5"], "For lithium-based cells, monitor at least 20 percent, distributed equally across containers; measuring every cell is a recommendation retained separately.", ["temperature", "bms"], "If lithium cells are selected, inspect cell count and container distribution and verify at least 20-percent coverage; record the every-cell recommendation separately.", ["Chemistry, cell count and container configuration TBD; lithium-specific applicability remains conditional."]),
    ("SHARED-SENSOR", ["EV.8.5.6"], "A temperature sensor may represent multiple cells only if all EV.8.5 conditions hold for every sensed cell.", ["temperature"], "If sensors are shared, substantiate contact, coverage and isolation for every represented cell.", ["Shared-sensor selection and applicability TBD."]),
    ("SENSOR-ISOLATION", ["EV.8.5.7"], "Provide suitable temperature-sensing isolation at the sensor/cell boundary or in the sensing circuit, accounting for TS/GLV isolation and sense-location common-mode voltage.", ["temperature", "bms"], "Test selected isolation barriers at required TS/GLV and common-mode conditions.", ["Isolation topology, ratings and common-mode envelope TBD."]),
    ("IMD-INSTALL", ["EV.8.6.1"], "Install an insulation monitor in the tractive system.", ["imd"], "Inspect the actual tractive-system IMD connections and exercise insulation sensing at the installed interface.", []),
    ("IMD-APPROVAL", ["EV.8.6.2"], "Choose an IMD model explicitly approved by the applicable official event Rules FAQ.", ["imd"], "Compare the selected exact part/model with the pinned applicable official FAQ approval list.", ["Approved IMD model and applicable official Rules FAQ are TBD; this draft selects no part."]),
    ("IMD-THRESHOLD", ["EV.8.6.3"], "Set IMD response to at least 500 ohms per volt of maximum TS operating voltage.", ["imd"], "Use the actual maximum operating voltage to calculate the response resistance and inject insulation values around the selected threshold.", ["Maximum operating voltage and resulting resistance setting TBD; the 600-V rule ceiling is not assumed to be the design voltage."]),
    ("IMD-INDEPENDENT", ["EV.8.6.4"], "Detect insulation failure and IMD malfunction without influence from programmable logic.", ["imd", "accept", "cutout"], "Inject insulation/monitor faults while stopping or corrupting programmable controllers; prove independent shutdown.", ["Nonprogrammable IMD safety output and independent wiring details TBD."]),
    ("IMD-TRIP-INDICATION", ["EV.8.6.5"], "IMD faults open shutdown and latch IMD warning plus TS-status indication until manual IMD reset.", ["imd", "accept", "latch", "indicate"], "Inject insulation and monitor faults; verify open circuit and retained indications until permitted reset.", ["TS status indication EV.6.1.6e implementation is a dependent slice."]),
    ("IMD-LAMP", ["EV.8.6.6", "EV.8.6.7"], "Provide a red driver-visible IMD-marked warning in bright sunlight and perform the EV.6.3 lamp check.", ["indicate", "lamp-check"], "Inspect marking/color and seated-driver sunlight visibility; execute the detailed EV.6.3 check.", ["IMD lamp mounting/brightness and detailed EV.6.3 sequence TBD."]),
    ("INTERLOCK-LOCATIONS", ["EV.8.8.1", "EV.8.8.2"], "Install required interlocks at EV.4.1.3, EV.5.5.2 and EV.7.4.4 interfaces; additional interlocks are permitted, not automatically required.", ["interlocks", "series"], "Review all three referenced interfaces, map every required connection and test each break independently.", ["Exact disconnect/enclosure/connector map and optional additional interlocks TBD; referenced clauses need full review."]),
    ("INTERLOCK-OPEN", ["EV.8.8.3"], "A broken or interrupted interlock connection opens shutdown.", ["interlocks", "accept", "cutout"], "Break each connector/interlock connection and verify isolation/precharge drive current stops.", []),
    ("INTERLOCK-ROUTING", ["EV.8.8.4"], "Apply any EV.7.5 interlock-separation exception only after reviewing EV.8.8.4's listed harness, connector and enclosure/location conditions, including the less-than-75-mm condition.", ["interlocks", "series"], "Inspect actual routing and evaluate every applicable exception condition and the strict 75-mm boundary.", ["Routing design and precise applicability of the listed exception conditions TBD; this draft does not automatically waive separation."]),
    ("MASTERS-DIRECT", ["EV.8.9.1"], "Provide two directly acting master switches meeting T.9.3 location/configuration; do not substitute relay or logic control.", ["switches", "series"], "Inspect distinct GLV/TS switches, direct-current contacts and T.9.3 location/configuration.", ["Switch parts and T.9.3 packaging review TBD."]),
    ("GLV-MASTER", ["EV.8.9.2"], "GLVMS stops all GLV power and is labeled LV within a completely red circle greater than 50 mm diameter.", ["glv", "switches", "series"], "Operate GLVMS and measure every GLV supply off; inspect marking/color and a circle strictly larger than 50 mm.", ["GLV power-distribution completeness and physical switch marking layout TBD."]),
    ("TS-MASTER", ["EV.8.9.3"], "TSMS opens shutdown when OFF; it is the last switch before IRs except precharge circuitry/interlocks, has an orange circle greater than 50 mm, TS/W012 marking and OFF lockout/tagout.", ["switches", "series", "coils"], "Inspect electrical order, OFF opening, lockout/tagout and the required orange area and TS/ISO-7010-W012 marking.", ["TSMS/contact selection, exact packaging, official symbol implementation and EV.12.3.1 process TBD."]),
    ("BUTTON-COUNT", ["EV.8.10.1"], "Install the three required shutdown buttons: left side, right side and cockpit.", ["switches", "series"], "Locate and independently operate all three required buttons.", []),
    ("BUTTON-MECHANISM", ["EV.8.10.2"], "Use push-pull or push-rotate emergency switches that open and hold OFF and permit closure ON, while never overriding a latched protective trip.", ["switches", "series", "latch"], "Inspect mechanisms and test hold-OFF/ON behavior both with and without a latched protective trip.", ["Selected emergency-stop hardware and current ratings TBD."]),
    ("BUTTON-SIDES", ["EV.8.10.3"], "Place left/right buttons aft of the main hoop near the specified bracing junction; each is at least 40 mm diameter and not easily removed or mounted on removable bodywork.", ["switches"], "Inspect both side installations, retention, bodywork and minimum 40-mm diameter.", ["Chassis packaging, F.5.9 junction location and mounting/retention design TBD."]),
    ("BUTTON-COCKPIT", ["EV.8.10.4"], "Place the cockpit button within easy unobstructed reach of the belted driver adjacent to the steering wheel; diameter is at least 24 mm.", ["switches"], "Test reach with the driver restrained and inspect wheel/other obstructions and minimum 24-mm diameter.", ["Driver envelope, cockpit geometry and button installation TBD."]),
    ("BUTTON-SYMBOL", ["EV.8.10.5"], "Place the specified red-spark electrical symbol on a white-edged blue triangle near every shutdown button.", ["switches"], "Inspect symbol identity and placement beside each of the three buttons.", ["Final marking artwork and installation TBD."]),
]


def build(private_source):
    inventory = json.loads((RULES / "normalized-clauses.json").read_text(encoding="utf-8"))
    rows = [row for row in inventory["clauses"] if (row.get("source_rule_id") or "").startswith("EV.8.") and not row["source_rule_id"].startswith("EV.8.7")]
    raw = {row["clause_id"]: row["text"] for row in json.loads(private_source.read_text(encoding="utf-8"))}
    for row in rows:
        if row["source_sha256"] != PINNED_SHA or hashlib.sha256(raw[row["clause_id"]].encode("utf-8")).hexdigest() != row["source_text_sha256"]:
            raise ValueError("Pinned source mismatch: " + row["clause_id"])
    artifacts = {"FSAE-2027-V1:EV.8.1.7:p002"} | {f"FSAE-2027-V1:EV.8.2.3:p{number:03}" for number in range(4, 16)}
    groups, assigned = [], set()
    for key, rules, obligation, model_keys, verification, unresolved in GROUPS:
        if key == "GLV-MASTER":
            model_keys = ["glv-master"] + model_keys
        if key == "TRIP-CAUSES":
            model_keys = ["glv-supply"] + model_keys
        selected = [row for row in rows if row["source_rule_id"] in rules and row["clause_id"] not in artifacts]
        sources = [{field: row[field] for field in ("clause_id", "source_rule_id", "source_pages", "source_sha256", "source_text_sha256")} for row in selected]
        for source in sources:
            if source["clause_id"] in assigned:
                raise ValueError("Duplicate interpretation ownership")
            assigned.add(source["clause_id"])
            source["source_review_status"] = "visually_checked_prose_on_pages_105_to_110"
            if source["clause_id"] == "FSAE-2027-V1:EV.8.2.3:p003":
                source["source_review_status"] = "visually_checked_manual_reset_prose_only_rotated_figure_text_ignored"
        groups.append({"key": key, "identifier": "FSAE2027:SD:obligation:" + key, "obligation": obligation,
                       "source_clauses": sources, "model_keys": model_keys, "verification_obligation": verification,
                       "unresolved_decisions": unresolved, "interpretation_status": "interpreted",
                       "coverage_status": "draft_not_applied", "verification_status": "not_verified"})
    if assigned | artifacts != {row["clause_id"] for row in rows}:
        raise ValueError("Shutdown sources were not fully accounted for")
    report = {"schema_version": 1, "slice_id": "FSAE2027:shutdown-v1", "source_document_id": "FSAE-2027-V1", "source_sha256": PINNED_SHA,
              "scope": "EV.8 excluding BSPD internals EV.8.7", "prose_review_pages": [105, 106, 107, 108, 109, 110],
              "source_record_count": len(rows), "interpreted_prose_record_count": len(assigned), "obligations": groups,
              "figure_label_records": [{field: row[field] for field in ("clause_id", "source_pages", "source_text_sha256")} | {"status": "figure_label_not_normative_prose_requires_separate_figure_review"} for row in rows if row["clause_id"] in artifacts],
              "completion_status": "partial_candidate_design_with_unresolved_hardware_and_dependencies", "verification_status": "not_verified",
              "stable_model_id_algorithm": "UUID.nameUUIDFromBytes(UTF8('FSAE2027:SD:' + model_key))"}
    (HERE / "shutdown-obligations.json").write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    template = (HERE / "shutdown-model-template.txt").read_text(encoding="utf-8")
    script_groups = []
    for group in groups:
        script_group = {key: group[key] for key in ("key", "identifier", "obligation", "model_keys", "verification_obligation", "unresolved_decisions")}
        script_group["source_clauses"] = [{key: source[key] for key in ("clause_id", "source_pages", "source_text_sha256", "source_review_status")} for source in group["source_clauses"]]
        script_groups.append(script_group)
    script = template.replace("@@OBLIGATIONS@@", groovy(script_groups)).replace("@@PINNED_SHA@@", PINNED_SHA)
    if len(script) > 100000:
        raise ValueError("Draft exceeds the actual executor script length limit")
    (HERE / "30-model-shutdown.groovy").write_text(script, encoding="utf-8")
    print(json.dumps({"source_records": len(rows), "prose_records": len(assigned), "figure_labels": len(artifacts), "obligations": len(groups), "script_characters": len(script)}))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--private-source", type=Path, required=True)
    build(parser.parse_args().private_source)

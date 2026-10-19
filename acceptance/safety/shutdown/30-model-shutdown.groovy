// Source-grounded additive candidate design. Apply only after reviewing this complete draft.
// BSPD detection logic EV.8.7 belongs to the driver-demand slice; this slice owns its series contact interface.
assert session.sessionResource.URI.toString().toLowerCase().contains('/fsae-2027-electric-arcadia/')
def req = api.requirements()
assert req.available() && req.viewpointEnabled()
assert api.layer('SA').system.summary == 'FSAE2027:bootstrap-v1'
def prefix = 'FSAE2027:SD '
def stableId = { key -> java.util.UUID.nameUUIDFromBytes(('FSAE2027:SD:' + key).getBytes('UTF-8')).toString() }
// Snapshot once: repeated findById calls after writes rescan the complete imported Requirements model.
def existingIds = api.query { value -> value.eClass().getEStructuralFeature('id') != null }.collect { api.id(it) }.toSet()
assert api.find('LogicalComponent', prefix + 'Independent BMS Logical monitor') == null:
    'Shutdown slice already exists. Verify and extend it; do not replay the creation draft.'
def module = req.find('FSAE-2027-V1:source-module')
assert module != null
def sourceRequirements = req.requirements(module).collectEntries { [(it.ReqIFIdentifier):it] }
def obligations = [['key': 'SERIES', 'identifier': 'FSAE2027:SD:obligation:SERIES', 'obligation': 'Connect the BMS, IMD, BSPD interface, required interlocks, GLVMS, TSMS, three emergency buttons, BOTS and inertia switch in one series shutdown-current path.', 'model_keys': ['series', 'accept'], 'verification_obligation': 'Inspect the complete physical series chain and open each channel independently; record loss of isolation/precharge coil current.', 'unresolved_decisions': [], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.1.1:p001', 'source_pages': [105], 'source_text_sha256': '8ec6f0190e171636bb876b7bba0a828a086c07d5089e31049a37a6049c1e518f', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.1.1:p002', 'source_pages': [105], 'source_text_sha256': 'c345f67fa86a4db52f984a79601110e5f7571b5693f572517a2fafd0dcc38a2f', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.1.1:p003', 'source_pages': [105], 'source_text_sha256': 'ad92c9eae3c85ab7f3473e8941f8fe66fa4ba119ecf76c8a43ee6d89fab8b7c8', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.1.1:p004', 'source_pages': [105], 'source_text_sha256': '226c3b9ae429ab7181cc7f554a7a9a51f81d8368ddd15fb4689282aa956e5ac6', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.1.1:p005', 'source_pages': [105], 'source_text_sha256': '39b42f8e682f844f3cf91a5d31fc304bd2a0d4d30a7ae9a2d7e84c437527a559', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.1.1:p006', 'source_pages': [105], 'source_text_sha256': '9c0c8bd27eecc9f03c7e36d3906b4568ae5fa941bd1ae4f3873789e531ba03a0', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.1.1:p007', 'source_pages': [105], 'source_text_sha256': 'd731300936ab972d990024929fb3d6650f32115fdef13f4b4a02dfe44035a4c3', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.1.1:p008', 'source_pages': [105], 'source_text_sha256': '83ecafa75437fbff270ca4522382cd18d9b71779964e09ff77b2666798cadf90', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.1.1:p009', 'source_pages': [105], 'source_text_sha256': 'e13e8fb88932b1b9538091d2e233baae2e54b19073b399d9558c1002ab3f8fd0', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'COIL-CURRENT', 'identifier': 'FSAE2027:SD:obligation:COIL-CURRENT', 'obligation': 'Carry isolation and precharge relay drive current directly through the high-side shutdown path; emergency buttons, BOTS, master switches and interlocks carry that current themselves.', 'model_keys': ['series', 'cutout', 'coils'], 'verification_obligation': 'Measure coil drive paths and show there is no low-side substitution, supervisory-only contact or bypass feed.', 'unresolved_decisions': ['Coil current and contact ratings TBD; select hardware after worst-case drive-current analysis.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.1.2:p001', 'source_pages': [106], 'source_text_sha256': 'd5685e1ce1230e4bbf93be2a3714534f7bde787b759a9f6f4f2eb1fb4b72fa40', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.1.3:p001', 'source_pages': [106], 'source_text_sha256': '719ade30cb97ee357230cd7f47a98582bbd236bee0cb7eb6ac8d41398558035f', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.1.6:p001', 'source_pages': [106], 'source_text_sha256': '91effc10c88655c685a3e002c950a8b0f36c29c9bb5dd77b36486ba7dba13337', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'NORMALLY-OPEN', 'identifier': 'FSAE2027:SD:obligation:NORMALLY-OPEN', 'obligation': 'Use normally-open BMS, IMD and BSPD shutdown outputs so loss of their sustaining drive opens the path.', 'model_keys': ['series', 'bms', 'imd', 'accept'], 'verification_obligation': 'Remove power and sustaining drive separately from each protective output; observe the open current path.', 'unresolved_decisions': ['Relay/contact output topology and parts TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.1.4:p001', 'source_pages': [106], 'source_text_sha256': '239cfa6d8a9b34bb159079cb54c5a2a32c8fd7c19319c20f3735149cdf1ac17c', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'INDEPENDENCE', 'identifier': 'FSAE2027:SD:obligation:INDEPENDENCE', 'obligation': 'BMS, IMD and BSPD interrupt the shutdown path independently; a fault cannot backfeed power into the path.', 'model_keys': ['series', 'bms', 'imd', 'cutout'], 'verification_obligation': 'Inject channel, supply and wiring faults; show independent opening and absence of backfeed.', 'unresolved_decisions': ['Independent hardware schematic and common-cause/backfeed analysis TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.1.5:p001', 'source_pages': [106], 'source_text_sha256': '231cf592c3335f165ef0f8fe27d5b2a1006ead7a7b8ce30a9c8cc6bcaad147a5', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.1.5:p002', 'source_pages': [106], 'source_text_sha256': '6b802749bea68b3c78c79734bcdbfddb4d696584caa5c4bc3357a227aa203793', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'INSPECTION', 'identifier': 'FSAE2027:SD:obligation:INSPECTION', 'obligation': 'Provide a technical-inspection demonstration of every shutdown component, feature and effect.', 'model_keys': ['demonstrate', 'series'], 'verification_obligation': 'Execute and record a complete vehicle-level demonstration checklist without claiming an unperformed test passed.', 'unresolved_decisions': ['Technical-inspection procedure, fixtures and evidence TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.1.7:p001', 'source_pages': [106], 'source_text_sha256': 'b50ac91e6a5cdc249c2b8dd65f89346673bd6136df13c46189c89da8aba31e7b', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'TRIP-CAUSES', 'identifier': 'FSAE2027:SD:obligation:TRIP-CAUSES', 'obligation': 'Open the shutdown circuit upon operation or detection by any required shutdown component and upon any GLV-system shutdown.', 'model_keys': ['glv-supply', 'accept', 'cutout', 'glv', 'switches', 'interlocks'], 'verification_obligation': 'Inject each independent trip source and GLV loss; prove interruption without relying on software arbitration.', 'unresolved_decisions': [], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.2.1:p001', 'source_pages': [106], 'source_text_sha256': '82d53a1b75a9d0ec17bd0f0ec3f5c1170ec42e575f274d684f5f6e0f4a88546d', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.2.1:p002', 'source_pages': [106], 'source_text_sha256': '554889c5ebe154c5d6f23cacd0b85e9f7c533a69859ff27f0cc34fe28354396d', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.2.1:p003', 'source_pages': [106], 'source_text_sha256': '970fdcd52a63455845c1fff1876799f5c617906401d04a129663a5f4adc3e75c', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'SHUTDOWN-EFFECTS', 'identifier': 'FSAE2027:SD:obligation:SHUTDOWN-EFFECTS', 'obligation': 'An open shutdown circuit stops tractive battery current immediately, disables the tractive system, removes motor torque so motors spin freely, and reduces TS voltage to the T.9.1.2 low-voltage class within five seconds.', 'model_keys': ['cutout', 'coils', 'current', 'discharge', 'torque'], 'verification_obligation': 'Measure battery current, motor torque/free-spin and residual voltage after every trip; prove low voltage within 5 s using the applicable T.9.1.2 definition.', 'unresolved_decisions': ['Discharge circuit, energy, isolation-relay arrangement and verified timing margins TBD; T.9.1.2 and EV.5.4.3 dependencies need review.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.2.2:p001', 'source_pages': [106], 'source_text_sha256': '833bb3a15abd3275868f18fe30183a77db45353bcd1430072cae4b9f26903e30', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.2.2:p002', 'source_pages': [106], 'source_text_sha256': 'a2a6a0067cbe85d1d05c4f5e42b2ca58f2993bed37212cc9b7fe71c71afe3f6e', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.2.2:p003', 'source_pages': [106], 'source_text_sha256': '8380f8170175c881bbe2177d68d5e11a554b596cd6634c76fc05715ec9f1089f', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.2.2:p004', 'source_pages': [106], 'source_text_sha256': '9342ba49b1a5b61fc38f6519dfee083c150e3a493403c116769773bf4a252c03', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.2.2:p005', 'source_pages': [106], 'source_text_sha256': '8c71f3dac8d6d809ab1ab202407dbf19ce1992b67799abfcddc6cb0d7c476c17', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'MANUAL-LATCH', 'identifier': 'FSAE2027:SD:obligation:MANUAL-LATCH', 'obligation': 'Latch BMS, IMD or BSPD trips disabled until a person directly at the vehicle manually resets them. The driver cannot reactivate TS; emergency-button or TSMS operation cannot bypass the latch.', 'model_keys': ['latch', 'reset', 'cutout'], 'verification_obligation': 'Trip each protective device; try driver controls, TSMS, button toggles, remote reset and GLV power cycling. Only a permitted local manual reset after clearing faults releases the latch.', 'unresolved_decisions': ['Reset location and independent fail-safe latch hardware TBD; resetting does not itself authorize TS activation.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.2.3:p001', 'source_pages': [106], 'source_text_sha256': '3aed423e09fe581ebc7eee983d95578328e643131f37ee8b4e093ce553458d30', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.2.3:p002', 'source_pages': [106], 'source_text_sha256': '5762548508f327e057c19d2d607a7fb1ce1bb5f6fce943567ab992cf1eeec958', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.2.3:p003', 'source_pages': [106], 'source_text_sha256': 'f0adb4800d5697f0959b5aa8705ad774c92d0e76425457506cc9a0599c321193', 'source_review_status': 'visually_checked_manual_reset_prose_only_rotated_figure_text_ignored'], ['clause_id': 'FSAE-2027-V1:EV.8.2.3:p016', 'source_pages': [107], 'source_text_sha256': '678ae5937b03e3931bf8bdc6f58014e34117865a717751d0698a75a8ed10790a', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.2.3:p017', 'source_pages': [107], 'source_text_sha256': '397a2dc62761067ba65a1d7c35c1f6b5939f1e315a4c2c69d08d1729ad239763', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'BMS-MONITORING', 'identifier': 'FSAE2027:SD:obligation:BMS-MONITORING', 'obligation': 'Monitor battery voltage and temperature whenever TS is active or the tractive battery is connected to a charger.', 'model_keys': ['voltage', 'temperature', 'bms'], 'verification_obligation': 'Test both active-TS and charger-connected modes, including TS-open charging; inject missing samples in each mode.', 'unresolved_decisions': ['Charging integration EV.9.3 and active-state definition EV.12.5 remain dependencies.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.3.1:p001', 'source_pages': [107], 'source_text_sha256': '4144fc5a02741caa1dc003e411d81c6d435f3cf7c0657bc633be67109d790097', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.3.1:p002', 'source_pages': [107], 'source_text_sha256': 'dd85f794331fc002be2ca6e60abf517eb10a0a385ab9e15bdb7278607d7f11ab', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.3.1:p003', 'source_pages': [107], 'source_text_sha256': '2252bec51a809c42e5d2eef01fda54e51a6a1a25af8b3746968927a6e62f57d0', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'MODULE-ISOLATION', 'identifier': 'FSAE2027:SD:obligation:MODULE-ISOLATION', 'obligation': 'Provide galvanic isolation at every BMS module-to-module boundary and substantiate it in the ESF.', 'model_keys': ['bms', 'voltage', 'temperature'], 'verification_obligation': 'Inspect the module-boundary isolation plan and perform appropriate isolation tests using the approved ESF design.', 'unresolved_decisions': ['Battery module configuration, isolation barriers/ratings and approved ESF TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.3.2:p001', 'source_pages': [107], 'source_text_sha256': '403eeca4f692df1cbdc211529454516d7428fccc5fab71721460317a79d6b7ae', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'BALANCE-INHIBIT', 'identifier': 'FSAE2027:SD:obligation:BALANCE-INHIBIT', 'obligation': 'Prevent cell balancing whenever the shutdown circuit is open, including charging.', 'model_keys': ['balance', 'cutout'], 'verification_obligation': 'Open every shutdown path while requesting balancing in active/charging modes; observe balancing current cease.', 'unresolved_decisions': ['Balancing hardware and charging interface EV.9.4 TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.3.3:p001', 'source_pages': [107], 'source_text_sha256': '28ce46659cfd7e6c75b49ff848228c9e420147c77632e1d062e424704a8ca4c6', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'BMS-FAULTS', 'identifier': 'FSAE2027:SD:obligation:BMS-FAULTS', 'obligation': 'Detect invalid cell voltage, tripped voltage-sense protection, invalid cell temperature, absent/interrupted measurements and BMS internal faults.', 'model_keys': ['voltage', 'temperature', 'sense-fuse', 'bms-health', 'bms'], 'verification_obligation': 'Inject each listed fault and distinguish fault detection from a merely plausible last stored sample.', 'unresolved_decisions': ['Selected-cell limits, measurement accuracy, sampling and diagnostic implementation TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.3.4:p001', 'source_pages': [107], 'source_text_sha256': '9098d81cfb6c7fd31a9ed06d256e1ed38753fbaf5104ec81c4a16797a1364747', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.3.4:p002', 'source_pages': [107], 'source_text_sha256': '7608a41e51e2be22d1dce98125481efe0b78fbe11d1ddb22a6ac56edc17420e4', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.3.4:p003', 'source_pages': [107], 'source_text_sha256': '7b919b180176358856d305a23e95294388a5321df00e5216a79aa899ea7334be', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.3.4:p004', 'source_pages': [107], 'source_text_sha256': 'dbc75b8ae858b798814d479a6c7da94f055cfc2caa51116567b2b4f67d2ab284', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.3.4:p005', 'source_pages': [107], 'source_text_sha256': '6e59a345d407ae4ad41315df31a1305df7a3c370b8e9f91575572524e3b4ee90', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.3.4:p006', 'source_pages': [107], 'source_text_sha256': '1cfd1197a63121f3510a51e8f16835af553d23fb6cd880cd96c3a0600eeb3a53', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'BMS-TRIP-INDICATION', 'identifier': 'FSAE2027:SD:obligation:BMS-TRIP-INDICATION', 'obligation': 'Every detected BMS fault opens shutdown and latches the BMS warning and TS-status indication until manual BMS reset.', 'model_keys': ['bms', 'accept', 'latch', 'indicate'], 'verification_obligation': 'Inject every BMS fault; verify circuit opening and both indications remain asserted until permitted reset.', 'unresolved_decisions': ['TS status indication EV.6.1.6e implementation is a dependent slice.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.3.5:p001', 'source_pages': [107], 'source_text_sha256': '82168f3e9504aa095204d1cad1b541b9830b9a44c0c688c7339f41b0531623cd', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.3.5:p002', 'source_pages': [107], 'source_text_sha256': '0a68182d54dbcc6988778bf115a863d17c4beef2a79b4d488a1f45998aa29efd', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.3.5:p003', 'source_pages': [107], 'source_text_sha256': '6ecb27db3041738cba8a87faaf47dbb15d81e04172656c33bc277f2fd5a5f059', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.3.5:p004', 'source_pages': [107], 'source_text_sha256': 'fbbcea8f7962c1557b24b615bd1286e265ee5c6ba7f9ded3ea4231f60ff4f44b', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'BMS-LAMP', 'identifier': 'FSAE2027:SD:obligation:BMS-LAMP', 'obligation': 'Provide a red driver-visible BMS-marked warning in bright sunlight and perform the EV.6.3 lamp check.', 'model_keys': ['indicate', 'lamp-check'], 'verification_obligation': 'Inspect marking/color and seated-driver visibility in bright sunlight; execute the EV.6.3 lamp-check sequence.', 'unresolved_decisions': ['Lamp hardware, mounting, brightness and detailed EV.6.3 sequence TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.3.6:p001', 'source_pages': [107], 'source_text_sha256': '54e81fd800b5a712342cbf54536a283befdb2cb3eef6176f14292c6e191d8a08', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.3.6:p002', 'source_pages': [107], 'source_text_sha256': 'f871e1a6da1f10ce9865651265aea732e1fe3760ad7c3a3d2795dc1254a171ed', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.3.6:p003', 'source_pages': [107], 'source_text_sha256': '03328a62fd70f4d85b061d721ac35891bfc7667e9214367fb9348a255bb65046', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.3.6:p004', 'source_pages': [107], 'source_text_sha256': '55a3e1cac6569d86f768874acbd277d1cd5097b64b4adae46f876aa2bd11163a', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.3.7:p001', 'source_pages': [107], 'source_text_sha256': '400832b62e25037fe5223308b6f1b89118e1c732e72eab080481bbba046b738f', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'CELL-VOLTAGE', 'identifier': 'FSAE2027:SD:obligation:CELL-VOLTAGE', 'obligation': 'Measure each cell voltage; directly parallel-connected cells may share one measurement.', 'model_keys': ['voltage', 'bms'], 'verification_obligation': 'Compare every electrical cell group against the sensing-channel map, including conditional direct-parallel grouping.', 'unresolved_decisions': ['Exact cells and series/parallel configuration TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.4.1:p001', 'source_pages': [107], 'source_text_sha256': '553b8a9d05f895b462587e3ef53ba48af6c6ebf5884a880748c61a1e830f722d', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.4.1:p002', 'source_pages': [107], 'source_text_sha256': '48c4c31da29c56b47b14ad51f7940c3c22ac1e590930f0d2ba1fed268893734c', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'CELL-VOLTAGE-LIMIT', 'identifier': 'FSAE2027:SD:obligation:CELL-VOLTAGE-LIMIT', 'obligation': 'Maintain cell voltage within the selected manufacturer\'s minimum/maximum limits with measurement accuracy included.', 'model_keys': ['voltage', 'bms'], 'verification_obligation': 'Apply worst-case measurement error and boundary voltages against the selected-cell datasheet; demonstrate trip behavior.', 'unresolved_decisions': ['Cell datasheet values and voltage measurement uncertainty TBD; no numerical cell limits are invented.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.4.2:p001', 'source_pages': [107], 'source_text_sha256': 'be8ed7de2309ee444ef3d593628e4d01eaba0b7d8b687d84991209a0e36ac04e', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'SENSE-PROTECTION', 'identifier': 'FSAE2027:SD:obligation:SENSE-PROTECTION', 'obligation': 'Protect every BMS voltage-sense connection at the directly cell-tab-connected conductor/trace; protection voltage rating is at least maximum module voltage.', 'model_keys': ['sense-fuse', 'voltage'], 'verification_obligation': 'Inspect cell-tab protection placement, ratings and the blown/tripped-protection fault path.', 'unresolved_decisions': ['Module voltage, protection devices, exact tab/trace layout and fault-current ratings TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.4.3:p001', 'source_pages': [107], 'source_text_sha256': '28fcb7437c6269afae392dba3649f9af3a9a0572bacfbb071bdea2400d4dac75', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.4.3:p002', 'source_pages': [107], 'source_text_sha256': '93892a5733af9bae26eeacd2d2f2003b30e9b921f646cc31ae293498b9d772fe', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.4.3:p003', 'source_pages': [107], 'source_text_sha256': '45a9bec98d1599538910dc6103d174ca07fca36154b70b7fea8fb7bad8f477b6', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'CELL-TEMPERATURE', 'identifier': 'FSAE2027:SD:obligation:CELL-TEMPERATURE', 'obligation': 'Measure cell temperatures and keep them strictly below the lower of the cell-datasheet maximum and 60 degrees C, including measurement accuracy.', 'model_keys': ['temperature', 'bms'], 'verification_obligation': 'Test boundary temperatures with uncertainty; confirm the manufacturer\'s limit controls whenever it is below 60 degrees C.', 'unresolved_decisions': ['Cell temperature limit, sensing uncertainty and thermal placement TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.5.1:p001', 'source_pages': [107], 'source_text_sha256': 'fce3203e3e3ad5e0f65ed08540e8b88e283c314d33fcac5074bfeb6d495c69c4', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.5.2:p001', 'source_pages': [108], 'source_text_sha256': '15ca70f84b5950cb9ce3691e936fb0a4ae7d9020a49a270fe84bab7e92d0a87b', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.5.2:p002', 'source_pages': [108], 'source_text_sha256': '5f6f6dbeab565829b4361490b682e69d6c2724c87c16559283516eb8d218436c', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.5.2:p003', 'source_pages': [108], 'source_text_sha256': '999fa45eb1850a2c18911d8356a6a78687d0b749e75f2a02bab8f7914d902121', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'SENSOR-LOCATION', 'identifier': 'FSAE2027:SD:obligation:SENSOR-LOCATION', 'obligation': 'Sense at each applicable cell\'s negative terminal with direct contact to that terminal or its negative-terminal busbar less than 10 mm from the specified weld/clamp location.', 'model_keys': ['temperature'], 'verification_obligation': 'Inspect sensor contact and negative-terminal location; measure the strict less-than-10-mm alternative where used.', 'unresolved_decisions': ['Physical cells, weld/clamp geometry and sensor installation TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.5.3:p001', 'source_pages': [108], 'source_text_sha256': '484b26674c1d570d675decbfd3e150d4b44a1e0324c4e6c39a285bd7d4235156', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.5.4:p001', 'source_pages': [108], 'source_text_sha256': '9bbb6f3cd3ca488126e81803e62636b944f5142fb80ed23287db1d3b02141011', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.5.4:p002', 'source_pages': [108], 'source_text_sha256': '706c6dcefdcbc5e4b9c5a09a3e9f594e05a1cae470237a16996f853061e08007', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.5.4:p003', 'source_pages': [108], 'source_text_sha256': '41cf28d1efbb0168cbb9213752f767dcde8493dbbf64c0d73fa5a60019e10588', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'LITHIUM-COVERAGE', 'identifier': 'FSAE2027:SD:obligation:LITHIUM-COVERAGE', 'obligation': 'For lithium-based cells, monitor at least 20 percent, distributed equally across containers; measuring every cell is a recommendation retained separately.', 'model_keys': ['temperature', 'bms'], 'verification_obligation': 'If lithium cells are selected, inspect cell count and container distribution and verify at least 20-percent coverage; record the every-cell recommendation separately.', 'unresolved_decisions': ['Chemistry, cell count and container configuration TBD; lithium-specific applicability remains conditional.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.5.5:p001', 'source_pages': [108], 'source_text_sha256': '53e6a914f6b41aa871ce27d5d070c135e79dd51f2a46bd688593e909bb46b4b9', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.5.5:p002', 'source_pages': [108], 'source_text_sha256': 'afa9d42fa10d2f57aefff59c374cc765bc680075500ce79c4a73e663a23d3e9d', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.5.5:p003', 'source_pages': [108], 'source_text_sha256': '444ec42752f7b4e8360e1209eb6acb7b27ecd2a08ac5032a8fc032732e96fd86', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.5.5:p004', 'source_pages': [108], 'source_text_sha256': '66cec3a6ab0e7e09abc03b99a0fb68ca7ac99211dceee3ec6aeb2d049844608e', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'SHARED-SENSOR', 'identifier': 'FSAE2027:SD:obligation:SHARED-SENSOR', 'obligation': 'A temperature sensor may represent multiple cells only if all EV.8.5 conditions hold for every sensed cell.', 'model_keys': ['temperature'], 'verification_obligation': 'If sensors are shared, substantiate contact, coverage and isolation for every represented cell.', 'unresolved_decisions': ['Shared-sensor selection and applicability TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.5.6:p001', 'source_pages': [108], 'source_text_sha256': '637f3676fc8dc5b649474f8771bcb2983d4312019e267c2fdd8f889d67a671c4', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'SENSOR-ISOLATION', 'identifier': 'FSAE2027:SD:obligation:SENSOR-ISOLATION', 'obligation': 'Provide suitable temperature-sensing isolation at the sensor/cell boundary or in the sensing circuit, accounting for TS/GLV isolation and sense-location common-mode voltage.', 'model_keys': ['temperature', 'bms'], 'verification_obligation': 'Test selected isolation barriers at required TS/GLV and common-mode conditions.', 'unresolved_decisions': ['Isolation topology, ratings and common-mode envelope TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.5.7:p001', 'source_pages': [108], 'source_text_sha256': '1e01220bd85ea92fdb41a2c0bccdd54af0ffae98c482bfce8196952a73f8a4ce', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.5.7:p002', 'source_pages': [108], 'source_text_sha256': '0effa58babe5e044a15b5f7495341b7cc18862371f2b98c8bd05d212fe6857f6', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.5.7:p003', 'source_pages': [108], 'source_text_sha256': 'f98047edfc86115e268e370dd01b7090ce9c3dc017aa40b4661b01b7c7357eb4', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.5.7:p004', 'source_pages': [108], 'source_text_sha256': '002c012048d20cdaaae3c69b5c2cd6e454e4ebd90b0d9aec6a3adf498aa8ed01', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'IMD-INSTALL', 'identifier': 'FSAE2027:SD:obligation:IMD-INSTALL', 'obligation': 'Install an insulation monitor in the tractive system.', 'model_keys': ['imd'], 'verification_obligation': 'Inspect the actual tractive-system IMD connections and exercise insulation sensing at the installed interface.', 'unresolved_decisions': [], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.6.1:p001', 'source_pages': [108], 'source_text_sha256': '69f42ea30b7505be455a8ef02c4f484fe4177bee0a02d7f945c373a05fe1c42e', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'IMD-APPROVAL', 'identifier': 'FSAE2027:SD:obligation:IMD-APPROVAL', 'obligation': 'Choose an IMD model explicitly approved by the applicable official event Rules FAQ.', 'model_keys': ['imd'], 'verification_obligation': 'Compare the selected exact part/model with the pinned applicable official FAQ approval list.', 'unresolved_decisions': ['Approved IMD model and applicable official Rules FAQ are TBD; this draft selects no part.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.6.2:p001', 'source_pages': [108], 'source_text_sha256': 'f829dc0de7028ca80bc35f75ed242b82195828d8dcbecb2c0ed07d0a66527e77', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'IMD-THRESHOLD', 'identifier': 'FSAE2027:SD:obligation:IMD-THRESHOLD', 'obligation': 'Set IMD response to at least 500 ohms per volt of maximum TS operating voltage.', 'model_keys': ['imd'], 'verification_obligation': 'Use the actual maximum operating voltage to calculate the response resistance and inject insulation values around the selected threshold.', 'unresolved_decisions': ['Maximum operating voltage and resulting resistance setting TBD; the 600-V rule ceiling is not assumed to be the design voltage.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.6.3:p001', 'source_pages': [108], 'source_text_sha256': 'c9e78d7603203b34ecfbfba1c8843740fbcd25c5dcbca3600fb9994a94067150', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'IMD-INDEPENDENT', 'identifier': 'FSAE2027:SD:obligation:IMD-INDEPENDENT', 'obligation': 'Detect insulation failure and IMD malfunction without influence from programmable logic.', 'model_keys': ['imd', 'accept', 'cutout'], 'verification_obligation': 'Inject insulation/monitor faults while stopping or corrupting programmable controllers; prove independent shutdown.', 'unresolved_decisions': ['Nonprogrammable IMD safety output and independent wiring details TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.6.4:p001', 'source_pages': [108], 'source_text_sha256': '0727b2fde3a3d8cafa24a908b72c1019a6105fa90f379c0f595a78e541c00266', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.6.4:p002', 'source_pages': [108], 'source_text_sha256': '764e9faa3f0acf13005dbbf794e975af5c93e19dc920493f29edd326489c25fa', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.6.4:p003', 'source_pages': [108], 'source_text_sha256': 'e08a9d28a781ab6f9e12d2bda41e99992e35c2c6a53f5fe70efa81f7f1c460a6', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.6.4:p004', 'source_pages': [108], 'source_text_sha256': '6f96fef9ca934d6976bd9c74dc5ac39aa33942761c873cbe967aa22ad90c9ba4', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'IMD-TRIP-INDICATION', 'identifier': 'FSAE2027:SD:obligation:IMD-TRIP-INDICATION', 'obligation': 'IMD faults open shutdown and latch IMD warning plus TS-status indication until manual IMD reset.', 'model_keys': ['imd', 'accept', 'latch', 'indicate'], 'verification_obligation': 'Inject insulation and monitor faults; verify open circuit and retained indications until permitted reset.', 'unresolved_decisions': ['TS status indication EV.6.1.6e implementation is a dependent slice.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.6.5:p001', 'source_pages': [108], 'source_text_sha256': '77db6931417f63d30f7bd68420bf70238fa5c64327e1fe98640d1c27a1baf92a', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.6.5:p002', 'source_pages': [108], 'source_text_sha256': '0a68182d54dbcc6988778bf115a863d17c4beef2a79b4d488a1f45998aa29efd', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.6.5:p003', 'source_pages': [108], 'source_text_sha256': 'dc983586b3527376833da9c003bea4fd87b15a0425bff72c61e4da151ace62cd', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.6.5:p004', 'source_pages': [108], 'source_text_sha256': '7fab3aa3cb1569bdab0967193ee98cd11e4fbabf41666471f46c8f77caa8fb18', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'IMD-LAMP', 'identifier': 'FSAE2027:SD:obligation:IMD-LAMP', 'obligation': 'Provide a red driver-visible IMD-marked warning in bright sunlight and perform the EV.6.3 lamp check.', 'model_keys': ['indicate', 'lamp-check'], 'verification_obligation': 'Inspect marking/color and seated-driver sunlight visibility; execute the detailed EV.6.3 check.', 'unresolved_decisions': ['IMD lamp mounting/brightness and detailed EV.6.3 sequence TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.6.6:p001', 'source_pages': [108], 'source_text_sha256': 'd91bbb4d65f58db8574025f9b85b959c803b5f7c1d3b570fefff4d6c3eb31508', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.6.6:p002', 'source_pages': [108], 'source_text_sha256': 'f871e1a6da1f10ce9865651265aea732e1fe3760ad7c3a3d2795dc1254a171ed', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.6.6:p003', 'source_pages': [108], 'source_text_sha256': '03328a62fd70f4d85b061d721ac35891bfc7667e9214367fb9348a255bb65046', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.6.6:p004', 'source_pages': [108], 'source_text_sha256': 'dfe40fdc19478c033663f278386f80ba4de03cdb07b4da277d60d310959e93df', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.6.7:p001', 'source_pages': [109], 'source_text_sha256': '60647c6f85cdfc1d0cf7b9a0e5d3ff70bc372fc52728ad611e7a48d2b6294d91', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'INTERLOCK-LOCATIONS', 'identifier': 'FSAE2027:SD:obligation:INTERLOCK-LOCATIONS', 'obligation': 'Install required interlocks at EV.4.1.3, EV.5.5.2 and EV.7.4.4 interfaces; additional interlocks are permitted, not automatically required.', 'model_keys': ['interlocks', 'series'], 'verification_obligation': 'Review all three referenced interfaces, map every required connection and test each break independently.', 'unresolved_decisions': ['Exact disconnect/enclosure/connector map and optional additional interlocks TBD; referenced clauses need full review.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.8.1:p001', 'source_pages': [109], 'source_text_sha256': '667ea8ad1d910368998fd520952e6d1bfa69f5eb3c1fcbad00a5566bebb18896', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.8.2:p001', 'source_pages': [109], 'source_text_sha256': 'bfe55e807b83e7566c11c622864cd3541c12790eaeaa5e3dc0b44844d996aa89', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'INTERLOCK-OPEN', 'identifier': 'FSAE2027:SD:obligation:INTERLOCK-OPEN', 'obligation': 'A broken or interrupted interlock connection opens shutdown.', 'model_keys': ['interlocks', 'accept', 'cutout'], 'verification_obligation': 'Break each connector/interlock connection and verify isolation/precharge drive current stops.', 'unresolved_decisions': [], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.8.3:p001', 'source_pages': [109], 'source_text_sha256': '53374b4c7f08a44968b722c475182249bda2a9d56e0e190dfe65eaa22411ff34', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'INTERLOCK-ROUTING', 'identifier': 'FSAE2027:SD:obligation:INTERLOCK-ROUTING', 'obligation': 'Apply any EV.7.5 interlock-separation exception only after reviewing EV.8.8.4\'s listed harness, connector and enclosure/location conditions, including the less-than-75-mm condition.', 'model_keys': ['interlocks', 'series'], 'verification_obligation': 'Inspect actual routing and evaluate every applicable exception condition and the strict 75-mm boundary.', 'unresolved_decisions': ['Routing design and precise applicability of the listed exception conditions TBD; this draft does not automatically waive separation.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.8.4:p001', 'source_pages': [109], 'source_text_sha256': '92f8202aa0f4a71bc8897e312e960e3d9187712898873a7de99b6a51d3764aff', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.8.4:p002', 'source_pages': [109], 'source_text_sha256': '9c94e9ce84812e608ac10f49da256b4b96175aa3b09189bad8888e4217c517e2', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.8.4:p003', 'source_pages': [109], 'source_text_sha256': '63fc1a53282a715ce48e0475be3be5ff8cb10093651d5b21bb8aef7de6ece0cf', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.8.4:p004', 'source_pages': [109], 'source_text_sha256': 'e9166fb6a2287e7c515a500304e8a1862fa8f384363af95b734b0998785f8a72', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'MASTERS-DIRECT', 'identifier': 'FSAE2027:SD:obligation:MASTERS-DIRECT', 'obligation': 'Provide two directly acting master switches meeting T.9.3 location/configuration; do not substitute relay or logic control.', 'model_keys': ['switches', 'series'], 'verification_obligation': 'Inspect distinct GLV/TS switches, direct-current contacts and T.9.3 location/configuration.', 'unresolved_decisions': ['Switch parts and T.9.3 packaging review TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.9.1:p001', 'source_pages': [110], 'source_text_sha256': 'c91c83b989224cbd9aa77386a596e93f33440ea54b262969b063500d5740e935', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.9.1:p002', 'source_pages': [110], 'source_text_sha256': '18f51d3a7de1331e42aa92d911f49b843785c692f985f36189a3e80e3bd9db4b', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.9.1:p003', 'source_pages': [110], 'source_text_sha256': '8e7918d152526e98e192f5813b5d7d64f2aa19e4a8ae919c9ccd098677033378', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'GLV-MASTER', 'identifier': 'FSAE2027:SD:obligation:GLV-MASTER', 'obligation': 'GLVMS stops all GLV power and is labeled LV within a completely red circle greater than 50 mm diameter.', 'model_keys': ['glv-master', 'glv', 'switches', 'series'], 'verification_obligation': 'Operate GLVMS and measure every GLV supply off; inspect marking/color and a circle strictly larger than 50 mm.', 'unresolved_decisions': ['GLV power-distribution completeness and physical switch marking layout TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.9.2:p001', 'source_pages': [110], 'source_text_sha256': 'cb83a38edb8369b0bdeb3e64f27f66f97ed8c753379e86c2cb7cd7feca7a94ab', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.9.2:p002', 'source_pages': [110], 'source_text_sha256': 'e86c60f3613814361a2003762a95a0470726cea63efe8bf33e46efb656a0642a', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.9.2:p003', 'source_pages': [110], 'source_text_sha256': '64b9855b55517f3a7e90bc7024e1b1748aa707f86962a56fd4125f54918311cf', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.9.2:p004', 'source_pages': [110], 'source_text_sha256': '34edf58707b8e2260e34f05f5f26b0387f45f0922cbce62c881aa5eb95801274', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'TS-MASTER', 'identifier': 'FSAE2027:SD:obligation:TS-MASTER', 'obligation': 'TSMS opens shutdown when OFF; it is the last switch before IRs except precharge circuitry/interlocks, has an orange circle greater than 50 mm, TS/W012 marking and OFF lockout/tagout.', 'model_keys': ['switches', 'series', 'coils'], 'verification_obligation': 'Inspect electrical order, OFF opening, lockout/tagout and the required orange area and TS/ISO-7010-W012 marking.', 'unresolved_decisions': ['TSMS/contact selection, exact packaging, official symbol implementation and EV.12.3.1 process TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.9.3:p001', 'source_pages': [110], 'source_text_sha256': '744bfe9523aeef31f39f3fcacede2779b8c86741d71ba86a80dce75a0685bcc5', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.9.3:p002', 'source_pages': [110], 'source_text_sha256': '34f5f8f9d41f9b2385979da46d33cb2e13d213b0f59fb5700f89d656845e88ad', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.9.3:p003', 'source_pages': [110], 'source_text_sha256': '8560c8a3b231acb75af6c809b56e0ad611c6027c22fef337de85d4bd03e8a7c1', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.9.3:p004', 'source_pages': [110], 'source_text_sha256': '5902bee526ca4f2748681dac0b4de1b7b2098efd43e1752576c55580b37f785d', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.9.3:p005', 'source_pages': [110], 'source_text_sha256': 'ddafdf0600023176dcffe295633b4ed7ccaf41a8c5be73bd5da8349336ccc5c7', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.9.3:p006', 'source_pages': [110], 'source_text_sha256': '177f4facfaa44bd23b61d25615cefc5e22d97e3bcf678c393f2adb441d5fa0c3', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'BUTTON-COUNT', 'identifier': 'FSAE2027:SD:obligation:BUTTON-COUNT', 'obligation': 'Install the three required shutdown buttons: left side, right side and cockpit.', 'model_keys': ['switches', 'series'], 'verification_obligation': 'Locate and independently operate all three required buttons.', 'unresolved_decisions': [], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.10.1:p001', 'source_pages': [110], 'source_text_sha256': '8b179556fa3cdd006926d153578d9c2d98c5450c91a02e6cc967a4ebfa073258', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'BUTTON-MECHANISM', 'identifier': 'FSAE2027:SD:obligation:BUTTON-MECHANISM', 'obligation': 'Use push-pull or push-rotate emergency switches that open and hold OFF and permit closure ON, while never overriding a latched protective trip.', 'model_keys': ['switches', 'series', 'latch'], 'verification_obligation': 'Inspect mechanisms and test hold-OFF/ON behavior both with and without a latched protective trip.', 'unresolved_decisions': ['Selected emergency-stop hardware and current ratings TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.10.2:p001', 'source_pages': [110], 'source_text_sha256': 'aed109a4c6b459e406d97da650883a8147c7d6de4e2c493d4dd999bc1cacc5e5', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.10.2:p002', 'source_pages': [110], 'source_text_sha256': '43c6deb18a386c6876d2085dffdfda44641ff3920378e9cdf7f87826391454e6', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.10.2:p003', 'source_pages': [110], 'source_text_sha256': '233c47ed43373c143256186a230a5b9f5bbe0c97423498db64b49d10bc33cbf1', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.10.2:p004', 'source_pages': [110], 'source_text_sha256': '45b7f7aca64570b9beca1b3dcbbca0bafd8864bde577f8aceaf8f3446ccffe88', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.10.2:p005', 'source_pages': [110], 'source_text_sha256': '8d6b5e766ab5784444969c17c8b5f3f28be5b1481c5a2f984d099964d5c69b40', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'BUTTON-SIDES', 'identifier': 'FSAE2027:SD:obligation:BUTTON-SIDES', 'obligation': 'Place left/right buttons aft of the main hoop near the specified bracing junction; each is at least 40 mm diameter and not easily removed or mounted on removable bodywork.', 'model_keys': ['switches'], 'verification_obligation': 'Inspect both side installations, retention, bodywork and minimum 40-mm diameter.', 'unresolved_decisions': ['Chassis packaging, F.5.9 junction location and mounting/retention design TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.10.3:p001', 'source_pages': [110], 'source_text_sha256': '7b00b8a9921299250af8c0690a3ab8e9d9f414ad222a939fd08edda20f784ff2', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.10.3:p002', 'source_pages': [110], 'source_text_sha256': '608321a536e5bce73c913e4a96b39d5a77b6bc90953538c995df9d9e6ed2ca63', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.10.3:p003', 'source_pages': [110], 'source_text_sha256': 'fe95fa65d2fae10185be70374bd949f9a9b3286abab3b731c77cc76a54103063', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.10.3:p004', 'source_pages': [110], 'source_text_sha256': '78aac798c8ea6b21210412a2a249d3c2406a1475620ff43935d5ab6675612a34', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'BUTTON-COCKPIT', 'identifier': 'FSAE2027:SD:obligation:BUTTON-COCKPIT', 'obligation': 'Place the cockpit button within easy unobstructed reach of the belted driver adjacent to the steering wheel; diameter is at least 24 mm.', 'model_keys': ['switches'], 'verification_obligation': 'Test reach with the driver restrained and inspect wheel/other obstructions and minimum 24-mm diameter.', 'unresolved_decisions': ['Driver envelope, cockpit geometry and button installation TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.10.4:p001', 'source_pages': [110], 'source_text_sha256': '91d13e3c7b98d851381b4570cb88284f84a0549342c2f16ba9613ec0ceab9e5d', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.10.4:p002', 'source_pages': [110], 'source_text_sha256': '832f912279a54ba5f88d52c2576634da5be0844d549bd8d5d19c3a614f4a2308', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110'], ['clause_id': 'FSAE-2027-V1:EV.8.10.4:p003', 'source_pages': [110], 'source_text_sha256': '577f36fc65d9c4fe69a3c5d255da89666f0b29caa21c83d34051e836c0530a69', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]], ['key': 'BUTTON-SYMBOL', 'identifier': 'FSAE2027:SD:obligation:BUTTON-SYMBOL', 'obligation': 'Place the specified red-spark electrical symbol on a white-edged blue triangle near every shutdown button.', 'model_keys': ['switches'], 'verification_obligation': 'Inspect symbol identity and placement beside each of the three buttons.', 'unresolved_decisions': ['Final marking artwork and installation TBD.'], 'source_clauses': [['clause_id': 'FSAE-2027-V1:EV.8.10.5:p001', 'source_pages': [110], 'source_text_sha256': '510f126f710aaef5b4505e07c2ff127557384d0aa7fcc9d9cb44d3f857bf22f1', 'source_review_status': 'visually_checked_prose_on_pages_105_to_110']]]]
obligations.each { group ->
    assert req.find(group.identifier) == null: 'A derived shutdown obligation already exists'
    group.source_clauses.each { source ->
        def requirement = sourceRequirements[source.clause_id]
        assert requirement != null: 'Import the exact pinned source clauses before this slice'
        def attributes = req.attributes(requirement)
        assert attributes.source_sha256 == 'f71df5552e341dab1b58f1ff47212e7281953d66b9c41833de8347560913ed0e'
        assert attributes.source_text_sha256 == source.source_text_sha256
        assert attributes.coverage_status == 'not_modeled': 'An existing source allocation must be reviewed before extending it'
        assert attributes.verification_status == 'not_verified': 'Do not overwrite existing verification evidence'
    }
}

def domainDescriptions = [shutdown:'Prevent or interrupt unsafe tractive energization',
    'energy-storage':'Store and supply traction energy', 'tractive-distribution':'Distribute and isolate tractive electrical energy',
    powertrain:'Convert traction energy into vehicle motion', status:'Indicate electrical and readiness states',
    charging:'Recharge traction energy under protected conditions', 'low-voltage':'Supply and manage low voltage electrical power']
def domains = domainDescriptions.collectEntries { key, description ->
    def result = [logical:api.find('LogicalComponent', 'FSAE2027:' + key + ' Logical subsystem'),
        behavior:api.find('PhysicalComponent', 'FSAE2027:' + key + ' Physical behavior'),
        equipment:api.find('PhysicalComponent', 'FSAE2027:' + key + ' Equipment'),
        ci:api.find('ConfigurationItem', 'FSAE2027:' + key + ' Subsystem CI')]
    [OA:'OperationalActivity', SA:'SystemFunction', LA:'LogicalFunction', PA:'PhysicalFunction'].each { layer, type ->
        result[layer] = api.find(type, 'FSAE2027:' + key + ' ' + description)
    }
    assert result.values().every { it != null }: 'Missing acceptance bootstrap domain ' + key
    [(key):result]
}
// OperationalActor is a creation alias; native operational actors have EClass Entity.
def operationalActors = ['Driver','Team personnel'].collectEntries { name ->
    def actor = api.find('Entity', 'FSAE2027:' + name + ' Operational actor')
    assert actor != null && actor.actor == true: 'Missing native operational actor ' + name
    [(name):actor]
}
def operationalVehicle = api.find('Entity', 'FSAE2027:Vehicle in operational context')
assert operationalVehicle != null && operationalVehicle.actor == false
def components = [:]
def functions = [:]
def hardware = [:]
def created = [:]
def state = [slice_id:'FSAE2027:shutdown-v1', completion_status:'partial_candidate_design',
    verification_status:'not_verified', components:[:], functions:[:], source_allocations:[:],
    derived_requirements:[:], verification_constraints:[:], physical_links:[], transitions:[]]
def identify = { element, key ->
    def id = stableId(key)
    assert !existingIds.contains(id): 'Conflicting deterministic shutdown ID ' + key
    // The facade synchronizes native XMLResource ID maps and follows command undo/redo.
    assert api.set(element, 'id', id)
    existingIds.add(id)
    created[key] = element
    element
}
def expression = { text ->
    def value = org.polarsys.capella.core.data.information.datavalue.DatavalueFactory.eINSTANCE.createOpaqueExpression()
    value.languages.add('Text')
    value.bodies.add(text)
    value
}
def constraint = { key, name, text, references, owner = null ->
    def value = identify(api.create('Constraint', prefix + name, owner ?: api.layer('SA')), 'constraint:' + key)
    value.ownedSpecification = expression(text)
    value.constrainedElements.addAll(references)
    value.summary = 'NOT_VERIFIED. Source-grounded candidate design obligation; hardware evidence and dependent decisions remain open.'
    value
}
def realizeExchange = { lower, upper, key ->
    def realization = identify(org.polarsys.capella.core.data.fa.FaFactory.eINSTANCE.createFunctionalExchangeRealization(), 'exchange-realization:' + key)
    realization.sourceElement = lower
    realization.targetElement = upper
    lower.ownedFunctionalExchangeRealizations.add(realization)
    ['source','target'].each { side ->
        def from = lower[side]
        def to = upper[side]
        if (from instanceof org.polarsys.capella.core.data.information.Port && to instanceof org.polarsys.capella.core.data.information.Port) {
            def portRealization = identify(org.polarsys.capella.core.data.information.InformationFactory.eINSTANCE.createPortRealization(), 'port-realization:' + key + ':' + side)
            portRealization.sourceElement = from
            portRealization.targetElement = to
            from.ownedPortRealizations.add(portRealization)
        }
    }
}
def createHardware = { key, name, domain, kind ->
    def node = identify(api.create('PhysicalComponent', prefix + name, domains[domain].equipment), 'node:' + key)
    node.nature = PhysicalComponentNature.NODE
    node.kind = PhysicalComponentKind.HARDWARE
    node.summary = 'Candidate hardware/interface. Exact parts, ratings, packaging and physical verification remain TBD.'
    def ci = identify(api.create('ConfigurationItem', prefix + name + ' CI', domains[domain].ci), 'ci:' + key)
    ci.kind = kind
    ci.itemIdentifier = 'FSAE2027:SD:' + key
    api.link(ci, node, 'PhysicalArtifactRealization')
    hardware[key] = node
    [node:node, ci:ci]
}

api.batch('Allocate EV.8 shutdown candidate safety behavior and native traceability') {
    def specifications = [
        [key:'bms', domain:'energy-storage', logical:'Independent BMS Logical monitor', behavior:'Independent BMS Physical monitor', equipment:'BMS safety monitor Equipment'],
        [key:'imd', domain:'shutdown', logical:'Independent IMD Logical monitor', behavior:'Independent nonprogrammable IMD Physical circuit', equipment:'IMD Equipment approved model TBD'],
        [key:'series', domain:'shutdown', logical:'Hardwired shutdown current path Logical subsystem', behavior:'Hardwired high-side shutdown Physical circuit', equipment:'Shutdown current path Equipment'],
        [key:'latch', domain:'shutdown', logical:'Local manual-reset latch Logical subsystem', behavior:'Independent protective-trip latch Physical circuit', equipment:'Protective-trip latch Equipment'],
        [key:'coils', domain:'tractive-distribution', logical:'Isolation precharge and discharge Logical interface', behavior:'Isolation precharge and discharge Physical behavior', equipment:'Isolation and discharge Equipment design TBD'],
        [key:'torque', domain:'powertrain', logical:'Shutdown torque removal Logical interface', behavior:'Shutdown torque removal Physical behavior', equipment:'Inverter shutdown interface Equipment'],
        [key:'indicate', domain:'status', logical:'Latched BMS IMD indication Logical subsystem', behavior:'BMS IMD warning and lamp-check Physical behavior', equipment:'Red BMS IMD warning Equipment']
    ]
    specifications.each { specification ->
        def key = specification.key
        def logical = identify(api.create('LogicalComponent', prefix + specification.logical, domains[specification.domain].logical), 'logical:' + key)
        def behavior = identify(api.create('PhysicalComponent', prefix + specification.behavior, domains[specification.domain].behavior), 'behavior:' + key)
        behavior.nature = PhysicalComponentNature.BEHAVIOR
        def equipment = createHardware(key, specification.equipment, specification.domain, ConfigurationItemKind.HWCI)
        api.link(behavior, logical, 'realization')
        def behaviorPart = api.ensurePart(behavior)
        def equipmentPart = api.ensurePart(equipment.node)
        def deployment = identify(org.polarsys.capella.core.data.pa.deployment.DeploymentFactory.eINSTANCE.createPartDeploymentLink(), 'deployment:' + key)
        deployment.deployedElement = behaviorPart
        deployment.location = equipmentPart
        equipmentPart.ownedDeploymentLinks.add(deployment)
        api.link(equipment.ci, behavior, 'PhysicalArtifactRealization')
        components[key] = [logical:logical, behavior:behavior, equipment:equipment.node, ci:equipment.ci]
        state.components[key] = [logical:api.id(logical), behavior:api.id(behavior), equipment:api.id(equipment.node), ci:api.id(equipment.ci)]
    }

    def functionSpecifications = [
        [key:'series', name:'Carry high-side shutdown current', owner:'series', domain:'shutdown'],
        [key:'accept', name:'Accept independent shutdown trips', owner:'series', domain:'shutdown'],
        [key:'cutout', name:'Open high-side shutdown current path', owner:'series', domain:'shutdown'],
        [key:'latch', name:'Latch BMS IMD BSPD trip until local manual reset', owner:'latch', domain:'shutdown'],
        [key:'reset', name:'Validate local manual reset with faults cleared', owner:'latch', domain:'shutdown'],
        [key:'coils', name:'Remove isolation and precharge relay coil drive', owner:'coils', domain:'tractive-distribution'],
        [key:'current', name:'Interrupt all tractive battery current', owner:'coils', domain:'tractive-distribution'],
        [key:'discharge', name:'Reduce residual TS voltage to low voltage within 5 seconds', owner:'coils', domain:'tractive-distribution'],
        [key:'torque', name:'Remove motor torque on shutdown', owner:'torque', domain:'powertrain'],
        [key:'glv', name:'Open shutdown current path on any GLV loss', owner:'series', domain:'shutdown'],
        [key:'glv-supply', name:'Observe GLV supply presence and loss', owner:'series', domain:'low-voltage'],
        [key:'glv-master', name:'Remove all GLV power through direct GLVMS', owner:'series', domain:'low-voltage'],
        [key:'switches', name:'Interrupt shutdown current through direct manual switches', owner:'series', domain:'shutdown'],
        [key:'interlocks', name:'Open shutdown path on broken interlock connection', owner:'series', domain:'shutdown'],
        [key:'voltage', name:'Measure every required cell-group voltage and apply datasheet limits', owner:'bms', domain:'energy-storage'],
        [key:'temperature', name:'Measure negative-terminal temperatures and apply lower safe limit', owner:'bms', domain:'energy-storage'],
        [key:'sense-fuse', name:'Detect tripped cell-tab voltage-sense protection', owner:'bms', domain:'energy-storage'],
        [key:'bms-health', name:'Detect lost measurements and BMS internal faults', owner:'bms', domain:'energy-storage'],
        [key:'bms', name:'Issue independent BMS trip for any monitored fault', owner:'bms', domain:'energy-storage'],
        [key:'balance', name:'Inhibit cell balancing whenever shutdown is open', owner:'bms', domain:'energy-storage'],
        [key:'imd', name:'Independently detect insulation and IMD operational faults', owner:'imd', domain:'shutdown'],
        [key:'indicate', name:'Latch BMS IMD and TS fault indications until manual reset', owner:'indicate', domain:'status'],
        [key:'lamp-check', name:'Check BMS and IMD warning lamps', owner:'indicate', domain:'status'],
        [key:'demonstrate', name:'Provide shutdown test observability', owner:'series', domain:'shutdown']
    ]
    functionSpecifications.each { specification ->
        def value = [owner:specification.owner]
        [SA:'SystemFunction', LA:'LogicalFunction', PA:'PhysicalFunction'].each { layer, type ->
            def function = identify(api.create(type, prefix + specification.name, domains[specification.domain][layer]), 'function:' + layer + ':' + specification.key)
            api.allocate(function, layer == 'SA' ? api.layer('SA').system : components[specification.owner][layer == 'LA' ? 'logical' : 'behavior'])
            function.summary = 'Candidate shutdown behavior. Threshold hardware, details and tests remain explicitly unresolved.'
            value[layer] = function
        }
        api.link(value.LA, value.SA, 'realization')
        api.link(value.PA, value.LA, 'realization')
        functions[specification.key] = value
        state.functions[specification.key] = [SA:api.id(value.SA), LA:api.id(value.LA), PA:api.id(value.PA)]
    }
    // Operational leaf interfaces let the independent BSPD slice connect without using an aggregate.
    ['accept','torque'].each { key ->
        def activity = identify(api.create('OperationalActivity', functions[key].SA.name, domains.shutdown.OA), 'function:OA:' + key)
        api.allocate(activity, operationalVehicle)
        activity.summary = 'Operational shutdown interface; physical implementation and verification remain pending.'
        api.link(functions[key].SA, activity, 'realization')
        functions[key].OA = activity
        state.functions[key].OA = api.id(activity)
    }
    def routes = [
        ['voltage','bms','Cell voltage and sense validity'], ['temperature','bms','Cell temperature and sense validity'],
        ['sense-fuse','bms','Cell-tab protection fault'], ['bms-health','bms','Measurement loss or internal BMS fault'],
        ['bms','accept','Independent BMS shutdown trip'], ['imd','accept','Independent IMD shutdown trip'],
        ['glv','accept','GLV supply loss'], ['switches','accept','Direct manual switch opening'],
        ['glv-master','glv','Master switch removes all GLV supply'],
        ['interlocks','accept','Interlock continuity lost'], ['accept','cutout','Any series path open request'],
        ['accept','latch','BMS IMD BSPD protective-trip cause'], ['latch','cutout','Latched protective inhibit'],
        ['reset','latch','Permitted local manual reset only'], ['cutout','coils','Coil drive removed'],
        ['coils','current','Isolation relays release and interrupt current'], ['cutout','discharge','Residual energy discharge required'],
        ['cutout','torque','Torque removed and motor free-spin'], ['cutout','balance','Open shutdown balancing inhibit'],
        ['bms','indicate','BMS warning and TS-status latch'], ['imd','indicate','IMD warning and TS-status latch'],
        ['latch','indicate','Retain trip indications until permitted reset'], ['lamp-check','indicate','Warning lamp check']
    ]
    routes.each { route ->
        ['SA','LA','PA'].each { layer ->
            def link = api.link(functions[route[0]][layer], functions[route[1]][layer], 'FunctionalExchange')
            identify(link, 'flow:' + layer + ':' + route[0] + ':' + route[1])
            api.rename(link, prefix + layer + ' ' + route[2])
        }
    }
    ['SA','LA','PA'].each { layer ->
        ['voltage','temperature'].each { key ->
            def link = api.link(domains.charging[layer], functions[key][layer], 'FunctionalExchange')
            identify(link, 'flow:' + layer + ':charging:' + key)
            api.rename(link, prefix + layer + ' Monitor while battery connected to charger ' + key)
        }
        def link = api.link(functions['glv-supply'][layer], functions.glv[layer], 'FunctionalExchange')
        identify(link, 'flow:' + layer + ':low-voltage:glv')
        api.rename(link, prefix + layer + ' Loss of GLV supply')
    }
    created.keySet().findAll { it.startsWith('flow:SA:') }.collect { it.substring('flow:SA:'.length()) }.each { key ->
        realizeExchange(created['flow:LA:' + key], created['flow:SA:' + key], 'LA:' + key)
        realizeExchange(created['flow:PA:' + key], created['flow:LA:' + key], 'PA:' + key)
    }

    // Hardwired current topology uses real native PhysicalPorts, Parts and PhysicalLinks.
    def cs = org.polarsys.capella.core.data.cs.CsFactory.eINSTANCE
    def contacts = [
        [key:'GLVMS', name:'Direct GLV master switch contact'], [key:'BMS-CONTACT', name:'Normally-open independent BMS shutdown contact'],
        [key:'IMD-CONTACT', name:'Normally-open independent IMD shutdown contact'], [key:'BSPD-CONTACT', name:'Normally-open BSPD series contact interface'],
        [key:'INERTIA', name:'Inertia switch contact'], [key:'BUTTON-LEFT', name:'Left-side shutdown button'],
        [key:'BUTTON-RIGHT', name:'Right-side shutdown button'], [key:'BUTTON-COCKPIT', name:'Cockpit shutdown button'],
        [key:'BOTS', name:'Brake-over-travel direct switch contact'], [key:'TSMS', name:'Direct lockable TS master switch contact'],
        [key:'INTERLOCKS', name:'Required interlock series-chain interface locations TBD']
    ]
    def port = { key, node, direction ->
        def value = identify(cs.createPhysicalPort(), 'physical-port:' + key + ':' + direction)
        value.name = prefix + key + ' shutdown-current ' + direction
        node.ownedFeatures.add(value)
        value
    }
    def physicalInterface = { key, name, domain ->
        def value = createHardware(key, name, domain, ConfigurationItemKind.INTERFACE_CI)
        value.input = port(key, value.node, 'in')
        value.output = port(key, value.node, 'out')
        value
    }
    def chain = contacts.collect { contact ->
        def value = physicalInterface(contact.key, contact.name, 'shutdown')
        value.node.summary = contact.key in ['BMS-CONTACT','IMD-CONTACT','BSPD-CONTACT']
            ? 'Normally-open high-side protective contact; sustaining-power loss opens it. Independent circuitry and no-backfeed design TBD.'
            : 'Direct current-carrying series shutdown contact/interface; hardware rating, exact packaging and referenced requirements TBD.'
        if (contact.key in ['BMS-CONTACT','IMD-CONTACT','BSPD-CONTACT']) {
            def normalOpen = identify(org.polarsys.capella.core.data.capellacore.CapellacoreFactory.eINSTANCE.createBooleanPropertyValue(), 'property:normally-open:' + contact.key)
            normalOpen.name = 'Normally open protective output'
            normalOpen.value = true
            value.node.ownedPropertyValues.add(normalOpen)
        }
        value.key = contact.key
        value
    }
    def supply = physicalInterface('GLV-SUPPLY', 'GLV battery and shutdown supply interface', 'low-voltage')
    def returnPath = physicalInterface('GLV-RETURN', 'GLV coil return interface', 'low-voltage')
    def allGlvLoads = physicalInterface('ALL-GLV-LOADS', 'All GLV loads power-distribution boundary details TBD', 'low-voltage')
    def coils = ['IR-POSITIVE','IR-NEGATIVE','PRECHARGE-RELAY'].collect { key ->
        physicalInterface(key, key + ' coil interface ratings TBD', 'tractive-distribution') + [key:key]
    }
    def wire = { key, source, target ->
        def link = identify(cs.createPhysicalLink(), 'wire:' + key)
        link.name = prefix + 'Current path ' + key
        [source,target].eachWithIndex { endpoint, index ->
            def end = identify(cs.createPhysicalLinkEnd(), 'wire-end:' + key + ':' + index)
            end.part = api.ensurePart(endpoint.node)
            end.port = index == 0 ? endpoint.output : endpoint.input
            link.ownedPhysicalLinkEnds.add(end)
            link.linkEnds.add(end)
        }
        api.layer('PA').system.ownedPhysicalLinks.add(link)
        state.physical_links << [id:api.id(link), source:api.id(source.node), target:api.id(target.node)]
        link
    }
    def ordered = [supply] + chain
    (0..ordered.size()-2).each { index -> wire('series-' + index, ordered[index], ordered[index+1]) }
    wire('all-glv-loads-supply', chain[0], allGlvLoads)
    wire('all-glv-loads-return', allGlvLoads, returnPath)
    coils.each { coil ->
        wire('load-' + coil.key, chain[-1], coil)
        wire('return-' + coil.key, coil, returnPath)
    }
    constraint('HIGH-SIDE-TOPOLOGY', 'High-side direct current and no backfeed INVARIANT',
        'Every listed contact directly carries the high-side sustaining current to all isolation/precharge coil branches. TSMS is the final switch, followed only by permitted interlocks/precharge circuitry. No bypass or backfeed source may energize these loads after any contact opens. Ratings and exact wiring remain TBD.',
        chain.collect { it.node } + coils.collect { it.node } + [supply.node,returnPath.node])
    constraint('DEPENDENCIES', 'External safety dependencies TBD',
        'Review T.3.3 BOTS, T.9.4 inertia switching, T.9.1.2 low-voltage limits, T.9.3 master-switch configuration/location, EV.5.4 isolation relays, EV.6 status/lamp check, EV.8.7 independent BSPD, EV.9 charging, EV.12 activation and reset work practices, and the current approved IMD FAQ before closing this design.',
        [components.series.logical,components.imd.equipment,components.coils.equipment])

    // A real native guarded state machine preserves protective-trip latching and safe outputs.
    def common = org.polarsys.capella.core.data.capellacommon.CapellacommonFactory.eINSTANCE
    def machine = identify(common.createStateMachine(), 'state-machine')
    machine.name = prefix + 'Shutdown latch and residual-energy state machine'
    components.latch.logical.ownedStateMachines.add(machine)
    def region = identify(common.createRegion(), 'state-region')
    region.name = prefix + 'Shutdown circuit operation'
    machine.ownedRegions.add(region)
    def states = [:]
    ['Open','Energized','Discharging','Latched protective trip'].each { name ->
        def value = identify(common.createState(), 'state:' + name)
        value.name = prefix + name
        region.ownedStates.add(value)
        states[name] = value
    }
    def initial = identify(common.createInitialPseudoState(), 'state:initial')
    initial.name = prefix + 'Initial safe shutdown state'
    region.ownedStates.add(initial)
    // State.availableAbstractFunctions is derived/read-only; write its stored function-side relation.
    def available = { value, keys ->
        keys.each { key ->
            def function = functions[key].LA
            if (!function.availableInStates.contains(value)) function.availableInStates.add(value)
        }
    }
    def activeFunctions = ['voltage','temperature','bms','imd','series']
    available(states.Energized, activeFunctions)
    // Charger-connected monitoring and protective indication remain available even while TS is open.
    states.values().each { value ->
        available(value, ['voltage','temperature','bms','imd','accept','latch','indicate'])
    }
    ['Open','Discharging','Latched protective trip'].each { name ->
        available(states[name], ['cutout','coils','current','torque','balance'])
        states[name].stateInvariant = constraint('STATE-' + name, name + ' safe outputs INVARIANT',
            'Isolation and precharge coil drive absent; tractive battery current interrupted; no applied motor torque; cell balancing inhibited. Residual TS voltage is discharged to T.9.1.2 low voltage within 5 s after opening. Monitoring remains required while charger-connected.',
            ['cutout','coils','current','torque','balance','discharge'].collect { functions[it].LA }, states[name])
    }
    available(states.Discharging, ['discharge'])
    available(states['Latched protective trip'], ['latch','reset','indicate'])
    def transition = { key, source, target, guard, trigger ->
        def value = identify(common.createStateTransition(), 'transition:' + key)
        value.name = prefix + key
        value.source = source
        value.target = target
        value.triggerDescription = trigger
        region.ownedTransitions.add(value)
        value.guard = constraint('GUARD-' + key, key + ' guard', guard, [functions.latch.LA,functions.cutout.LA], value)
        state.transitions << [id:api.id(value),source:api.id(source),target:api.id(target),guard:guard]
    }
    transition('Initialize open', initial, states.Open, 'Always initialize disabled; no assumed authorization to energize.', 'Initialization')
    transition('Permit energization', states.Open, states.Energized,
        'All series contacts healthy/closed; GLV present; BMS IMD BSPD protective latch clear; applicable TS activation requirements independently satisfied. Detailed activation EV.12 design TBD.', 'Valid activation request')
    transition('Any trip removes energy', states.Energized, states.Discharging,
        'Any required series contact opens or any GLV shutdown occurs. BMS IMD BSPD causes set the protective latch; other causes do not silently reset an existing latch.', 'Series-current loss or protective trip')
    transition('Discharge to latched safe state', states.Discharging, states['Latched protective trip'],
        'TS reaches T.9.1.2 low voltage within 5 s and a BMS IMD BSPD protective trip is latched; remain disabled.', 'Residual voltage becomes low voltage with protective latch set')
    transition('Discharge to ordinary open state', states.Discharging, states.Open,
        'TS reaches T.9.1.2 low voltage within 5 s and no protective trip is latched.', 'Residual voltage becomes low voltage without protective latch')
    transition('Protective fault while open', states.Open, states['Latched protective trip'],
        'BMS IMD or BSPD trip detected while disabled or charger-connected; keep coil current and balancing off.', 'Independent protective trip')
    transition('Only local manual reset releases latch', states['Latched protective trip'], states.Open,
        'All protective faults cleared AND manual reset by a person directly at the vehicle, inaccessible to driver reactivation; no remote or automatic reset. This transition keeps TS disabled; it is not an energization transition.', 'Permitted local manual reset')
    transition('Controls cannot bypass protective latch', states['Latched protective trip'], states['Latched protective trip'],
        'TSMS or emergency-button toggles, driver controls, remote requests, GLV power cycling, or uncleared faults cannot release the protective latch.', 'Unpermitted reset or reactivation attempt')
    state.state_machine = api.id(machine)

    def oaSpecifications = [
        [key:'emergency-stop', name:'Initiate emergency stop and observe disabled tractive energy', actor:'Driver'],
        [key:'manual-reset', name:'Clear protective fault and reset locally with driver excluded', actor:'Team personnel'],
        [key:'inspection', name:'Demonstrate every shutdown feature and fault effect', actor:'Team personnel']
    ]
    oaSpecifications.each { specification ->
        def activity = identify(api.create('OperationalActivity', prefix + specification.name, domains.shutdown.OA), 'activity:' + specification.key)
        api.allocate(activity, operationalActors[specification.actor])
        activity.summary = 'Operational safety/inspection behavior; procedure and test evidence remain NOT_VERIFIED.'
        if (specification.key == 'manual-reset') api.link(functions.reset.SA, activity, 'realization')
        if (specification.key == 'inspection') api.link(functions.demonstrate.SA, activity, 'realization')
        if (specification.key == 'emergency-stop') api.link(functions.switches.SA, activity, 'realization')
    }

    def derivedModule = req.createModule(api.layer('SA'), 'FSAE2027:SD:derived-module', 'EV.8 shutdown interpreted candidate obligations')
    def folder = req.createFolder(derivedModule, 'FSAE2027:SD:derived-folder', 'Shutdown safety obligations')
    obligations.each { group ->
        def derived = req.create(folder, group.identifier, prefix + group.key, group.obligation)
        req.setAttribute(derived, 'source_document_id', 'FSAE-2027-V1')
        req.setAttribute(derived, 'source_sha256', 'f71df5552e341dab1b58f1ff47212e7281953d66b9c41833de8347560913ed0e')
        req.setAttribute(derived, 'slice_id', 'FSAE2027:shutdown-v1')
        req.setAttribute(derived, 'interpretation_status', 'interpreted')
        req.setAttribute(derived, 'coverage_status', 'allocated')
        req.setAttribute(derived, 'verification_status', 'not_verified')
        def represented = group.model_keys.collectMany { key ->
            def value = functions[key]
            [value.OA,value.SA,value.LA,value.PA,components[value.owner].logical,components[value.owner].behavior,components[value.owner].equipment].findAll { it != null }
        }.unique()
        def specificHardware = [
            SERIES:contacts.collect { it.key }, 'COIL-CURRENT':contacts.collect { it.key } + ['IR-POSITIVE','IR-NEGATIVE','PRECHARGE-RELAY'],
            'NORMALLY-OPEN':['BMS-CONTACT','IMD-CONTACT','BSPD-CONTACT'], INDEPENDENCE:['BMS-CONTACT','IMD-CONTACT','BSPD-CONTACT'],
            'SHUTDOWN-EFFECTS':['IR-POSITIVE','IR-NEGATIVE','PRECHARGE-RELAY'],
            'INTERLOCK-LOCATIONS':['INTERLOCKS'], 'INTERLOCK-OPEN':['INTERLOCKS'], 'INTERLOCK-ROUTING':['INTERLOCKS'],
            'MASTERS-DIRECT':['GLVMS','TSMS'], 'GLV-MASTER':['GLVMS','ALL-GLV-LOADS'], 'TS-MASTER':['TSMS'],
            'BUTTON-COUNT':['BUTTON-LEFT','BUTTON-RIGHT','BUTTON-COCKPIT'], 'BUTTON-MECHANISM':['BUTTON-LEFT','BUTTON-RIGHT','BUTTON-COCKPIT'],
            'BUTTON-SIDES':['BUTTON-LEFT','BUTTON-RIGHT'], 'BUTTON-COCKPIT':['BUTTON-COCKPIT'], 'BUTTON-SYMBOL':['BUTTON-LEFT','BUTTON-RIGHT','BUTTON-COCKPIT']]
        (specificHardware[group.key] ?: []).each { key -> if (!represented.contains(hardware[key])) represented.add(hardware[key]) }
        represented.each { req.satisfy(it, derived) }
        def verification = constraint('VERIFY-' + group.key, group.key + ' verification NOT_VERIFIED',
            group.verification_obligation + '\nStatus: NOT_VERIFIED. No physical test or compliance conclusion is implied.\nOpen choices: ' + group.unresolved_decisions.join(' '), represented)
        req.verify(derived, verification)
        if (!group.unresolved_decisions.isEmpty()) {
            constraint('TBD-' + group.key, group.key + ' implementation TBD', group.unresolved_decisions.join('\n'), represented)
        }
        group.source_clauses.each { source ->
            def requirement = sourceRequirements[source.clause_id]
            req.derive(derived, requirement)
            represented.each { req.satisfy(it, requirement) }
            req.verify(requirement, verification)
            req.setAttribute(requirement, 'interpretation_status', 'interpreted')
            req.setAttribute(requirement, 'coverage_status', 'allocated')
            req.setAttribute(requirement, 'verification_status', 'not_verified')
            req.setAttribute(requirement, 'model_element_ids', represented.collect { '"' + api.id(it) + '"' }.join(',').with { '[' + it + ']' })
            req.setAttribute(requirement, 'verification_element_ids', '["' + api.id(verification) + '"]')
            req.setAttribute(requirement, 'shutdown_slice_id', 'FSAE2027:shutdown-v1')
            req.setAttribute(requirement, 'shutdown_source_review_status', source.source_review_status)
            req.setAttribute(requirement, 'dependencies_review_status', group.unresolved_decisions.isEmpty() ? 'no_additional_choice_identified_in_this_group' : 'pending_TBD_choices_and_referenced_sources')
            if (group.key in ['LITHIUM-COVERAGE','SHARED-SENSOR','INTERLOCK-ROUTING']) {
                req.setAttribute(requirement, 'applicability', 'conditional')
                req.setAttribute(requirement, 'applicability_reason', group.unresolved_decisions.join(' '))
            }
            state.source_allocations[source.clause_id] = [models:represented.collect { api.id(it) },verification:api.id(verification),status:'allocated',verification_status:'not_verified']
        }
        state.derived_requirements[group.key] = derived.ReqIFIdentifier
        state.verification_constraints[group.key] = api.id(verification)
    }
    constraint('SEMANTIC-STRUCTURE-v2', 'semantic structure v2 APPLIED',
        'GLV shutdown supply and operational shutdown interfaces have leaf owners. Physical verification NOT_VERIFIED.', [])
    constraint('SEMANTIC-REPAIR-v2', 'semantic repair v2 APPLIED',
        'Shutdown exchanges terminate at leaf functions and LA/PA exchanges and ports carry native realizations. Shared bootstrap aggregate allocation and boundary normalization is a separate prerequisite for final native validation. Physical verification NOT_VERIFIED.', [])
    assert created.values().every { it.eResource() != null && it.eResource().getURIFragment(it) == api.id(it) }:
        'Native serialized-reference IDs must agree before saving'
    log.info('Added EV.8 candidate shutdown behavior, hardwired interfaces, latch states and native traces; all physical verification remains pending')
}
assert api.layer('SA').system.ownedSystemComponents.isEmpty() && api.layer('SA').system.ownedFeatures.isEmpty()
return state

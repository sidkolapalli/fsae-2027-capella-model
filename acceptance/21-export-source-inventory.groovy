assert session.sessionResource.URI.toString().contains('/FSAE-2027-Electric-Arcadia/')
def requirements = api.requirements()
def module = requirements.find('FSAE-2027-V1:source-module')
assert module != null
def rows = requirements.requirements(module).collect { requirement ->
    def attributes = requirements.attributes(requirement)
    [identifier:requirement.ReqIFIdentifier,element_id:api.id(requirement),source_sha256:attributes.source_sha256,
        source_text_sha256:attributes.source_text_sha256,source_rule_id:attributes.source_rule_id,
        coverage_status:attributes.coverage_status,interpretation_status:attributes.interpretation_status,
        verification_status:attributes.verification_status]
}.sort { it.identifier }
assert rows.size() == 3197
assert rows.collect { it.identifier }.toSet().size() == rows.size()
def encodeJson
def escapeJson = { String value -> '"'+value.replace('\\','\\\\').replace('"','\\"').replace('\n','\\n').replace('\r','\\r').replace('\t','\\t')+'"' }
encodeJson = { value ->
    if (value == null) return 'null'
    if (value instanceof Boolean || value instanceof Number) return value.toString()
    if (value instanceof java.util.Map) return '{'+value.collect { k,v -> escapeJson(String.valueOf(k))+':'+encodeJson(v) }.join(',')+'}'
    if (value instanceof java.util.Collection) return '['+value.collect { encodeJson(it) }.join(',')+']'
    return escapeJson(value.toString())
}
def chunks = rows.collate(250)
chunks.eachWithIndex { chunk,index ->
    assert api.fs.write('workspace/fsae-source-inventory-'+index+'.json',encodeJson(chunk)).success()
}
return [source_record_count:rows.size(),chunk_count:chunks.size(),source_ids_unique:true]

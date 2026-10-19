assert session.sessionResource.URI.toString().contains('/FSAE-2027-Electric-Arcadia/')
assert api.layer('SA').system.summary == 'FSAE2027:bootstrap-v1'
def removed = []
api.batch('Remove duplicate bootstrap root realizations') {
    [api.layer('LA').system,api.layer('PA').system,api.epbs().system].each { component ->
        def relations = component.eContents().findAll {
            it.eClass().name in ['ComponentRealization','PhysicalArtifactRealization']
        }
        relations.groupBy { [it.eClass().name,api.id(it.targetElement)] }.each { key,duplicates ->
            duplicates.drop(1).each { relation ->
                removed << [id:api.id(relation),source:api.id(component),target:api.id(relation.targetElement),type:relation.eClass().name]
                assert api.delete(relation)
            }
        }
    }
}
return [removed_duplicate_root_traces:removed]

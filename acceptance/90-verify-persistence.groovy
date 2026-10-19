// Read-only check after closing and reopening the saved native project.
assert session.sessionResource.URI.toString().contains('/FSAE-2027-Electric-Arcadia/')
def resources = session.semanticResources.toList()
assert resources.every { it.errors.isEmpty() }: 'Saved model must reopen without unresolved-reference/load errors'
def checked = 0
def ids = new java.util.HashSet()
resources.each { resource ->
    def iterator = resource.getAllContents()
    while (iterator.hasNext()) {
        def element = iterator.next()
        def attribute = element.eClass().EIDAttribute
        if (attribute != null) {
            def id = element.eGet(attribute)
            if (id != null) {
                assert ids.add(id): 'Duplicate persistent semantic ID '+id
                assert resource.getURIFragment(element) == id: 'Stale serialized ID '+id
                assert resource.getEObject(id) == element: 'Persistent resource lookup mismatch '+id
                checked++
            }
        }
    }
}
assert checked >= 86000: 'Persistence check must traverse the complete saved candidate model'
def proxies = org.eclipse.emf.ecore.util.EcoreUtil.UnresolvedProxyCrossReferencer.find(resources.collectMany { it.contents })
assert proxies.isEmpty(): 'Saved semantic model has unresolved proxy endpoints'
return [phase:'native-persistence-verification',semantic_resources:resources.collect { it.URI.toString() },
    resource_load_errors:0,unresolved_semantic_proxies:0,verified_persistent_ids:checked,
    engineering_verification_status:'not_verified',completion_status:'in_progress']

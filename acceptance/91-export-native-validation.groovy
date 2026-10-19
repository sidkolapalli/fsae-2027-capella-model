// Preserve every installed native validator issue in the acceptance evidence.
assert session.sessionResource.URI.toString().contains('/FSAE-2027-Electric-Arcadia/')
assert session.semanticResources.every { it.errors.isEmpty() }
def projects = session.semanticResources.collectMany { it.contents }.findAll {
    it instanceof org.polarsys.capella.core.data.capellamodeller.Project
}
assert projects.size() == 1
def bundle = org.eclipse.core.runtime.Platform.getBundle('org.polarsys.capella.core.validation')
assert bundle != null
def adapter = bundle.loadClass('org.polarsys.capella.core.validation.CapellaValidatorAdapter').getConstructor().newInstance()
def validator = adapter.getClass().getMethod('getValidator').invoke(adapter)
def contract = bundle.loadClass('org.eclipse.emf.validation.service.IBatchValidator')
contract.getMethod('setIncludeLiveConstraints', boolean.class).invoke(validator, true)
def status = contract.getMethod('validate', org.eclipse.emf.ecore.EObject.class,
    org.eclipse.core.runtime.IProgressMonitor.class).invoke(validator, projects[0],
        new org.eclipse.core.runtime.NullProgressMonitor())
def issues = []
def collectIssues
collectIssues = { item ->
    if (item.isMultiStatus()) item.children.each { collectIssues(it) }
    else if (!item.isOK()) issues << [severity:item.severity,message:item.message,plugin:item.plugin,code:item.code]
}
collectIssues(status)
issues.sort { a,b -> b.severity <=> a.severity }
return [phase:'complete-native-validation-export',severity:status.severity,
    errors:issues.count { it.severity >= org.eclipse.core.runtime.IStatus.ERROR },
    warnings:issues.count { it.severity == org.eclipse.core.runtime.IStatus.WARNING },
    issueCount:issues.size(),truncated:false,issues:issues,
    engineering_verification_status:'not_verified',completion_status:'in_progress']

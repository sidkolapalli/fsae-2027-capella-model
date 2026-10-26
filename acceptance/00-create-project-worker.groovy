import java.util.concurrent.atomic.AtomicReference
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.sirius.ui.business.api.viewpoint.ViewpointSelection
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.ide.IDE
import com.sidkolapalli.capella.ai.session.SessionSafeguards
import org.polarsys.capella.core.platform.sirius.ui.project.operations.ProjectSessionCreationHelper

// The native helper owns its workspace/session commands. Queue it after this script's transaction.
def workspaceRoot = ResourcesPlugin.workspace.root
def workspacePath = workspaceRoot.location.toString().replace('\\','/')
assert workspacePath.toLowerCase().endsWith('/capella-test-workspace')
assert session.sessionResource.URI.toString().startsWith('platform:/resource/capella-ai-native-integration/') ||
    session.sessionResource.URI.toString().startsWith('platform:/resource/FSAE-2027-Electric-Arcadia/')
def projectName = 'FSAE-2027-Electric-Arcadia'
def reportPath = 'workspace/fsae-project-creation-results.json'
assert api.fs.write(reportPath,'{"success":false,"status":"started","project":"FSAE-2027-Electric-Arcadia"}').success()
def worker = new Thread({
    def report = [success:false,status:'not_started',project:projectName]
    def onUI = { Closure action ->
        def value = new AtomicReference()
        SessionSafeguards.runOnUiThread({ value.set(action.call()) } as Runnable)
        value.get()
    }
    try {
        onUI {
            assert !SessionSafeguards.isInWriteContext(): 'Project creation must run outside the script transaction'
            assert ResourcesPlugin.workspace.root.location.toString().replace('\\','/').equalsIgnoreCase(workspacePath)
            def project = workspaceRoot.getProject(projectName)
            if (project.exists()) {
                report.status = 'already_exists_untouched'
                report.success = true
                report.created = false
                return null
            }
            // The native helper appends projectName to its parent destination.
            def destination = workspaceRoot.location
            def directory = org.eclipse.core.filesystem.EFS.getLocalFileSystem().getStore(destination.append(projectName))
            assert !directory.fetchInfo().exists(): 'A directory already exists at the exact destination; refusing to overwrite or import it'
            def selectedViewpoints = new HashSet(ViewpointSelection.getViewpoints('capella'))
            assert !selectedViewpoints.isEmpty(): 'Native Capella viewpoints must be available before creating the project'
            def helper = new ProjectSessionCreationHelper(true,true)
            def createdSession = helper.createFullProject(projectName,destination,[],selectedViewpoints,new NullProgressMonitor())
            assert createdSession != null && createdSession.isOpen()
            assert createdSession.sessionResource.URI.toString().startsWith('platform:/resource/'+projectName+'/')
            report.created = true
            report.session_uri = createdSession.sessionResource.URI.toString()
            def createdApi = new CapellaAPI(createdSession)
            assert ['OA','SA','LA','PA','EPBS'].every { createdApi.layer(it) != null }
            createdApi.requirements().enableViewpoint()
            assert createdApi.requirements().viewpointEnabled()
            report.requirements_viewpoint_enabled = true
            createdSession.save(new NullProgressMonitor())
            def window = PlatformUI.workbench.activeWorkbenchWindow
            if (window != null && window.activePage != null) {
                IDE.openEditor(window.activePage,project.getFile(projectName+'.aird'),true)
                report.editor_opened = true
            } else {
                report.editor_opened = false
            }
            report.status = 'created_and_saved'
            report.success = true
            return null
        }
    } catch (Throwable error) {
        report.status = report.created ? 'created_but_initialization_failed' : 'failed_before_creation_completed'
        report.error = error.toString()
        if (error.cause != null) report.cause = error.cause.toString()
    } finally {
        def escaped = { value -> String.valueOf(value).replace('\\','\\\\').replace('"','\\"').replace('\n','\\n').replace('\r','\\r') }
        def entries = report.collect { key,value -> '"'+escaped(key)+'":'+
            (value instanceof Boolean ? value.toString() : '"'+escaped(value)+'"') }.join(',')
        assert new FileSystemAPI().write(reportPath,'{'+entries+'}').success()
    }
} as Runnable,'CapellaAI-FSAEProjectCreation')
worker.daemon = true
worker.start()
return 'Project creation worker queued. Read workspace/fsae-project-creation-results.json before executing the bootstrap.'

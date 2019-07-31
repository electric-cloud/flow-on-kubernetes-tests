package steps


import cucumber.api.groovy.EN
import org.slf4j.LoggerFactory

this.metaClass.mixin(EN)

def commandOut

def logger = LoggerFactory.getLogger("Steps")

def chartsFolder = "./flow-on-kubernetes/charts"
def helmInit = "helm init"
def createServiceAccount = "kubectl create serviceaccount --namespace kube-system tiller"
def alreadyExistsSuppressedError = "AlreadyExists"
def createClusterRoleBinding = "kubectl create clusterrolebinding tiller-cluster-rule --clusterrole=cluster-admin --serviceaccount=kube-system:tiller"
def patchTillerDeploy = '''kubectl patch deploy --namespace kube-system tiller-deploy -p {"spec":{"template":{"spec":{"serviceAccount":"tiller"}}}}'''

When(~/^I execute command (.*)$/) { String command ->
  def out = Executor.runCommand(command)
  commandOut = out
}

When(~/^I run command (.*) in directory (.*)$/) { String command, String directory ->
  def out = Executor.runCommand(command, directory)
  commandOut = out
}

Then(~/^output matches (.*)$/) { String template ->
  outputMatches(template, commandOut)
}

Then(~/^output matches:$/) { String template ->
  outputMatches(template, commandOut)
}

private static void outputMatches(String template, commandOut) {
  def pattern = ~"${template}"
  assert commandOut =~ pattern
}

Then(~/^output contains:$/) { String str ->
  assert commandOut.contains(str)
}

Then(~/^output contains: (.*)$/) { String str ->
  assert commandOut.contains(str)
}

Given(~/^Cluster provisioned$/) { ->
  def out = Executor.runCommand("kubectl get namespaces")
  assert out.contains("kube-system")
  logger.info("Cluster provisioned")
}

And(~/^Helm installed$/) { ->
  def out = Executor.runCommand("helm version", null, "Error: could not find tiller")
  assert out =~ ~/SemVer:"v(\d+)\.(\d+)\.(\d+)"/
  logger.info("Helm installed")
}

And(~/^tiller deployed to the cluster$/) { ->
  Executor.runCommand(helmInit, chartsFolder)
  Executor.runCommand(createServiceAccount, chartsFolder, alreadyExistsSuppressedError)
  Executor.runCommand(createClusterRoleBinding, chartsFolder, alreadyExistsSuppressedError)
  Executor.runCommand(patchTillerDeploy, chartsFolder)
  logger.info("Tiller deployed")
}

And(~/^flow-on-kubernetes repo cloned$/) { ->
  Executor.runCommand("rm -rf flow-on-kubernetes")
  Executor.runCommand("git clone git@github.com:electric-cloud/flow-on-kubernetes.git")
}

When(~/I replace (.*) value to (.*) value in (.*)$/) { String regex, String replacement, String filePath ->
  def newReplacement = replaceIfApplicable(replacement)
  def file = new File(filePath)
  def fileContent = file.text
  def replaced = fileContent.replaceAll(regex, newReplacement)
  def w = file.newWriter()
  w << replaced
  w.close()
}

static String replaceIfApplicable(String template) {

  if (template.contains("<rds-endpoint>")) {
    return template.replace("<rds-endpoint>", Shared.properties["aws.rds.endpoint"].toString())
  }
  if(template.contains("<hostname>")) {
    return template.replace("<hostname>", Shared.properties["web.server.hostname"].toString())
  }

  return template
}
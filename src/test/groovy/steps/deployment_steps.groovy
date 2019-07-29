package groovy.steps


import cucumber.api.groovy.EN
import org.slf4j.LoggerFactory

this.metaClass.mixin(EN)

def commandOut

def logger = LoggerFactory.getLogger("Steps")

def chartsFolder = "./flow-on-kubernetes/charts"
def helmInit = "helm init"
def createServiceAccount = "kubectl create serviceaccount --namespace kube-system tiller"
def createClusterRoleBinding = "kubectl create clusterrolebinding tiller-cluster-rule --clusterrole=cluster-admin --serviceaccount=kube-system:tiller"


When(~/^I execute command (.*)$/) { String command ->
  logger.info("Execution '$command'")
  def out = Executor.runCommand(command)
  commandOut = out
}

When(~/^I run command (.*) in directory (.*)$/) { String command, String directory ->
  logger.info("Execution '$command'")
  def out = Executor.runCommandDir(command, directory)
  commandOut = out
}

Then(~/^output matches (.*)$/) { String str ->
  def pattern = ~"$str"
  assert commandOut =~ pattern
}

Then(~/^output matches:$/) { String str ->
  def pattern = ~"$str"
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
  def out = Executor.runCommand("helm version")
  assert out =~ ~/SemVer:"v(\d+)\.(\d+)\.(\d+)"/
  logger.info("Helm installed")
}

And(~/^tiller deployed to the cluster$/) { ->

  Executor.runCommandDir(helmInit, chartsFolder)
  Executor.runCommandDir(createServiceAccount, chartsFolder)
  Executor.runCommandDir(createClusterRoleBinding, chartsFolder)

  logger.info("Tiller deployed")
}

And(~/^flow-on-kubernetes repo cloned$/) { ->
  Executor.runCommand("rm -rf flow-on-kubernetes")
  Executor.runCommand("git clone https://github.com/electric-cloud/flow-on-kubernetes.git")
}

When(~/I replace (.*) value to (.*) value in (.*)$/) { String regex, String replacement, String filePath ->

  newReplacement = replaceIfApplicable(replacement)

  file = new File(filePath)
  fileContent = file.text
  replaced = fileContent.replaceAll(regex, replacement)
  w = file.newWriter()
  w << replaced
  w.close()
}

def replaceIfApplicable(String template)
{
  replaced = template

  if(replaced.contains("<rds-endpoint>"))
  {
    Properties properties = new Properties()
    File propertiesFile = new File('test.properties')
    propertiesFile.withInputStream {
      properties.load(it)
    }
    replaced = replaced.replace("<rds-endpoint>", properties["rdsEndpoint"].toString())
  }

  return replaced
}
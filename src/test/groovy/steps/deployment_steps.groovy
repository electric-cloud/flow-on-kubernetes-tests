package steps


import cucumber.api.groovy.EN
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml

import static steps.Executor.runCommand

this.metaClass.mixin(EN)

def commandOut

def logger = LoggerFactory.getLogger("Steps")

def chartsFolder = "./flow-on-kubernetes/charts"
def helmInit = "helm init"
def createServiceAccount = "kubectl create serviceaccount --namespace kube-system tiller"
def alreadyExistsSuppressedError = "AlreadyExists"
def createClusterRoleBinding = "kubectl create clusterrolebinding tiller-cluster-rule --clusterrole=cluster-admin --serviceaccount=kube-system:tiller"
def patchTillerDeploy = '''kubectl patch deploy --namespace kube-system tiller-deploy -p {"spec":{"template":{"spec":{"serviceAccount":"tiller"}}}}'''
String recreateDbTemplate = "kubectl exec <mySqlProxyPod> -- bash -c \"mysql -h\\\"<dbHost>\\\" -P<dbPort> -u<dbUser> -p<dbPassword> -e \\\"drop database IF EXISTS <dbName>;create database <dbName> CHARACTER SET utf8 COLLATE utf8_unicode_ci;\\\"\""

private static String replaceIfApplicable(String template) {

  if (template.contains("<rds-endpoint>")) {
    return template.replace("<rds-endpoint>", Shared.properties["aws.rds.endpoint"].toString())
  }
  if (template.contains("<hostname>")) {
    return template.replace("<hostname>", Shared.properties["web.server.hostname"].toString())
  }
  return template
}

private static void outputMatches(String template, commandOut) {
  def pattern = ~template
  assert commandOut =~ pattern
}

When(~/^I execute command (.*)$/) { String command ->
  def out = runCommand(command)
  commandOut = out
}

When(~/^I run command (.*) in directory (.*)$/) { String command, String directory ->
  def out = runCommand(command, directory)
  commandOut = out
}

Then(~/^output matches (.*)$/) { String template ->
  outputMatches(template, commandOut)
}

Then(~/^output matches:$/) { String template ->
  outputMatches(template, commandOut)
}

Then(~/^output contains:$/) { String str ->
  assert commandOut.contains(str)
}

Then(~/^output contains: (.*)$/) { String str ->
  assert commandOut.contains(str)
}

Given(~/^Cluster provisioned$/) { ->
  def out = runCommand("kubectl get namespaces")
  assert out.contains("kube-system")
  logger.info("Cluster provisioned")
}

And(~/^Helm installed$/) { ->
  def out = runCommand("helm version", null, "Error: could not find tiller")
  assert out =~ ~/SemVer:"v(\d+)\.(\d+)\.(\d+)"/
  logger.info("Helm installed")
}

And(~/^tiller deployed to the cluster$/) { ->
  runCommand(helmInit, chartsFolder)
  runCommand(createServiceAccount, chartsFolder, alreadyExistsSuppressedError)
  runCommand(createClusterRoleBinding, chartsFolder, alreadyExistsSuppressedError)
  runCommand(patchTillerDeploy, chartsFolder)
  logger.info("Tiller deployed")
}

And(~/^flow-on-kubernetes repo cloned$/) { ->
  runCommand("rm -rf flow-on-kubernetes")
  runCommand("git clone git@github.com:electric-cloud/flow-on-kubernetes.git")
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

And(~/^mysql-proxy pod deployed to the cluster$/) { ->
  def createMySqlPod = '''kubectl run mysql-proxy --image mysql:5.7.23 --env="MYSQL_ROOT_PASSWORD=mypassword"'''
  runCommand(createMySqlPod, null, alreadyExistsSuppressedError)
}

And(~/^I wait for (\d+) seconds$/) { int seconds ->
  sleep(seconds * 1000)
}

Given(~/^I recreate the database from (.*)$/) { String valuesFile ->
  def podName = Executor.getPodName("default", "mysql-proxy")
  Yaml yaml = new Yaml()
  String yamlContent = new File(valuesFile).text
  Map<String, String> dbConfig = yaml.load(yamlContent).database
  String cmd = recreateDbTemplate.replace("<dbHost>", dbConfig.externalEndpoint)
          .replace("<dbUser>", dbConfig.dbUser)
          .replace("<dbPassword>", dbConfig.dbPassword)
          .replace("<dbPort>", dbConfig.dbPort)
          .replace("<dbName>", dbConfig.dbName)
          .replace("<mySqlProxyPod>", podName)
  runCommand(cmd)
}
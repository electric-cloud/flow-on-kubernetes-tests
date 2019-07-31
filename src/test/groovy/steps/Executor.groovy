package steps

import org.slf4j.LoggerFactory

class Executor {
  static final def logger = LoggerFactory.getLogger("Executor")

  static String runCommand(command, executionDir = null, suppressedError = null) {

    logger.info("Execution '$command'")
    def sout = new StringBuilder(), serr = new StringBuilder()

    Process process = executionDir ? command.execute(null, new File(executionDir))
            : command.execute()
    process.consumeProcessOutput(sout, serr)
    process.waitFor()
    def exitCode = process.exitValue()

    logger.info(sout.toString())

    if (exitCode) {
      if (suppressedError && serr.toString().contains(suppressedError as String)) {
        return sout.toString()
      }
      executionDir = executionDir ? executionDir : ""
      logger.error("Error during execution command $executionDir\$ $command")
      logger.error("Exit value: ${exitCode}")
      logger.error("Error message: ${serr}")
      assert false: "Exit value: ${exitCode} Message: ${serr}"
    }

    return sout.toString()
  }

  static String getPodName(String namespace, String regex) {
    ['/bin/bash', '-c', "kubectl get pods -n $namespace -o name | grep $regex"].execute().text.trim() - "pod/"
  }
}
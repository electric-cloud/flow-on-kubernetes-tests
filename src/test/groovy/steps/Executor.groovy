package groovy.steps

import org.slf4j.LoggerFactory

class Executor {
  static final def logger = LoggerFactory.getLogger("Executor")

  static String runCommand(command) {
    assert command != ""
    def out = new StringBuilder()
    def err = new StringBuilder()
    Process process
    try {
      process = command.execute()
      process.consumeProcessOutputStream(out)
      process.consumeProcessErrorStream(err)
      process.waitFor()
    } catch (IOException exception) {
      assert false: exception.toString()
    }

    if (process?.exitValue()) {
      logger.error("Exit value: ${process.exitValue()} Error: ${err}")
    }

    logger.info(out.toString())

    return out.toString()
  }

  static String runCommandDir(command, executionDir) {
    assert command != ""
    assert executionDir != ""
    def sout = new StringBuilder(), serr = new StringBuilder()
    def exitCode
    Process process
    try {
      process = command.execute(null, new File(executionDir))
      process.consumeProcessOutput(sout, serr)
      process.waitFor()
      exitCode = process.exitValue()
    } catch (IOException exception) {
      assert false: exception.toString()
    }

    logger.info(sout.toString())
    if (exitCode) {
      logger.error("Exit value: ${exitCode} Error: ${serr}")
    }
    return sout.toString()
  }
}
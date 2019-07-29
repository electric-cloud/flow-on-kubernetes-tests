import cucumber.api.CucumberOptions
import cucumber.api.junit.Cucumber
import org.junit.runner.RunWith

@RunWith(Cucumber.class)
@CucumberOptions(
  plugin = ['pretty', 'junit:build/cucumber.xml', 'json:build/cucumber.json', 'html:target/cucumber-html-report'],
  glue = ['steps', 'classpath:io.jdev.geb.cucumber.steps.groovy.en'],
  tags = ['not @defect', 'not @updateTest'],
  strict = true
)
class RunDeploymentTests {
}
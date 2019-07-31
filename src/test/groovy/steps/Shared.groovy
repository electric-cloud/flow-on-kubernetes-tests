package steps

class Shared {
  static Properties properties
  static getProperties() {
    if (properties == null) {
      properties = new Properties()
      File propertiesFile = new File('test.properties')
      propertiesFile.withInputStream {
        properties.load(it)
      }
    }
    return properties
  }
}

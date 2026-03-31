def call(String sonar_qube_api, String project_name, String project_key){
  withSonarQubeEnv("${sonar_qube_api}"){
      sh "$SONAR_HOME/bin/sonar-scanner -Dsonar.projectName=${project_name} -Dsonar.projectKey=${project_key} -X"
  }
}
